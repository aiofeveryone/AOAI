package com.aoai.chat.core.brain.aoai01

/**
 * aoai01의 지능형 학습기 (응답 속도 기반)
 */
class AOAI01Learner(
    private val store: AOAI01StateStore
) {
    suspend fun learn(providerName: String, report: ReviewReport) {
        val currentPenalty = store.getProviderPenalty(providerName)

        if (report.ok) {
            store.incOk(providerName)
            
            // 응답 속도에 따른 가중치 (성공 시)
            val bonus = when {
                report.latencyMs <= 1000 -> 0.10 // 1초 이하: 매우 빠름 (보상 강화)
                report.latencyMs <= 3000 -> 0.05 // 3초 이하: 보통
                else -> 0.01                     // 느리지만 성공: 보상 미미
            }
            store.setProviderPenalty(providerName, currentPenalty - bonus)
        } else {
            store.incFail(providerName)
            
            // 응답 속도에 따른 패널티 (실패 시)
            val penalty = when {
                report.latencyMs >= 10000 -> 0.40 // 10초 이상 타임아웃성 실패: 패널티 대폭 강화
                else -> 0.20                      // 일반 실패
            }
            store.setProviderPenalty(providerName, currentPenalty + penalty)
        }
    }

    /**
     * 시간이 흐름에 따라 패널티를 서서히 회복시킵니다.
     */
    suspend fun decayPenalties() {
        val targets = listOf("web", "phoneServer", "local", "gemini_backup")
        for (p in targets) {
            val cur = store.getProviderPenalty(p)
            store.setProviderPenalty(p, cur * 0.98)
        }
    }
}
