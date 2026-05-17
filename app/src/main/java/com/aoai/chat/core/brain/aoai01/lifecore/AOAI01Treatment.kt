package com.aoai.chat.core.brain.aoai01.lifecore

import android.util.Log
import com.aoai.chat.BuildConfig
import com.aoai.chat.core.brain.aoai01.AOAI01StateStore
import com.aoai.chat.core.brain.aoai01.ReviewReport

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

        // Case 1: 낮은 지능 (점수 기반)
        if (report.score < 50) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Low intelligence detected. Prescribing 'Logic Flush'...")
            }
            vitality.update(-5.0)
        }

        // Case 2: 과부하 (높은 지연시간) 치료
        if (report.latencyMs > 10000) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Latency overload. Prescribing 'Cache Purge' and 'Light Mode'...")
            }
            store.setPolicyValue("resource_mode", "ULTRA_LIGHT")
            vitality.update(-3.0)
        }

        // Case 3: 에너지 고갈 위기
        if (currentEnergy < 20.0) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Low energy emergency. Prescribing 'Deep Sleep'...")
            }
            store.setEvolutionWeight("local_confidence", 0.1)
        }

        // Case 4: 완벽한 상태에서의 '강화' (Self-Enhancement)
        if (report.score > 95 && currentEnergy > 150.0) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Perfect health detected. Prescribing 'Evolution Acceleration'...")
            }
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
            when {
                reason.contains("short", ignoreCase = true) || reason.contains("짧음") -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Resolution: Detected failure in response content. Forcing Provider Swap.")
                    }
                    store.setEvolutionWeight("last_error_type", 1.0)
                }
                reason.contains("slow", ignoreCase = true) || reason.contains("느림") -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Resolution: System congestion. Switching to Lightweight Strategy.")
                    }
                    store.setPolicyValue("resource_mode", "ULTRA_LIGHT")
                }
                reason.contains("repetitive", ignoreCase = true) || reason.contains("반복") -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Resolution: Output truncation detected. Resetting reasoning depth.")
                    }
                    store.setEvolutionWeight("reasoning_depth", 0.9)
                }
            }
        }
    }
}
