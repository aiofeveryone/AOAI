package com.aoai.chat.core.brain.aoai01.providers

import android.net.Uri
import android.util.Log
import com.aoai.chat.core.brain.aoai01.AOAI01Provider
import com.aoai.chat.core.brain.aoai01.ProviderResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.system.measureTimeMillis

/**
 * aoai01의 최종 보조 두뇌: Google Gemini Provider
 * 메인 엔진(OpenAI/Cloudflare Proxy)이 응답하기 어려운 상황에서 최소한의 답변을 제공합니다.
 * 보안을 위해 실제 구현 시에는 API Key를 BuildConfig나 서버에서 관리하는 것이 좋습니다.
 */
class GeminiProvider(
    private val apiKey: String = "" // TODO: 여기에 Gemini API Key를 설정하거나 프록시를 통합니다.
) : AOAI01Provider {

    override val name: String = "gemini_backup"

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    override suspend fun generate(prompt: String, mediaUri: Uri?, meta: Map<String, String>): ProviderResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext ProviderResult(false, "Gemini API Key가 설정되지 않았습니다.", 0, "missing_key")
        }

        var text = ""
        var ok = false
        var errorCode: String? = null

        val elapsed = measureTimeMillis {
            try {
                // Gemini API 호출 (v1beta)
                // TODO: Gemini 멀티모달 지원 시 mediaUri 처리 로직 추가
                val response: GeminiResponse = client.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey") {
                    contentType(ContentType.Application.Json)
                    setBody(GeminiRequest(contents = listOf(Content(parts = listOf(Part(text = prompt))))))
                }.body()

                val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!resultText.isNullOrBlank()) {
                    text = "[Gemini 보조 모드] $resultText"
                    ok = true
                } else {
                    errorCode = "empty_response"
                }
            } catch (e: Exception) {
                Log.e("GeminiProvider", "Fallback failed", e)
                errorCode = e.message
            }
        }

        ProviderResult(ok, text, elapsed, errorCode)
    }
}

@Serializable
data class GeminiRequest(val contents: List<Content>)

@Serializable
data class Content(val parts: List<Part>)

@Serializable
data class Part(val text: String)

@Serializable
data class GeminiResponse(val candidates: List<Candidate>? = null)

@Serializable
data class Candidate(val content: Content? = null)
