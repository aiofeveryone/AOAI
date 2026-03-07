package com.aoai.chat.core.brain.aoai01.evolution

import android.content.Context
import android.util.Log
import com.aoai.chat.core.brain.aoai01.PlanOutcome
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * [AOAI01 Telemetry]
 * 실행 결과(Outcome)를 수집하여 익명화된 통계 데이터를 서버로 전송합니다.
 * Wi-Fi 환경에서 배치 업로드를 수행하여 발전을 도모합니다.
 */
object AOAI01Telemetry {
    private const val TAG = "AOAI01Telemetry"
    private const val TELEMETRY_URL = "https://api.aiofeveryone.com/v1/telemetry"
    private const val QUEUE_FILE_NAME = "telemetry_queue.json"

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val queue = mutableListOf<PlanOutcome>()

    /**
     * 실행 결과를 큐에 추가합니다.
     */
    fun record(outcome: PlanOutcome, context: Context) {
        synchronized(queue) {
            queue.add(outcome)
            if (queue.size >= 10) {
                persistQueue(context)
            }
        }
        
        // 업로드 시도 (네트워크 조건 확인 후)
        scope.launch {
            tryUpload(context)
        }
    }

    private suspend fun tryUpload(context: Context) {
        // Wi-Fi 환경 체크 로직 (상징적)
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val isWifi = cm.activeNetwork?.let { 
            cm.getNetworkCapabilities(it)?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) 
        } ?: false

        if (!isWifi || queue.isEmpty()) return

        try {
            val toUpload = synchronized(queue) {
                val list = queue.toList()
                queue.clear()
                list
            }

            val response = client.post(TELEMETRY_URL) {
                contentType(ContentType.Application.Json)
                setBody(TelemetryBatch(toUpload))
            }

            if (response.status == HttpStatusCode.OK) {
                Log.i(TAG, "Telemetry batch uploaded successfully.")
                clearPersistedQueue(context)
            } else {
                // 실패 시 다시 큐에 삽입
                synchronized(queue) { queue.addAll(toUpload) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload telemetry: ${e.message}")
        }
    }

    private fun persistQueue(context: Context) {
        try {
            val file = File(context.filesDir, QUEUE_FILE_NAME)
            val data = synchronized(queue) { json.encodeToString(TelemetryBatch.serializer(), TelemetryBatch(queue)) }
            file.writeText(data)
        } catch (e: Exception) {
            Log.e(TAG, "Persist queue failed", e)
        }
    }

    private fun clearPersistedQueue(context: Context) {
        File(context.filesDir, QUEUE_FILE_NAME).delete()
    }
}

@Serializable
data class TelemetryBatch(
    val outcomes: List<PlanOutcome>
)
