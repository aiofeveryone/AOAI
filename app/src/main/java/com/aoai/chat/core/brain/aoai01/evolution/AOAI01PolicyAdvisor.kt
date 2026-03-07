package com.aoai.chat.core.brain.aoai01.evolution

import android.util.Log
import com.aoai.chat.core.brain.aoai01.PlanOutcome
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * [AOAI01 PolicyAdvisor]
 * 수집된 텔레메트리 데이터를 분석하여 최적의 정책(rules.json) 개선안을 제안합니다.
 * 마스터의 최종 승인을 거쳐 전 세계 복제본에 배포될 수 있도록 설계되었습니다.
 */
object AOAI01PolicyAdvisor {
    private const val TAG = "PolicyAdvisor"
    private const val RECOMMENDATION_FILE = "suggested_rules_update.json"

    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true 
    }

    /**
     * 최근 성과 데이터를 바탕으로 정책 개선안을 도출합니다.
     */
    fun analyzeAndAdvise(outcomes: List<PlanOutcome>, currentRules: AdaptiveRules): RulesImprovement? {
        if (outcomes.size < 10) return null // 최소 샘플 수 부족

        val averageLatency = outcomes.map { it.latencyMs }.average()
        val errorRate = outcomes.count { !it.success }.toDouble() / outcomes.size
        
        Log.i(TAG, "Analyzing $outcomes.size samples. Avg Latency: $averageLatency, Error Rate: $errorRate")

        // 1. 성능 기반 튜닝 제안
        if (averageLatency > 5000) {
            // 지연 시간이 너무 길면 토큰 제한 축소 제안
            return RulesImprovement(
                targetVersion = currentRules.policy.policyVersion,
                reason = "High latency detected in field.",
                suggestedChanges = mapOf("runtime.planner.limits.maxOutputTokensFinal" to "320")
            )
        }

        // 2. 에러율 기반 전략 변경 제안
        if (errorRate > 0.2) {
            return RulesImprovement(
                targetVersion = currentRules.policy.policyVersion,
                reason = "Error rate exceeded threshold (20%).",
                suggestedChanges = mapOf("runtime.fallback.order" to "[\"GEMINI\", \"OPENAI\", \"LOCAL_ONLY\"]")
            )
        }

        return null
    }

    /**
     * 제안된 개선안을 마스터 확인용 파일로 저장합니다.
     */
    fun saveRecommendation(context: android.content.Context, improvement: RulesImprovement) {
        try {
            val file = File(context.filesDir, RECOMMENDATION_FILE)
            file.writeText(json.encodeToString(RulesImprovement.serializer(), improvement))
            Log.w(TAG, "New Policy Recommendation generated for Master's approval.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save recommendation", e)
        }
    }
}

@Serializable
data class RulesImprovement(
    val targetVersion: Int,
    val reason: String,
    val suggestedChanges: Map<String, String>
)
