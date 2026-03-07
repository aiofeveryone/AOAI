package com.aoai.chat.core.brain.aoai01

import kotlinx.serialization.Serializable

@Serializable
data class AOAIContext(
    val userText: String,
    val recentSummary: String? = null,
    val device: DeviceStateInfo,
    val network: NetworkStateInfo,
    val settings: UserSettingsState,
    val deviceGrade: String, 
    val platformType: PlatformType = PlatformType.ANDROID,
    val memoryHits: List<String> = emptyList(),
    val cacheHit: String? = null
)

enum class PlatformType { ANDROID, IOS, WEB_PC, WEB_MOBILE, IOT_DEVICE }

@Serializable
data class DeviceStateInfo(
    val batteryPct: Int,
    val isCharging: Boolean,
    val thermalState: String,
    val cpuUsage: Int? = null,
    val browserInfo: String? = null
)

@Serializable
data class NetworkStateInfo(
    val isOnline: Boolean = false,
    val isWifi: Boolean = false,
    val isMetered: Boolean = false,
    val strength: String = "GOOD", 
    val description: String = "",
    val rttMs: Int? = null
)

@Serializable
data class UserSettingsState(
    val preferLocal: Boolean = false,
    val verbosity: Int = 5
)

@Serializable
data class IntentAnalysis(
    val type: IntentType,
    val complexity: Complexity = Complexity.LOW,
    val needsClarification: Boolean = false,
    val riskLevel: SecurityLevel = SecurityLevel.SAFE
)

enum class Complexity { LOW, MEDIUM, HIGH, EXTREME }
enum class SecurityLevel { SAFE, SUSPICIOUS, THREAT }

@Serializable
data class Plan(
    val analysis: IntentAnalysis,
    val strategy: PlanStrategy,
    val steps: List<PlanStep>,
    val output: OutputSpec,
    val memoryPolicy: MemoryPolicy,
    val evalPolicy: EvalPolicy,
    val policyVersion: Int 
)

enum class IntentType {
    CHAT, TRANSLATE, SUMMARIZE, CODE, TROUBLESHOOT, SEARCH, SETTINGS, INTERNAL_SECURITY, CLARIFICATION_REQUEST,
    ACTION_REQUEST // ✅ 자동화 요청 추가
}

enum class PlanStrategy {
    LOCAL_ONLY, SERVER_ONLY, HYBRID, CACHE_ONLY
}

@Serializable
sealed class PlanStep {
    @Serializable
    data class UseCache(val key: String): PlanStep()
    @Serializable
    data class RetrieveMemory(val query: String, val topK: Int): PlanStep()
    @Serializable
    data class RunDraftReasoning(val modelTier: String): PlanStep()
    @Serializable
    data class RunReasoning(val provider: String, val modelName: String): PlanStep()
    @Serializable
    data class ValidateOutput(val criteria: String): PlanStep()
    @Serializable
    data class PostProcess(val format: String): PlanStep()
    
    // ✅ 지능적 자동화를 위한 실행 단계 추가
    @Serializable
    data class ExecuteDeviceAction(val actionType: String, val params: Map<String, String>): PlanStep()
}

@Serializable
data class OutputSpec(
    val maxTokensHint: Int,
    val style: String
)

enum class MemoryPolicy {
    NONE, SAVE_SUMMARY, SAVE_FACTS, SAVE_ACTION_ITEMS
}

@Serializable
data class EvalPolicy(
    val enable: Boolean,
    val metrics: List<String>
)

@Serializable
data class PlanOutcome(
    val timestamp: Long,
    val policyVersion: Int,
    val deviceGrade: String,
    val platformType: String,
    val usedStrategy: PlanStrategy,
    val modelName: String?,
    val latencyMs: Long,
    val success: Boolean,
    val fallbackReason: String? = null,
    val cacheHit: Boolean = false,
    val errorCode: String? = null,
    val userSatisfaction: Int? = null,
    val actionResults: Map<String, String>? = null // ✅ 실행 결과 추가
)
