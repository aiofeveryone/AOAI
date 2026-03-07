package com.aoai.chat.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * Cloudflare Worker 프록시로 OpenAI Chat Completions 요청을 보내는 Provider
 */
class ServerPhoneProvider(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val model: String = DEFAULT_MODEL,
) {

    companion object {
        private const val DEFAULT_BASE_URL = "https://api.aiofeveryone.com"
        private const val DEFAULT_MODEL = "gpt-4o-mini"

        // ✅ 타임아웃 설정 더욱 강화 및 재시도 로직을 위한 설정
        private const val CONNECT_TIMEOUT_MS = 60_000
        private const val READ_TIMEOUT_MS = 90_000
        private const val MAX_RETRIES = 2

        private const val TAG = "AOAI-CloudflareProxy"
    }

    private fun endpointChatCompletions(): String {
        val trimmed = baseUrl.trim()
        return "${trimmed.trimEnd('/')}/v1/chat/completions"
    }

    suspend fun sendMessage(text: String): String = withContext(Dispatchers.IO) {
        val input = text.trim()
        if (input.isEmpty()) return@withContext ""

        var lastException: Exception? = null
        for (attempt in 0..MAX_RETRIES) {
            try {
                return@withContext performRequest(input)
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < MAX_RETRIES) {
                    delay(1000L * (attempt + 1)) // 지수 백오프 대신 간단한 재시도 대기
                }
            }
        }

        throw lastException ?: RuntimeException("Unknown error occurred")
    }

    private fun performRequest(input: String): String {
        val endpoint = endpointChatCompletions()
        val conn = (URL(endpoint).openConnection() as HttpURLConnection)
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.doOutput = true
            conn.useCaches = false

            // ✅ 연결 유지 설정 추가 (Keep-Alive)
            conn.setRequestProperty("Connection", "Keep-Alive")
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")

            val bodyBytes = buildChatCompletionsBody(input)
                .toString()
                .toByteArray(Charsets.UTF_8)

            conn.outputStream.use { os ->
                os.write(bodyBytes)
                os.flush()
            }

            val code = conn.responseCode
            val raw = if (code in 200..299) {
                readStream(conn.inputStream).orEmpty()
            } else {
                val err = readStream(conn.errorStream).orEmpty()
                val pretty = extractErrorMessage(err).ifBlank { err }
                throw RuntimeException("HTTP $code $pretty")
            }

            return parseAssistantContentOrRaw(raw)

        } finally {
            runCatching { conn.disconnect() }
        }
    }

    private fun buildChatCompletionsBody(userText: String): JSONObject {
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "user").put("content", userText))
        }
        return JSONObject().put("model", model).put("messages", messages)
    }

    private fun parseAssistantContentOrRaw(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        return runCatching {
            val obj = JSONObject(trimmed)
            val choices = obj.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val first = choices.optJSONObject(0)
                val msg = first?.optJSONObject("message")
                return msg?.optString("content", "")?.trim().orEmpty()
            }
            trimmed
        }.getOrElse { trimmed }
    }

    private fun extractErrorMessage(raw: String): String {
        return runCatching {
            val obj = JSONObject(raw)
            obj.optJSONObject("error")?.optString("message") ?: ""
        }.getOrElse { "" }
    }

    private fun readStream(stream: InputStream?): String? {
        if (stream == null) return null
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }
}
