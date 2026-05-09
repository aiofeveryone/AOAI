package com.aoai.chat.core.brain.aoai01

import android.content.Context
import android.util.Log
import com.aoai.chat.core.brain.aoai01.evolution.AOAI01AdaptivePolicyStore
import com.aoai.chat.core.brain.aoai01.evolution.IntentPolicy
import kotlin.math.max

/**
 * [AOAI01 Planner: 지능적 자동화 및 전략 수립]
 */
class AOAI01Planner(
    private val context: Context,
    private val store: AOAI01StateStore
) {
    private val TAG = "AOAI01Planner"

    suspend fun makePlan(ctx: AOAIContext): Plan {
        val rules = AOAI01AdaptivePolicyStore.getActiveRules(context)
        val analysis = AOAI01IntentRouter.analyze(ctx.userText)
        val intentPolicy = rules.intentPolicies[analysis.type.name] ?: IntentPolicy()
        
        // ✅ 지능 레벨에 따른 전략 확장 (Evolutionary Tiering)
        val intelligenceLevel = store.getIntelligenceLevel()

        val steps = mutableListOf<PlanStep>()
        
        // 기본 전략 결정 (스코어링 기반)
        var strategy = decideStrategyByScoring(ctx, analysis, intentPolicy)

        // ✅ 고지능 레벨(Level 3+)에서 '이중 검증(Double Verification)' 단계 추가
        if (intelligenceLevel >= 3 && analysis.complexity >= Complexity.HIGH) {
            steps.add(PlanStep.ValidateOutput(criteria = "Logical Consistency & Fact Check"))
        }

        if (analysis.needsClarification || analysis.type == IntentType.CLARIFICATION_REQUEST) {
            steps.add(PlanStep.RunReasoning(provider = AOAI01Providers.LOCAL, modelName = "critique-clarifier-v1"))
            strategy = PlanStrategy.LOCAL_ONLY
        } else {
            steps.add(PlanStep.UseCache(key = "cache_${ctx.userText.hashCode()}"))
            
            if (analysis.type == IntentType.ACTION_REQUEST) {
                val actionType = when {
                    ctx.userText.contains("알람") -> "SET_ALARM"
                    ctx.userText.contains("검색") -> "WEB_SEARCH"
                    else -> "GENERAL_ACTION"
                }
                steps.add(PlanStep.ExecuteDeviceAction(
                    actionType = actionType,
                    params = mapOf("raw_query" to ctx.userText)
                ))
            }

            val provider = when (strategy) {
                PlanStrategy.SERVER_ONLY -> AOAI01Providers.PHONE_SERVER
                PlanStrategy.HYBRID -> AOAI01Providers.HYBRID
                else -> AOAI01Providers.LOCAL
            }
            steps.add(PlanStep.RunReasoning(provider = provider, modelName = "adaptive-v3"))
        }

        steps.add(PlanStep.PostProcess(format = "concise"))

        return Plan(
            analysis = analysis,
            strategy = strategy,
            steps = steps,
            output = OutputSpec(maxTokensHint = 1000, style = "adaptive"),
            memoryPolicy = determineMemoryPolicy(analysis.type),
            evalPolicy = EvalPolicy(enable = true, metrics = listOf("latency", "accuracy")),
            policyVersion = rules.policy.policyVersion
        )
    }

    private suspend fun decideStrategyByScoring(ctx: AOAIContext, analysis: IntentAnalysis, intentPolicy: IntentPolicy): PlanStrategy {
        if (ctx.device.thermalState == "CRITICAL" || ctx.device.batteryPct < 5) return PlanStrategy.LOCAL_ONLY
        if (!ctx.network.isOnline) return PlanStrategy.LOCAL_ONLY

        val localScore = calculateLocalScore(ctx, analysis)
        val serverScore = calculateServerScore(ctx, analysis)

        return when {
            serverScore > localScore + 40 -> PlanStrategy.SERVER_ONLY
            serverScore > localScore -> PlanStrategy.HYBRID
            else -> PlanStrategy.LOCAL_ONLY
        }
    }

    private suspend fun calculateLocalScore(ctx: AOAIContext, analysis: IntentAnalysis): Int {
        var score = 30
        
        // ✅ 자율 진화 가중치 반영: 로컬 신뢰도가 높으면 로컬 우선 순위 상승
        val localConfidence = store.getEvolutionWeight("local_confidence")
        score += (localConfidence * 30).toInt()

        if (ctx.device.batteryPct < 25) score += 50
        if (ctx.network.isMetered) score += 40
        if (analysis.complexity == Complexity.LOW) score += 40
        return score
    }

    private suspend fun calculateServerScore(ctx: AOAIContext, analysis: IntentAnalysis): Int {
        var score = 50

        // ✅ 자율 진화 가중치 반영: 추론 깊이가 높으면 서버 선호도 상승 (더 똑똑한 결과 기대)
        val reasoningDepth = store.getEvolutionWeight("reasoning_depth")
        score += (reasoningDepth * 40).toInt()

        if (ctx.network.isWifi) score += 50
        if (analysis.complexity == Complexity.HIGH || analysis.complexity == Complexity.EXTREME) score += 60
        return score
    }

    private fun determineMemoryPolicy(intent: IntentType): MemoryPolicy {
        return when (intent) {
            IntentType.TROUBLESHOOT -> MemoryPolicy.SAVE_ACTION_ITEMS
            IntentType.SUMMARIZE -> MemoryPolicy.SAVE_SUMMARY
            IntentType.CODE -> MemoryPolicy.SAVE_FACTS
            else -> MemoryPolicy.SAVE_FACTS
        }
    }
}
