package com.aoai.chat.core.brain.aoai01.evolution

import android.content.Context
import android.util.Log
import com.aoai.chat.core.brain.aoai01.AOAI01StateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * aoai01의 상태와 학습 데이터를 기기 디스크에 영구 저장하는 클래스입니다.
 */
class DiskAOAI01StateStore(context: Context) : AOAI01StateStore {
    private val file = File(context.filesDir, "aoai01_brain_state.json")
    private val data = JSONObject()
    private val TAG = "DiskAOAI01StateStore"

    private var memoryAccessGranted = false

    init {
        load()
    }

    private fun load() {
        if (file.exists()) {
            try {
                val content = file.readText()
                val json = JSONObject(content)
                json.keys().forEach { key ->
                    data.put(key, json.get(key))
                }
                memoryAccessGranted = data.optBoolean("memory_access_granted", false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load brain state", e)
            }
        }
    }

    private fun save() {
        try {
            data.put("memory_access_granted", memoryAccessGranted)
            file.writeText(data.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save brain state", e)
        }
    }

    fun getAllState(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        data.keys().forEach { key ->
            map[key] = data.get(key).toString()
        }
        return map
    }

    override suspend fun getProviderPenalty(providerName: String): Double = withContext(Dispatchers.IO) {
        data.optDouble("penalty_$providerName", 0.0)
    }

    override suspend fun setProviderPenalty(providerName: String, penalty: Double) {
        withContext(Dispatchers.IO) {
            data.put("penalty_$providerName", penalty.coerceAtLeast(0.0))
            save()
        }
    }

    override suspend fun getProviderOkCount(providerName: String): Int = withContext(Dispatchers.IO) {
        data.optInt("ok_$providerName", 0)
    }

    override suspend fun getProviderFailCount(providerName: String): Int = withContext(Dispatchers.IO) {
        data.optInt("fail_$providerName", 0)
    }

    override suspend fun incOk(providerName: String) {
        withContext(Dispatchers.IO) {
            val count = getProviderOkCount(providerName) + 1
            data.put("ok_$providerName", count)
            save()
        }
    }

    override suspend fun incFail(providerName: String) {
        withContext(Dispatchers.IO) {
            val count = getProviderFailCount(providerName) + 1
            data.put("fail_$providerName", count)
            save()
        }
    }

    override suspend fun setPolicyValue(key: String, value: String) {
        withContext(Dispatchers.IO) {
            data.put("policy_$key", value)
            save()
        }
    }

    override suspend fun getPolicyValue(key: String, defaultValue: String): String = withContext(Dispatchers.IO) {
        data.optString("policy_$key", defaultValue)
    }

    override suspend fun isMemoryAccessGranted(): Boolean = memoryAccessGranted

    override suspend fun setMemoryAccessGranted(granted: Boolean) {
        withContext(Dispatchers.IO) {
            memoryAccessGranted = granted
            save()
        }
    }

    override suspend fun saveUserContext(key: String, value: String) {
        withContext(Dispatchers.IO) {
            if (memoryAccessGranted) {
                val contextJson = data.optJSONObject("user_context") ?: JSONObject()
                contextJson.put(key, value)
                data.put("user_context", contextJson)
                save()
            }
        }
    }

    override suspend fun getUserContext(key: String): String? = withContext(Dispatchers.IO) {
        data.optJSONObject("user_context")?.optString(key)
    }

    override suspend fun getAllUserContexts(): Map<String, String> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, String>()
        data.optJSONObject("user_context")?.let { json ->
            json.keys().forEach { key ->
                map[key] = json.getString(key)
            }
        }
        map
    }

    override suspend fun clearUserContext() {
        withContext(Dispatchers.IO) {
            data.remove("user_context")
            save()
        }
    }

    // ✅ 자율 진화 가중치 및 지능 레벨 관리 (Evolutionary Persistence)
    override suspend fun setEvolutionWeight(trait: String, value: Double) {
        withContext(Dispatchers.IO) {
            data.put("evo_weight_$trait", value)
            save()
        }
    }

    override suspend fun getEvolutionWeight(trait: String): Double = withContext(Dispatchers.IO) {
        data.optDouble("evo_weight_$trait", 0.0)
    }

    override suspend fun getIntelligenceLevel(): Int = withContext(Dispatchers.IO) {
        val exp = data.optInt("intelligence_exp", 0)
        (exp / 100) + 1
    }

    override suspend fun incrementIntelligenceExp(exp: Int) {
        withContext(Dispatchers.IO) {
            val currentExp = data.optInt("intelligence_exp", 0)
            data.put("intelligence_exp", currentExp + exp)
            save()
        }
    }
}
