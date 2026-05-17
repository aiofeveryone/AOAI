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
 * [AOAI01 AdaptivePolicyStore v3]
 * 무중단 지능 운영 체계 (Dynamic Workflow Control)
 */
object AOAI01AdaptivePolicyStore {
    private const val TAG = "AdaptivePolicyStore"
    private const val POLICY_URL = "https://api.aiofeveryone.com/v1/config/rules.json"
    
    private const val FILE_PRIMARY = "adaptive_rules_v3.json"
    private const val FILE_BACKUP = "adaptive_rules_v3.json.bak"

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
        prettyPrint = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    private var currentRules: AdaptiveRules? = null

    @Serializable
    data class PolicyResponse(
        val ok: Boolean = true,
        val data: AdaptiveRules? = null,
        val error: String? = null
    )

    suspend fun syncRemotePolicy(context: Context) = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Fetching dynamic intelligence policy...")
            val response: PolicyResponse = client.get(POLICY_URL).body()
            val remoteRules = response.data ?: throw Exception("Policy data is null")

            validateCompatibility(context, remoteRules)
            saveToLocalAtomic(context, remoteRules)
            currentRules = remoteRules
            Log.i(TAG, "Dynamic policy v${remoteRules.policy.policyVersion} synchronized.")
        } catch (e: Exception) {
            Log.e(TAG, "Policy sync failed: ${e.message}")
            loadWithRollback(context)
        }
    }

    fun getActiveRules(context: Context): AdaptiveRules {
        if (currentRules == null) loadWithRollback(context)
        return currentRules ?: AdaptiveRules()
    }

    private fun validateCompatibility(context: Context, rules: AdaptiveRules) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val appVersion = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
        if (appVersion < rules.compat.minAppVersionCode) throw Exception("App version too old")
    }

    private fun saveToLocalAtomic(context: Context, rules: AdaptiveRules) {
        try {
            val dir = context.filesDir
            val primaryFile = File(dir, FILE_PRIMARY)
            val backupFile = File(dir, FILE_BACKUP)
            if (primaryFile.exists()) primaryFile.renameTo(backupFile)
            primaryFile.writeText(json.encodeToString(AdaptiveRules.serializer(), rules))
        } catch (e: Exception) { Log.e(TAG, "Save failed", e) }
    }

    private fun loadWithRollback(context: Context) {
        val dir = context.filesDir
        val primaryFile = File(dir, FILE_PRIMARY)
        val backupFile = File(dir, FILE_BACKUP)
        try {
            if (primaryFile.exists()) {
                currentRules = json.decodeFromString(AdaptiveRules.serializer(), primaryFile.readText())
                return
            }
        } catch (e: Exception) { /* retry backup */ }
        try {
            if (backupFile.exists()) {
                currentRules = json.decodeFromString(AdaptiveRules.serializer(), backupFile.readText())
                return
            }
        } catch (e: Exception) { /* fail */ }
        currentRules = AdaptiveRules()
    }
}

@Serializable
data class AdaptiveRules(
    val schemaVersion: Int = 3,
    val policy: PolicyInfo = PolicyInfo(),
    val compat: CompatInfo = CompatInfo(),
    val runtime: RuntimeConfig = RuntimeConfig(),
    val intentPolicies: Map<String, IntentPolicy> = emptyMap(), // ✅ 의도별 워크플로우 제어
    val globalFeatureFlags: Map<String, Boolean> = emptyMap()
)

@Serializable
data class PolicyInfo(val policyVersion: Int = 1, val channel: String = "stable")

@Serializable
data class CompatInfo(val minAppVersionCode: Int = 100)

@Serializable
data class RuntimeConfig(
    val guardrails: Map<String, Map<String, Int>> = emptyMap(),
    val planner: Map<String, Map<String, Int>> = emptyMap()
)

/**
 * [의도별 운영 정책]
 * 특정 요청에 대해 검증 강제, 2단계 추론 여부 등을 제어합니다.
 */
@Serializable
data class IntentPolicy(
    val forceValidation: Boolean = false,
    val enableTwoStage: Boolean = true,
    val allowedStrategies: List<String> = listOf("LOCAL_ONLY", "SERVER_ONLY", "HYBRID"),
    val customSystemPrompt: String? = null
)
