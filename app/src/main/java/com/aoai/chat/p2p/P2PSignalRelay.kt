package com.aoai.chat.p2p

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class P2PSignalRelay(
    private val baseUrl: String,
    private val enabled: Boolean = true,   // ✅ 추가: 잠재 스위치
) {
    private fun enc(v: String) = URLEncoder.encode(v, "UTF-8")

    private fun endpointPost(): String =
        "${baseUrl.trimEnd('/')}/p2p/signal"

    private fun endpointGet(sessionId: String, type: String, consume: Boolean = true): String {
        val sid = enc(sessionId)
        val t = enc(type)
        val c = if (consume) "true" else "false"
        return "${baseUrl.trimEnd('/')}/p2p/signal?sessionId=$sid&type=$t&consume=$c"
    }

    suspend fun postOffer(sessionId: String, sdp: String, fromNodeId: String = "unknown") {
        if (!enabled) return  // ✅ 잠재면 아무것도 안 함
        postSignalJson(
            json = JSONObject()
                .put("type", "offer")
                .put("sessionId", sessionId)
                .put("sdp", sdp)
                .put("fromNodeId", fromNodeId)
                .toString()
        )
    }

    suspend fun getOffer(sessionId: String): String? {
        if (!enabled) return null // ✅ 잠재면 조회도 안 함
        return getSignalSdp(sessionId, type = "offer", consume = true)
    }

    suspend fun postAnswer(sessionId: String, sdp: String, fromNodeId: String = "unknown") {
        if (!enabled) return
        postSignalJson(
            json = JSONObject()
                .put("type", "answer")
                .put("sessionId", sessionId)
                .put("sdp", sdp)
                .put("fromNodeId", fromNodeId)
                .toString()
        )
    }

    suspend fun getAnswer(sessionId: String): String? {
        if (!enabled) return null
        return getSignalSdp(sessionId, type = "answer", consume = true)
    }

    private suspend fun postSignalJson(json: String) {
        withContext(Dispatchers.IO) {
            val url = URL(endpointPost())
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            conn.outputStream.use { os ->
                os.write(json.toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                val err = readStreamSafely(conn.errorStream)
                conn.disconnect()
                throw RuntimeException("postSignal failed: HTTP $code ${err?.take(200) ?: ""}".trim())
            }

            conn.disconnect()
        }
    }

    private suspend fun getSignalSdp(sessionId: String, type: String, consume: Boolean): String? {
        return withContext(Dispatchers.IO) {
            val url = URL(endpointGet(sessionId, type, consume))
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val code = conn.responseCode
            val body = when (code) {
                200 -> readStreamSafely(conn.inputStream)
                404 -> null
                else -> {
                    val err = readStreamSafely(conn.errorStream)
                    conn.disconnect()
                    throw RuntimeException("getSignal failed: HTTP $code ${err?.take(200) ?: ""}".trim())
                }
            }

            conn.disconnect()

            val jsonText = body?.trim()
            if (jsonText.isNullOrBlank()) return@withContext null

            val obj = runCatching { JSONObject(jsonText) }.getOrNull() ?: return@withContext null
            val sdp = obj.optString("sdp", "").trim()
            if (sdp.isBlank()) null else sdp
        }
    }

    private fun readStreamSafely(stream: java.io.InputStream?): String? {
        if (stream == null) return null
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }
}