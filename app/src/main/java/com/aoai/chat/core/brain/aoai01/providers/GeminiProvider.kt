package com.aoai.chat.core.brain.aoai01.providers

import android.net.Uri
import android.util.Log
import com.aoai.chat.BuildConfig
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
 * 메인 엔진이 응답하기 어려운 상황에서 최소한의 답변을 제공합니다.
 */
class GeminiProvider : AOAI01Provider {
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
    override val name: String = "gemini_backup"

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    override suspend fun generate(prompt: String, mediaUri: Uri?, meta: Map<String, String>): ProviderResult = withContext(Dispatchers.IO) {
        var text = ""
        var ok = false
        var errorCode: String? = null

        if (apiKey.isBlank()) {
            if (BuildConfig.DEBUG) {
                Log.d("GeminiProvider", "Gemini API Key not configured")
            }
            return@withContext ProviderResult(false, "API Key missing", 0, "no_api_key")
        }

        val elapsed = measureTimeMillis {
            try {
                // Gemini API 호출 (v1beta)
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
                if (BuildConfig.DEBUG) {
                    Log.e("GeminiProvider", "Fallback failed: ${e.message}", e)
                }
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
data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)
