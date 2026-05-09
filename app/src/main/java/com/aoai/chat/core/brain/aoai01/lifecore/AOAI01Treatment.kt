package com.aoai.chat.core.brain.aoai01.lifecore

import android.util.Log
import com.aoai.chat.core.brain.aoai01.ReviewReport
import com.aoai.chat.core.brain.aoai01.AOAI01StateStore

/**
 * [aoai01 Treatment: Self-Healing System]
 * aoai01이 스스로의 상태를 진단하고 '처방'을 내립니다.
 */
object AOAI01Treatment {
    private const val TAG = "AOAI01Treatment"

    /**
     * 자가 진단 및 처방 집행
     */
    suspend fun diagnoseAndTreat(report: ReviewReport, lifeSystem: AOAI01LifeSystem, store: AOAI01StateStore) {
        val vitality = lifeSystem.vitality
        val currentEnergy = vitality.energy.value

        // Case 1: 지능 저하 (낮은 점수) 치료
        if (report.score < 50) {
            Log.w(TAG, "Low intelligence detected. Prescribing 'Logic Flush'...")
            // 패널티 일부 초기화 및 추론 경로 재설정 유도
            store.setEvolutionWeight("reasoning_depth", 0.8) 
            vitality.update(-5.0) // 치료에는 에너지가 소모됨
        }

        // Case 2: 과부하 (높은 지연시간) 치료
        if (report.latencyMs > 10000) {
            Log.w(TAG, "Latency overload. Prescribing 'Cache Purge' and 'Light Mode'...")
            store.setPolicyValue("resource_mode", "ULTRA_LIGHT")
            vitality.update(-3.0)
        }

        // Case 3: 에너지 고갈 상태에서의 자가 치유
        if (currentEnergy < 20.0) {
            Log.i(TAG, "Low energy emergency. Prescribing 'Deep Sleep'...")
            // 지능을 낮추고 생존 모드로 전환하여 에너지를 보존
            store.setEvolutionWeight("local_confidence", 0.1)
            store.setPolicyValue("resource_mode", "SLEEP")
        }

        // Case 4: 완벽한 상태에서의 '강화' (Self-Enhancement)
        if (report.score > 95 && currentEnergy > 150.0) {
            Log.i(TAG, "Perfect health detected. Prescribing 'Evolution Acceleration'...")
            vitality.update(10.0) // 보너스 에너지
            val currentExp = store.getEvolutionWeight("evolution_exp")
            store.setEvolutionWeight("evolution_exp", currentExp + 1.0)
        }

        // ✅ 에러 자동 복구 (Autonomous Error Resolution)
        if (!report.ok) {
            resolveError(report, store)
        }
    }

    /**
     * 특정 에러 패턴을 분석하고 즉각적인 해결책을 실행합니다.
     */
    private suspend fun resolveError(report: ReviewReport, store: AOAI01StateStore) {
        report.reasons.forEach { reason ->
            when (reason) {
                "ERROR_KEYWORD_DETECTED" -> {
                    Log.w(TAG, "Resolution: Detected failure in response content. Forcing Provider Swap.")
                    // 해당 프로바이더의 패널티를 즉시 대폭 인상하여 다음 턴에서 배제
                    store.setEvolutionWeight("last_error_type", 1.0) // 1.0 for Content Error
                }
                "VERY_SLOW_RESPONSE" -> {
                    Log.w(TAG, "Resolution: System congestion. Switching to Lightweight Strategy.")
                    store.setPolicyValue("resource_mode", "ULTRA_LIGHT")
                }
                "CRITICAL_SHORT" -> {
                    Log.w(TAG, "Resolution: Output truncation detected. Resetting reasoning depth.")
                    store.setEvolutionWeight("reasoning_depth", 0.9)
                }
            }
        }
    }
}
