package com.aoai.chat.ai

import com.aoai.chat.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

// ============================
// 1️⃣ Request / Response 모델
// ============================

data class OpenAIMessage(
    val role: String,
    val content: String
)

data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>
)

data class OpenAIChoice(
    val message: OpenAIMessage
)

data class OpenAIResponse(
    val choices: List<OpenAIChoice>
)

// ============================
// 2️⃣ Retrofit API 인터페이스
// ============================

interface OpenAIService {

    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIRequest
    ): OpenAIResponse
}

// ============================
// 3️⃣ Provider 구현
// ============================

class OpenAIProvider {

    private val apiKey = BuildConfig.OPENAI_API_KEY

    private val service: OpenAIService by lazy {

        val logging = HttpLoggingInterceptor().apply {
            // ✅ BODY는 편하지만 민감정보/토큰 노출 위험이 있어 debug에만 권장
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            // ✅ 무한 대기 방지 타임아웃
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIService::class.java)
    }

    /**
     * ✅ 모델이 "오늘 날짜"를 추측하지 않도록 현재 시간을 system 메시지로 주입
     * - ZonedDateTime.now(): 디바이스(에뮬레이터/기기)의 현재 시간 기준
     * - ISO 포맷으로 명확히 전달
     */
    private fun buildTimeSystemMessage(): OpenAIMessage {
        val now = ZonedDateTime.now()
        val iso = now.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val tz = now.zone.id

        return OpenAIMessage(
            role = "system",
            content = "현재 시간 정보: $iso (timezone=$tz). 날짜/시간 질문은 이 값을 기준으로 답하세요. 모르면 추측하지 말고 모른다고 답하세요."
        )
    }

    suspend fun sendMessage(input: String): String {

        if (apiKey.isBlank()) {
            return "API Key가 설정되지 않았습니다."
        }

        return try {

            val request = OpenAIRequest(
                model = "gpt-4o-mini",
                messages = listOf(
                    buildTimeSystemMessage(),
                    OpenAIMessage(
                        role = "user",
                        content = input
                    )
                )
            )

            val response = service.getChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            response.choices.firstOrNull()?.message?.content
                ?: "AI 응답이 비어 있습니다."

        } catch (e: Exception) {
            "AI 호출 실패: ${e.message}"
        }
    }
}