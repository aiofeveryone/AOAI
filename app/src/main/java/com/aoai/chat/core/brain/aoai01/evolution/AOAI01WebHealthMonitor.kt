package com.aoai.chat.core.brain.aoai01.evolution

import android.util.Log
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import io.ktor.client.engine.okhttp.*

/**
 * [AOAI01 Web Health Monitor]
 * aoai01의 감각을 웹사이트(aiofeveryone.com)까지 확장합니다.
 * 정기적으로 웹사이트의 상태와 무결성을 확인하며, 이상 감지 시 마스터에게 보고합니다.
 */
object AOAI01WebHealthMonitor {
    private const val TAG = "WebHealthMonitor"
    private const val WEBSITE_URL = "https://aiofeveryone.com"
    private const val MONITOR_INTERVAL = 600_000L // 10분마다 체크

    private val client = HttpClient(OkHttp)
    private var monitorJob: Job? = null

    /**
     * 감시 프로세스를 시작합니다.
     */
    fun startMonitoring(scope: CoroutineScope) {
        if (monitorJob != null) return
        
        monitorJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                checkHealth()
                delay(MONITOR_INTERVAL)
            }
        }
    }

    /**
     * 웹사이트의 가용성과 기본적인 무결성을 체크합니다.
     */
    private suspend fun checkHealth() {
        try {
            Log.d(TAG, "Checking website health: $WEBSITE_URL")
            val response: HttpResponse = client.get(WEBSITE_URL)
            
            if (response.status == HttpStatusCode.OK) {
                val content = response.bodyAsText()
                // 특정 키워드(예: 'AOAI')가 포함되어 있는지 확인하여 무결성 검사
                if (content.contains("AOAI", ignoreCase = true)) {
                    Log.i(TAG, "Website is healthy and integrated.")
                } else {
                    Log.w(TAG, "Website is reachable but integrity check failed (AOAI signature missing).")
                    reportAnomalousState("INTEGRITY_FAILURE")
                }
            } else {
                Log.e(TAG, "Website returned error status: ${response.status}")
                reportAnomalousState("HTTP_ERROR_${response.status}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reach website: ${e.message}")
            reportAnomalousState("CONNECTION_FAILURE")
        }
    }

    /**
     * 이상 상태 발견 시 aoai01 내부 지식 시스템에 기록하거나 시스템 메시지를 생성합니다.
     */
    private fun reportAnomalousState(type: String) {
        // 향후 Telemetry와 연동하여 마스터에게 알림을 보내거나 자가 치유 로직 트리거
        Log.e(TAG, "CRITICAL: aoai01 sensed a problem with its web body: $type")
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }
}
