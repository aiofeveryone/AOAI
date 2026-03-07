package com.aoai.chat.core.brain.aoai01

import android.net.Uri

/**
 * aoai01에게 들어오는 표준 입력
 */
data class AOAI01Input(
    val userText: String,
    val historyText: String = "",
    val mediaUri: Uri? = null, // ✅ 미디어 URI 추가
    val meta: Map<String, String> = emptyMap()
)

/**
 * aoai01이 앱에 돌려주는 표준 출력
 */
data class AOAI01Output(
    val finalText: String,
    val route: AOAI01Route,
    val providerUsed: String,
    val qualityScore: Int,
    val debug: Map<String, String> = emptyMap()
)

/**
 * aoai01이 선택한 라우팅
 */
enum class AOAI01Route {
    OPENAI_ONLY,
    GEMINI_ONLY,
    LOCAL_ONLY,
    OPENAI_THEN_GEMINI,
    GEMINI_THEN_OPENAI,
    LOCAL_THEN_OPENAI
}

/**
 * 실제 “행동기관(수단)” 인터페이스
 */
interface AOAI01Provider {
    val name: String
    // ✅ mediaUri 매개변수 추가
    suspend fun generate(prompt: String, mediaUri: Uri? = null, meta: Map<String, String>): ProviderResult
}

/**
 * Provider 실행 결과
 */
data class ProviderResult(
    val ok: Boolean,
    val text: String,
    val latencyMs: Long,
    val errorCode: String? = null
)
