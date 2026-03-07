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
        
        var isValidated = true // 검증 상태 관리

        Log.i(TAG, "Executing TaskGraph for intent: ${plan.analysis.type}, complexity: ${plan.analysis.complexity}")

        // 그래프의 노드를 순차적으로 또는 조건부로 실행
        for (step in plan.steps) {
            when (step) {
                is PlanStep.UseCache -> {
                    Log.d(TAG, "Node: Cache Check")
                    cacheHit = true
                }
                is PlanStep.RetrieveMemory -> {
                    Log.d(TAG, "Node: Context Retrieval")
                }
                is PlanStep.RunDraftReasoning -> {
                    Log.d(TAG, "Node: Draft Generation (${step.modelTier})")
                    val result = localProvider?.generate(ctx.userText, mediaUri, mapOf("tier" to step.modelTier))
                    if (result?.ok == true) {
                        currentResponse = result.text
                        agent.updateLiveMessage(msgId, currentResponse + "\n\n(상세 분석 및 검증 중...)")
                    }
                }
                is PlanStep.RunReasoning -> {
                    Log.d(TAG, "Node: Refined Reasoning via ${step.provider}")
                    currentModelName = step.modelName
                    var result = performReasoning(step.provider, ctx, mediaUri)
                    
                    // ✅ [Bug Fix] 기본 프로바이더 실패 시 Gemini Backup 시도
                    if (!result.ok && step.provider != "gemini_backup") {
                        Log.w(TAG, "Primary provider (${step.provider}) failed. Trying gemini_backup...")
                        val backupResult = performReasoning("gemini_backup", ctx, mediaUri)
                        if (backupResult.ok) {
                            result = backupResult
                            currentModelName = "gemini-pro (fallback)"
                            fallbackReason = "Primary provider failed: ${result.errorCode}"
                        }
                    }

                    if (result.ok) {
                        currentResponse = result.text
                        agent.updateLiveMessage(msgId, currentResponse)
                    } else {
                        fallbackReason = result.errorCode
                        errorCode = result.errorCode
                    }
                }
                is PlanStep.ValidateOutput -> {
                    Log.d(TAG, "Node: Output Validation (Criteria: ${step.criteria})")
                    isValidated = validateContent(currentResponse, step.criteria)
                    if (!isValidated) {
                        Log.w(TAG, "Validation failed. Response requires refinement.")
                        currentResponse += "\n\n(검증 결과 보완이 필요하여 재구조화 중입니다...)"
                        agent.updateLiveMessage(msgId, currentResponse)
                    }
                }
                is PlanStep.PostProcess -> {
                    Log.d(TAG, "Node: Post-Processing (${step.format})")
                }
                is PlanStep.ExecuteDeviceAction -> {
                    Log.d(TAG, "Node: Execute Device Action (${step.actionType})")
                    
                    // ✅ 검색 엔진별 액션 처리 (구글, 네이버, 다음)
                    when (step.actionType) {
                        "GOOGLE_SEARCH", "NAVER_SEARCH", "DAUM_SEARCH" -> {
                            val query = step.params["query"] ?: ctx.userText
                            val engineName = step.actionType.split("_")[0]
                            executeWebSearch(engineName, query)
                            actionResults[step.actionType] = "$engineName Search initiated: $query"
                            agent.addSystemMessage("$engineName 웹사이트에서 '$query' 관련 정보를 검색하도록 브라우저를 실행했습니다.")
                        }
                        else -> {
                            actionResults[step.actionType] = "Action initiated: ${step.actionType}"
                            agent.addSystemMessage("기기 동작 실행 시도: ${step.actionType}")
                        }
                    }
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
            success = currentResponse.isNotEmpty() && isValidated,
            fallbackReason = fallbackReason,
            cacheHit = cacheHit,
            errorCode = errorCode,
            userSatisfaction = null,
            actionResults = actionResults
        )

        return currentResponse to outcome
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
        val isSelfContradictory = content.contains("모르겠습니다") && content.length > 500
        return !isSelfContradictory
    }

    private suspend fun performReasoning(providerName: String, ctx: AOAIContext, mediaUri: Uri?): ProviderResult {
        return when (providerName) {
            "local" -> localProvider?.generate(ctx.userText, mediaUri, emptyMap()) ?: ProviderResult(false, "", 0)
            "phoneServer" -> phoneServerProvider?.generate(ctx.userText, mediaUri, emptyMap()) ?: ProviderResult(false, "", 0)
            "gemini_backup" -> geminiProvider.generate(ctx.userText, mediaUri, emptyMap())
            "hybrid" -> {
                val primary = phoneServerProvider?.generate(ctx.userText, mediaUri, emptyMap())
                if (primary?.ok == true) primary else geminiProvider.generate(ctx.userText, mediaUri, emptyMap())
            }
            else -> ProviderResult(false, "Unknown provider", 0)
        }
    }
}
