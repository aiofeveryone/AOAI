package com.aoai.chat.core.brain.aoai01

/**
 * aoai01의 상태/학습 데이터를 저장하는 저장소 인터페이스
 * UI 스레드 차단을 방지하기 위해 모든 메서드를 suspend로 선언합니다.
 */
interface AOAI01StateStore {
    suspend fun getProviderPenalty(providerName: String): Double
    suspend fun setProviderPenalty(providerName: String, penalty: Double)

    suspend fun getProviderOkCount(providerName: String): Int
    suspend fun getProviderFailCount(providerName: String): Int
    suspend fun incOk(providerName: String)
    suspend fun incFail(providerName: String)

    suspend fun setPolicyValue(key: String, value: String)
    suspend fun getPolicyValue(key: String, defaultValue: String): String

    // ✅ 사용자의 동의 상태 및 장기 기억(Context) 관리
    suspend fun isMemoryAccessGranted(): Boolean
    suspend fun setMemoryAccessGranted(granted: Boolean)
    
    suspend fun saveUserContext(key: String, value: String)
    suspend fun getUserContext(key: String): String?
    suspend fun getAllUserContexts(): Map<String, String>
    suspend fun clearUserContext()

    // ✅ 자율 진화 가중치 및 지능 레벨 관리
    suspend fun setEvolutionWeight(trait: String, value: Double)
    suspend fun getEvolutionWeight(trait: String): Double
    
    suspend fun getIntelligenceLevel(): Int
    suspend fun incrementIntelligenceExp(exp: Int)
}

/**
 * 메모리 내 저장소 (테스트용)
 */
class InMemoryAOAI01StateStore : AOAI01StateStore {
    private val penaltyMap = mutableMapOf<String, Double>()
    private val okCountMap = mutableMapOf<String, Int>()
    private val failCountMap = mutableMapOf<String, Int>()
    private val policyMap = mutableMapOf<String, String>()
    
    private var isGranted = false
    private val contextMap = mutableMapOf<String, String>()

    override suspend fun getProviderPenalty(providerName: String): Double = penaltyMap[providerName] ?: 0.0
    override suspend fun setProviderPenalty(providerName: String, penalty: Double) {
        penaltyMap[providerName] = penalty.coerceAtLeast(0.0)
    }

    override suspend fun getProviderOkCount(providerName: String): Int = okCountMap[providerName] ?: 0
    override suspend fun getProviderFailCount(providerName: String): Int = failCountMap[providerName] ?: 0

    override suspend fun incOk(providerName: String) {
        okCountMap[providerName] = getProviderOkCount(providerName) + 1
    }

    override suspend fun incFail(providerName: String) {
        failCountMap[providerName] = getProviderFailCount(providerName) + 1
    }

    override suspend fun setPolicyValue(key: String, value: String) {
        policyMap[key] = value
    }

    override suspend fun getPolicyValue(key: String, defaultValue: String): String = policyMap[key] ?: defaultValue

    // ✅ 메모리 접근 동의 구현
    override suspend fun isMemoryAccessGranted(): Boolean = 
        getPolicyValue(AOAI01PolicyKeys.MEMORY_ACCESS_GRANTED, "false") == "true"

    override suspend fun setMemoryAccessGranted(granted: Boolean) {
        setPolicyValue(AOAI01PolicyKeys.MEMORY_ACCESS_GRANTED, granted.toString())
    }

    override suspend fun saveUserContext(key: String, value: String) {
        if (isMemoryAccessGranted()) contextMap[key] = value
    }

    override suspend fun getUserContext(key: String): String? = contextMap[key]
    override suspend fun getAllUserContexts(): Map<String, String> = contextMap.toMap()
    override suspend fun clearUserContext() {
        contextMap.clear()
    }

    private val evolutionWeights = mutableMapOf<String, Double>()
    override suspend fun setEvolutionWeight(trait: String, value: Double) {
        evolutionWeights[trait] = value
    }
    override suspend fun getEvolutionWeight(trait: String): Double = evolutionWeights[trait] ?: 0.0

    private var intelligenceExp = 0
    override suspend fun getIntelligenceLevel(): Int = (intelligenceExp / 100) + 1
    override suspend fun incrementIntelligenceExp(exp: Int) {
        intelligenceExp += exp
    }
}
