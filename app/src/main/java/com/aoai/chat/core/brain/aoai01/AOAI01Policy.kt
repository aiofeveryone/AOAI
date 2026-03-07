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

        // 1) 특정 키워드에 따른 엔진 선호도 (예: 창의적 글쓰기는 OpenAI, 정보 검색은 Gemini 등 - 상징적)
        val prefersGemini = listOf("검색", "정보", "확인", "날씨", "뉴스")
            .any { text.contains(it, ignoreCase = true) }

        // 2) 패널티 확인 (최근 실패율 반영)
        val openAiPenalty = store.getProviderPenalty("phoneServer") // OpenAI 프록시
        val geminiPenalty = store.getProviderPenalty("gemini_backup") // Gemini 프록시
        val localPenalty = store.getProviderPenalty("local")

        // ---- 결정 로직 ----

        // 로컬이 매우 우수하거나 텍스트가 매우 짧은 경우 로컬 시도
        if (text.length <= 10 && localPenalty <= 1.0) {
            return AOAI01Route.LOCAL_THEN_OPENAI
        }

        // 특정 엔진의 패널티가 너무 높으면 다른 엔진으로 즉시 전환
        if (openAiPenalty > 2.0 && geminiPenalty <= 2.0) {
            return AOAI01Route.GEMINI_THEN_OPENAI
        }
        if (geminiPenalty > 2.0 && openAiPenalty <= 2.0) {
            return AOAI01Route.OPENAI_THEN_GEMINI
        }

        // 기본 전략: OpenAI를 메인으로 하되, Gemini를 상시 대기(Fallback)로 설정
        // 질문 성격에 따라 우선순위 조정
        return if (prefersGemini) {
            AOAI01Route.GEMINI_THEN_OPENAI
        } else {
            AOAI01Route.OPENAI_THEN_GEMINI
        }
    }
}
