package com.aoai.chat.core.brain.aoai01.persistence

import android.content.Context
import com.aoai.chat.core.brain.aoai01.AOAI01StateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Room Database를 사용하여 aoai01의 지능 데이터를 디바이스에 영구 저장합니다.
 * runBlocking을 제거하고 모든 메서드를 suspend와 Dispatchers.IO를 사용하여 구현합니다.
 */
class RoomAOAI01StateStore(context: Context) : AOAI01StateStore {
    private val dao = AOAI01Database.getDatabase(context).aoai01Dao()

    override suspend fun getProviderPenalty(providerName: String): Double = withContext(Dispatchers.IO) {
        dao.getProviderStats(providerName)?.penalty ?: 0.0
    }

    override suspend fun setProviderPenalty(providerName: String, penalty: Double) = withContext(Dispatchers.IO) {
        val current = dao.getProviderStats(providerName) ?: ProviderStatsEntity(providerName)
        dao.upsertProviderStats(current.copy(penalty = penalty.coerceAtLeast(0.0)))
    }

    override suspend fun getProviderOkCount(providerName: String): Int = withContext(Dispatchers.IO) {
        dao.getProviderStats(providerName)?.okCount ?: 0
    }

    override suspend fun getProviderFailCount(providerName: String): Int = withContext(Dispatchers.IO) {
        dao.getProviderStats(providerName)?.failCount ?: 0
    }

    override suspend fun incOk(providerName: String) = withContext(Dispatchers.IO) {
        val current = dao.getProviderStats(providerName) ?: ProviderStatsEntity(providerName)
        dao.upsertProviderStats(current.copy(okCount = current.okCount + 1))
    }

    override suspend fun incFail(providerName: String) = withContext(Dispatchers.IO) {
        val current = dao.getProviderStats(providerName) ?: ProviderStatsEntity(providerName)
        dao.upsertProviderStats(current.copy(failCount = current.failCount + 1))
    }

    override suspend fun setPolicyValue(key: String, value: String) = withContext(Dispatchers.IO) {
        dao.upsertPolicySetting(PolicySettingEntity(key, value))
    }

    override suspend fun getPolicyValue(key: String, defaultValue: String): String = withContext(Dispatchers.IO) {
        dao.getPolicySetting(key)?.value ?: defaultValue
    }

    // ✅ 메모리 접근 동의 상태 관리 (Policy 테이블 활용)
    override suspend fun isMemoryAccessGranted(): Boolean = withContext(Dispatchers.IO) {
        dao.getPolicySetting("memory_access_granted")?.value == "true"
    }

    override suspend fun setMemoryAccessGranted(granted: Boolean) = withContext(Dispatchers.IO) {
        dao.upsertPolicySetting(PolicySettingEntity("memory_access_granted", granted.toString()))
    }

    override suspend fun saveUserContext(key: String, value: String) = withContext(Dispatchers.IO) {
        if (isMemoryAccessGranted()) {
            dao.upsertUserContext(UserContextEntity(key, value))
        }
    }

    override suspend fun getUserContext(key: String): String? = withContext(Dispatchers.IO) {
        dao.getUserContext(key)?.value
    }

    override suspend fun getAllUserContexts(): Map<String, String> = withContext(Dispatchers.IO) {
        dao.getAllUserContexts().associate { it.key to it.value }
    }

    override suspend fun clearUserContext() = withContext(Dispatchers.IO) {
        dao.clearAllUserContexts()
    }
}
