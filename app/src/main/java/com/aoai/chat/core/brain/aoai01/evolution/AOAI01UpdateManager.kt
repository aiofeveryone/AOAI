package com.aoai.chat.core.brain.aoai01.evolution

import android.util.Log
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

/**
 * AOAI 앱의 새로운 버전을 감지하고 업데이트 경로를 관리하는 엔진입니다.
 * aoai01은 이 정보를 바탕으로 사용자에게 업데이트를 제안합니다.
 */
object AOAI01UpdateManager {
    private const val TAG = "AOAI01Update"
    private const val UPDATE_CHECK_URL = "https://api.aiofeveryone.com/v1/version"
    
    // ✅ 구글 플레이스토어 경로로 수정 (패키지명: com.aoai.chat)
    private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.aoai.chat"

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    @Serializable
    data class VersionInfo(
        val latestVersionCode: Int,
        val latestVersionName: String,
        val releaseNotes: String,
        val updateUrl: String? = null
    )

    /**
     * 서버에서 최신 버전 정보를 가져와 현재 버전과 비교합니다.
     */
    suspend fun checkNewVersion(currentVersionCode: Int): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for new version... Current: $currentVersionCode")
            
            // 실제 서버 응답을 시뮬레이션하거나 실제 API를 호출합니다.
            val info: VersionInfo = client.get(UPDATE_CHECK_URL).body()

            if (info.latestVersionCode > currentVersionCode) {
                Log.i(TAG, "New version detected: ${info.latestVersionName}")
                info
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check version: ${e.message}")
            null
        }
    }

    /**
     * 사용자에게 보여줄 업데이트 안내 메시지를 생성합니다.
     */
    fun getUpdatePrompt(info: VersionInfo): String {
        val finalUpdateUrl = info.updateUrl ?: PLAY_STORE_URL
        return """
[🚀 AOAI 새로운 버전 안내]
aoai01이 새로운 업데이트를 감지했습니다. 
더 똑똑해진 지능과 개선된 성능을 경험해 보세요!

- 최신 버전: ${info.latestVersionName}
- 업데이트 내용: ${info.releaseNotes}

지금 바로 업데이트하시겠습니까? 
아래 구글 플레이스토어 링크에서 최신 버전을 받으실 수 있습니다:
$finalUpdateUrl
""".trimIndent()
    }
}
