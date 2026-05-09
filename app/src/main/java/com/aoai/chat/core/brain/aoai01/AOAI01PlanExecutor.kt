package com.aoai.chat.core.brain.aoai01

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * [AOAI01 PlanExecutor]
 * Planner가 수립한 작업 그래프(TaskGraph) 형태의 실행 계획을 정밀하게 집행합니다.
 */
class AOAI01PlanExecutor(
    private val context: Context,
    private val agent: AOAI01Agent,
    private val phoneServerProvider: AOAI01Provider?,
    private val localProvider: AOAI01Provider?,
    private val geminiProvider: AOAI01Provider
) {
    private val TAG = "AOAI01Executor"

    /**
     * 실행 계획을 집행합니다. 
     */
    suspend fun execute(ctx: AOAIContext, plan: Plan, msgId: String, mediaUri: Uri? = null): Pair<String, PlanOutcome> {
        val startTime = System.currentTimeMillis()
        var currentResponse = ""
        val usedStrategy = plan.strategy
        var cacheHit = false
        var currentModelName: String? = null
        var fallbackReason: String? = null
        var errorCode: String? = null
        val actionResults = mutableMapOf<String, String>()
        
        var isValidated = true

        Log.i(TAG, "Executing TaskGraph for intent: ${plan.analysis.type}, strategy: $usedStrategy")

        for (step in plan.steps) {
            when (step) {
                is PlanStep.UseCache -> {
                    // 실제 캐시 로직 구현 가능
                    cacheHit = false 
                }
                is PlanStep.RetrieveMemory -> {
                    // 장기 기억 조회 로직
                }
                is PlanStep.RunDraftReasoning -> {
                    val result = localProvider?.generate(ctx.userText, mediaUri, mapOf("tier" to step.modelTier))
                    if (result?.ok == true) {
                        currentResponse = result.text
                        agent.updateLiveMessage(msgId, currentResponse + "\n\n(상세 분석 및 검증 중...)")
                    }
                }
                is PlanStep.RunReasoning -> {
                    currentModelName = step.modelName
                    var result = performReasoning(step.provider, ctx, mediaUri)
                    
                    // ✅ 기본 시도 실패 시 Fallback 체인 (Local -> Server -> Gemini)
                    if (!result.ok) {
                        Log.w(TAG, "Primary provider (${step.provider}) failed. Trying fallbacks...")
                        
                        val fallbacks = listOf(
                            AOAI01Providers.PHONE_SERVER to AOAI01Providers.PHONE_SERVER,
                            AOAI01Providers.GEMINI_BACKUP to AOAI01Providers.GEMINI_BACKUP
                        ).filter { it.first != step.provider }

                        for (fb in fallbacks) {
                            val fbResult = performReasoning(fb.first, ctx, mediaUri)
                            if (fbResult.ok) {
                                result = fbResult
                                currentModelName = fb.second
                                fallbackReason = "Primary (${step.provider}) failed"
                                break
                            }
                        }
                    }

                    if (result.ok) {
                        currentResponse = result.text
                        agent.updateLiveMessage(msgId, currentResponse)
                    } else {
                        fallbackReason = result.errorCode
                        errorCode = result.errorCode
                        if (currentResponse.isBlank()) {
                            currentResponse = "요청을 처리하는 중에 문제가 발생했습니다. (Error: ${result.errorCode})"
                        }
                    }
                }
                is PlanStep.ValidateOutput -> {
                    isValidated = validateContent(currentResponse, step.criteria)
                    if (!isValidated) {
                        currentResponse += "\n\n(참고: 응답 내용의 정확도가 낮을 수 있습니다.)"
                        agent.updateLiveMessage(msgId, currentResponse)
                    }
                }
                is PlanStep.PostProcess -> {
                    // 포스트 프로세싱 (포맷팅 등)
                }
                is PlanStep.ExecuteDeviceAction -> {
                    executeAction(step, ctx, actionResults)
                }
            }
        }

        val latency = System.currentTimeMillis() - startTime
        val outcome = PlanOutcome(
            timestamp = startTime,
            policyVersion = plan.policyVersion,
            deviceGrade = ctx.deviceGrade,
            platformType = ctx.platformType.name,
            usedStrategy = usedStrategy,
            modelName = currentModelName,
            latencyMs = latency,
            success = currentResponse.isNotEmpty() && errorCode == null,
            fallbackReason = fallbackReason,
            cacheHit = cacheHit,
            errorCode = errorCode,
            userSatisfaction = null,
            actionResults = actionResults
        )

        return currentResponse to outcome
    }

    private fun executeAction(step: PlanStep.ExecuteDeviceAction, ctx: AOAIContext, results: MutableMap<String, String>) {
        when (step.actionType) {
            "GOOGLE_SEARCH", "WEB_SEARCH" -> {
                val query = step.params["query"] ?: ctx.userText
                executeWebSearch("GOOGLE", query)
                results[step.actionType] = "Web Search: $query"
                agent.addSystemMessage("웹에서 '$query' 검색을 시작합니다.")
            }
            "NAVER_SEARCH" -> {
                val query = step.params["query"] ?: ctx.userText
                executeWebSearch("NAVER", query)
                results[step.actionType] = "Naver Search: $query"
                agent.addSystemMessage("네이버에서 '$query' 검색을 시작합니다.")
            }
            else -> {
                results[step.actionType] = "Unknown Action"
            }
        }
    }

    private fun executeWebSearch(engine: String, query: String) {
        try {
            val baseUrl = when (engine) {
                "NAVER" -> "https://search.naver.com/search.naver?query="
                "DAUM" -> "https://search.daum.net/search?q="
                else -> "https://www.google.com/search?q="
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$baseUrl${Uri.encode(query)}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "$engine Search failed", e)
        }
    }

    private fun validateContent(content: String, criteria: String): Boolean {
        if (content.isBlank()) return false
        return !content.contains("데이터가 없습니다")
    }

    private suspend fun performReasoning(providerName: String, ctx: AOAIContext, mediaUri: Uri?): ProviderResult {
        return when (providerName) {
            AOAI01Providers.LOCAL -> localProvider?.generate(ctx.userText, mediaUri, emptyMap()) ?: ProviderResult(false, "Local unavailable", 0)
            AOAI01Providers.PHONE_SERVER -> phoneServerProvider?.generate(ctx.userText, mediaUri, emptyMap()) ?: ProviderResult(false, "Server unavailable", 0)
            AOAI01Providers.GEMINI_BACKUP -> geminiProvider.generate(ctx.userText, mediaUri, emptyMap())
            AOAI01Providers.HYBRID -> {
                val primary = phoneServerProvider?.generate(ctx.userText, mediaUri, emptyMap())
                if (primary?.ok == true) primary else geminiProvider.generate(ctx.userText, mediaUri, emptyMap())
            }
            else -> ProviderResult(false, "Unknown provider: $providerName", 0)
        }
    }
}
