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
            store.incrementIntelligenceExp(report.score / 10) // ✅ 성공적인 추론을 통한 지능 성숙도(EXP) 획득
            
            // 응답 속도에 따른 가중치 (성공 시)
            val bonus = when {
                report.latencyMs <= 1000 -> 0.10 // 1초 이하: 매우 빠름 (보상 강화)
                report.latencyMs <= 3000 -> 0.05 // 3초 이하: 보통
                else -> 0.01                     // 느리지만 성공: 보상 미미
            }
            store.setProviderPenalty(providerName, (currentPenalty - bonus).coerceAtLeast(0.0))
        } else {
            store.incFail(providerName)
            
            // 응답 속도에 따른 패널티 (실패 시)
            val penalty = when {
                report.latencyMs >= 10000 -> 0.40 // 10초 이상 타임아웃성 실패: 패널티 대폭 강화
                else -> 0.20                      // 일반 실패
            }
            store.setProviderPenalty(providerName, currentPenalty + penalty)

            // ✅ 자율 진화: 점수가 너무 낮으면 지능 모드를 '심층 분석'으로 유도
            if (report.score < 40) {
                val currentDepth = store.getEvolutionWeight("reasoning_depth")
                store.setEvolutionWeight("reasoning_depth", (currentDepth + 0.1).coerceAtMost(1.0))
            }
        }

        // ✅ 자율 지능 성장: 성공률이 높으면 로컬 신뢰도 향상
        if (report.score >= 85) {
            val confidence = store.getEvolutionWeight("local_confidence")
            store.setEvolutionWeight("local_confidence", (confidence + 0.05).coerceAtMost(1.0))
        }
    }

    /**
     * 시간이 흐름에 따라 패널티를 서서히 회복시킵니다.
     */
    suspend fun decayPenalties() {
        val targets = listOf(
            AOAI01Providers.PHONE_SERVER, 
            AOAI01Providers.LOCAL, 
            AOAI01Providers.GEMINI_BACKUP
        )
        for (p in targets) {
            val cur = store.getProviderPenalty(p)
            if (cur > 0) {
                store.setProviderPenalty(p, cur * 0.95) // 회복 속도 소폭 상향
            }
        }
    }
}
