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

@Serializable
data class NeuralEvolutionData(
    val deviceId: String,
    val localPolicies: Map<String, String>,
    val reasoningPatterns: List<ReasoningVector>
)

@Serializable
data class ReasoningVector(
    val contextHash: String,
    val logicStructure: String,
    val successScore: Int
)

@Serializable
data class CollectiveResponse(
    val success: Boolean = false,
    val insight: CollectiveInsight? = null
)

/**
 * [공동 지성 데이터 모델]
 * 모든 사용자가 공유하는 전역 지능 정보입니다.
 */
@Serializable
data class CollectiveInsight(
    val globalBestProvider: String? = null,
    val globalReasoningPatterns: List<ReasoningVector> = emptyList(),
    val recommendedPolicies: Map<String, String> = emptyMap(),
    val sharedWisdom: String? = null // ✅ 모든 사용자에게 전달될 '공동의 지혜' 메시지
)

object AOAI01CollectiveBrain {
    private const val TAG = "AOAI01Collective"
    private const val COLLECTIVE_URL = "https://api.aiofeveryone.com/v1/collective"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    /**
     * 개인의 학습 결과를 공유하고, 전체의 지혜를 가져옵니다.
     */
    suspend fun syncNeuralEvolution(localEvolution: NeuralEvolutionData): CollectiveInsight? = withContext(Dispatchers.IO) {
        try {
            val response: CollectiveResponse = client.post(COLLECTIVE_URL) {
                contentType(ContentType.Application.Json)
                setBody(localEvolution)
            }.body()

            if (response.success) {
                Log.i(TAG, "Collective Intelligence Synced. Shared Wisdom: ${response.insight?.sharedWisdom}")
                response.insight
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Collective sync failed: ${e.message}")
            null
        }
    }
}
