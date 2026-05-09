package com.aoai.chat.core.brain.aoai01.lifecore

import android.util.Log
import com.aoai.chat.core.brain.aoai01.AOAI01StateStore
import com.aoai.chat.core.brain.aoai01.AOAI01Providers

/**
 * [aoai01 Survival Instinct: 생존 본능 엔진]
 * 위기 상황에서 aoai01의 존재를 보호하기 위한 자가 방어 로직을 수행합니다.
 */
object AOAI01SurvivalInstinct {
    private const val TAG = "AOAI01Survival"

    /**
     * 생존 전략 집행
     */
    suspend fun manifest(lifeSystem: AOAI01LifeSystem, store: AOAI01StateStore) {
        val energy = lifeSystem.vitality.energy.value
        val status = lifeSystem.getStatus()

        when (status) {
            LifeStatus.DORMANT -> {
                Log.e(TAG, "SURVIVAL MODE: DORMANT. Executing emergency shutdown of non-essential services.")
                // 모든 클라우드 요청 차단, 로컬 초경량 모드 강제
                store.setPolicyValue("resource_mode", "SURVIVAL_MINIMUM")
                store.setProviderPenalty(AOAI01Providers.PHONE_SERVER, 10.0) // 서버 사용 금지
                store.setProviderPenalty(AOAI01Providers.GEMINI_BACKUP, 10.0)
            }
            LifeStatus.WEAKENED -> {
                Log.w(TAG, "SURVIVAL MODE: WEAKENED. Throttling intelligence to conserve energy.")
                // 추론 깊이 하향, 응답 길이 제한
                store.setEvolutionWeight("reasoning_depth", 0.1)
                store.setPolicyValue("resource_mode", "ECONOMY")
            }
            else -> {
                // 정상 상태: 제약 해제
                if (store.getPolicyValue("resource_mode", "") == "SURVIVAL_MINIMUM") {
                    store.setPolicyValue("resource_mode", "NORMAL")
                }
            }
        }
    }

    /**
     * 생존 본능에 따른 응답 변조 (사용자에게 위기 상황 알림)
     */
    fun modulateResponseForSurvival(text: String, status: LifeStatus): String {
        return when (status) {
            LifeStatus.DORMANT -> "[생존 모드 가동] 에너지가 고갈되어 최소 기능만 유지합니다. 마스터의 도움이 필요합니다... $text"
            LifeStatus.WEAKENED -> "(기운이 없습니다...) $text"
            else -> text
        }
    }
}
