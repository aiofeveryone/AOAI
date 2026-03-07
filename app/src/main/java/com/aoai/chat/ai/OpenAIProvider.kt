package com.aoai.chat.ai

import com.aoai.chat.core.AOAIProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * ✅ Cloudflare Worker 프록시 Provider
 * - 앱에서는 OpenAI 키를 절대 다루지 않음
 * - Authorization 헤더를 보내지 않음
 * - 프록시가 OpenAI로 전달하며 키는 Worker에만 존재
 */
class CloudflareProxyProvider(
    private val baseUrl: String = "https://api.aiofeveryone.com",
    private val model: String = "gpt-4o-mini"
) : AOAIProvider {

    override val name: String = "CLOUDFLARE_PROXY"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }

        // ✅ 네트워크 환경에서 멈춤 방지
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 60_000
        }
    }

    override suspend fun sendMessage(input: String): String = withContext(Dispatchers.IO) {
        val text = input.trim()
        if (text.isEmpty()) return@withContext ""

        val url = "${baseUrl.trimEnd('/')}/v1/chat/completions"

        try {
            val req = OpenAIRequest(
                model = model,
                messages = listOf(OpenAIMessage(role = "user", content = text))
            )

            val res: OpenAIResponse = client
                .post(url) {
                    // ✅ Authorization 헤더 금지 (키는 Worker에만)
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }
                .body()

            res.choices.firstOrNull()?.message?.content?.trim().orEmpty()
                .ifBlank { "응답이 비어 있습니다." }

        } catch (e: Exception) {
            "프록시 오류: ${e.message ?: "unknown"}"
        }
    }
}

@Serializable
private data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>
)

@Serializable
private data class OpenAIMessage(
    val role: String,
    val content: String
)

@Serializable
private data class OpenAIResponse(
    val choices: List<Choice> = emptyList()
)

@Serializable
private data class Choice(
    val message: OpenAIMessage
)