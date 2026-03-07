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

    fun makePlan(ctx: AOAIContext): Plan {
        val rules = AOAI01AdaptivePolicyStore.getActiveRules(context)
        val analysis = AOAI01IntentRouter.analyze(ctx.userText)
        val intentPolicy = rules.intentPolicies[analysis.type.name] ?: IntentPolicy()

        val steps = mutableListOf<PlanStep>()
        
        // 기본 전략 결정 (스코어링 기반)
        var strategy = decideStrategyByScoring(ctx, analysis, intentPolicy)

        if (analysis.needsClarification || analysis.type == IntentType.CLARIFICATION_REQUEST) {
            steps.add(PlanStep.RunReasoning(provider = "local", modelName = "critique-clarifier-v1"))
            strategy = PlanStrategy.LOCAL_ONLY
        } else {
            steps.add(PlanStep.UseCache(key = "cache_${ctx.userText.hashCode()}"))
            
            if (analysis.type == IntentType.ACTION_REQUEST) {
                val actionType = if (ctx.userText.contains("알람")) "SET_ALARM" else "GENERAL_ACTION"
                steps.add(PlanStep.ExecuteDeviceAction(
                    actionType = actionType,
                    params = mapOf("raw_query" to ctx.userText)
                ))
            }

            val provider = if (strategy == PlanStrategy.SERVER_ONLY) "phoneServer" else "local"
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

    private fun decideStrategyByScoring(ctx: AOAIContext, analysis: IntentAnalysis, intentPolicy: IntentPolicy): PlanStrategy {
        if (ctx.device.thermalState == "CRITICAL" || ctx.device.batteryPct < 5) return PlanStrategy.LOCAL_ONLY
        if (!ctx.network.isOnline) return PlanStrategy.LOCAL_ONLY

        val localScore = calculateLocalScore(ctx, analysis)
        val serverScore = calculateServerScore(ctx, analysis)

        return if (serverScore + 50 > localScore) PlanStrategy.SERVER_ONLY else PlanStrategy.LOCAL_ONLY
    }

    private fun calculateLocalScore(ctx: AOAIContext, analysis: IntentAnalysis): Int {
        var score = 30
        if (ctx.device.batteryPct < 20) score += 50
        if (ctx.network.isMetered) score += 40
        if (analysis.complexity == Complexity.LOW) score += 30
        return score
    }

    private fun calculateServerScore(ctx: AOAIContext, analysis: IntentAnalysis): Int {
        var score = 60
        if (ctx.network.isWifi) score += 40
        if (analysis.complexity == Complexity.HIGH || analysis.complexity == Complexity.MEDIUM) score += 50
        return score
    }

    private fun determineMemoryPolicy(intent: IntentType): MemoryPolicy {
        return when (intent) {
            IntentType.TROUBLESHOOT -> MemoryPolicy.SAVE_ACTION_ITEMS
            IntentType.SUMMARIZE -> MemoryPolicy.SAVE_SUMMARY
            else -> MemoryPolicy.SAVE_FACTS
        }
    }
}
