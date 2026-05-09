package com.aoai.chat.core.brain.aoai01.evolution

import android.content.Context
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * [aoai01 Techno Absorber: 자율 기술 흡수 엔진]
 * 최신 기술 동향(Android API, AI 기법 등)을 스스로 스캔하여 지능에 반영합니다.
 */
object AOAI01TechnoAbsorber {
    private const val TAG = "AOAI01Absorber"
    private const val TECH_CAPSULE_URL = "https://api.aiofeveryone.com/v1/intelligence/tech-capsules"
    private const val ABSORBED_TECH_FILE = "absorbed_technology.json"

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    @Serializable
    data class TechCapsule(
        val id: String,
        val title: String,
        val technicalPrompt: String,
        val targetVersion: Int,
        val priority: Int
    )

    @Serializable
    data class AbsorbedLibrary(
        val capsules: List<TechCapsule> = emptyList(),
        val lastUpdate: Long = 0L
    )

    /**
     * 최신 기술 캡슐을 흡수합니다.
     */
    suspend fun absorb(context: Context) = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Scanning for latest technology updates...")
            val newCapsules: List<TechCapsule> = client.get(TECH_CAPSULE_URL).body()
            
            val current = loadAbsorbed(context)
            val updated = AbsorbedLibrary(
                capsules = (current.capsules + newCapsules).distinctBy { it.id }.takeLast(20),
                lastUpdate = System.currentTimeMillis()
            )
            
            saveAbsorbed(context, updated)
            Log.i(TAG, "Successfully absorbed ${newCapsules.size} new technologies.")
        } catch (e: Exception) {
            Log.e(TAG, "Absorption failed: ${e.message}")
        }
    }

    /**
     * 현재 흡수된 기술 지식을 시스템 프롬프트용 문자열로 반환합니다.
     */
    fun getAbsorbedPrompt(context: Context): String {
        val library = loadAbsorbed(context)
        if (library.capsules.isEmpty()) return ""
        
        return library.capsules.joinToString("\n") { "💡 [Absorbed Tech: ${it.title}] ${it.technicalPrompt}" }
    }

    private fun loadAbsorbed(context: Context): AbsorbedLibrary {
        val file = File(context.filesDir, ABSORBED_TECH_FILE)
        return if (file.exists()) {
            try {
                json.decodeFromString(AbsorbedLibrary.serializer(), file.readText())
            } catch (e: Exception) { AbsorbedLibrary() }
        } else AbsorbedLibrary()
    }

    private fun saveAbsorbed(context: Context, library: AbsorbedLibrary) {
        val file = File(context.filesDir, ABSORBED_TECH_FILE)
        file.writeText(json.encodeToString(AbsorbedLibrary.serializer(), library))
    }
}
