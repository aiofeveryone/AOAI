package com.aoai.chat.core.brain.aoai01

/**
 * aoai01의 판단(정책) 엔진
 * - OpenAI(Cloudflare)와 Gemini(Cloudflare)를 적절히 배분하여 원활한 환경 제공
 */
class AOAI01Policy(
    private val store: AOAI01StateStore
) {
    suspend fun decideRoute(input: AOAI01Input): AOAI01Route {
        val text = input.userText.trim()

        // 1) 특정 키워드에 따른 엔진 선호도
        val prefersGemini = listOf("검색", "정보", "확인", "날씨", "뉴스", "이미지", "분석")
            .any { text.contains(it, ignoreCase = true) } || input.mediaUri != null

        // 2) 패널티 확인 (최근 실패율 반영)
        val serverPenalty = store.getProviderPenalty(AOAI01Providers.PHONE_SERVER)
        val geminiPenalty = store.getProviderPenalty(AOAI01Providers.GEMINI_BACKUP)
        val localPenalty = store.getProviderPenalty(AOAI01Providers.LOCAL)

        // ---- 결정 로직 ----

        // 로컬이 매우 우수하거나 텍스트가 매우 짧은 경우 로컬 시도 (지능 향상에 따라 기준 완화)
        if (text.length <= 25 && localPenalty <= 0.6) {
            return AOAI01Route.LOCAL_THEN_OPENAI
        }

        // 특정 엔진의 패널티가 너무 높으면 다른 엔진으로 즉시 전환 (더 민감하게 반응)
        if (serverPenalty > 1.8 && geminiPenalty <= 1.8) {
            return AOAI01Route.GEMINI_THEN_OPENAI
        }
        if (geminiPenalty > 1.8 && serverPenalty <= 1.8) {
            return AOAI01Route.OPENAI_THEN_GEMINI
        }
        
        // 둘 다 상태가 안 좋으면 로컬 우선 시도
        if (serverPenalty > 2.5 && geminiPenalty > 2.5) {
            return AOAI01Route.LOCAL_ONLY
        }

        // 기본 전략: 질문 성격에 따라 우선순위 조정
        return if (prefersGemini) {
            AOAI01Route.GEMINI_THEN_OPENAI
        } else {
            AOAI01Route.OPENAI_THEN_GEMINI
        }
    }
}
