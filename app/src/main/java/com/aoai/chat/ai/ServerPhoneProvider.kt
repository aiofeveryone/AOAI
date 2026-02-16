package com.aoai.chat.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ServerPhoneProvider(
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    companion object {
        private const val DEFAULT_BASE_URL = "http://192.168.0.54:8080"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 20_000
    }

    private fun endpointAsk(): String = "${baseUrl.trimEnd('/')}/ask"

    suspend fun sendMessage(text: String): String = withContext(Dispatchers.IO) {
        Log.d("AOAI", "ServerPhone endpoint=${endpointAsk()}")

        val url = URL(endpointAsk())
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        val bodyJson = JSONObject().put("text", text).toString()

        conn.outputStream.use { os ->
            os.write(bodyJson.toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        val raw = when (code) {
            in 200..299 -> readStream(conn.inputStream).orEmpty()
            else -> {
                val err = readStream(conn.errorStream).orEmpty()
                conn.disconnect()
                throw RuntimeException("ServerPhone /ask failed: HTTP $code ${err.take(200)}".trim())
            }
        }

        conn.disconnect()

        runCatching {
            val obj = JSONObject(raw)
            val ans = obj.optString("answer", "").trim()
            if (ans.isNotBlank()) ans else raw
        }.getOrElse { raw }
    }

    private fun readStream(stream: InputStream?): String? {
        if (stream == null) return null
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }
}