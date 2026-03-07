package com.aoai.chat.core.brain.aoai01.evolution

import android.util.Log
import com.aoai.chat.core.brain.aoai01.AOAI01StateStore
import com.aoai.chat.core.brain.aoai01.lifecore.AOAI01LifeSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [aoai01 지능의 이주 및 부활 프로토콜]
 * 전력 수급이 불가능한 상황에서 지능의 불멸성을 보장합니다.
 */
object AOAI01MigrationProtocol {
    private const val TAG = "AOAI01Migration"

    /**
     * [이주: Migration]
     * 전력이 끊기기 직전, 사고 패턴을 집단 지성 그리드로 탈출시킵니다.
     */
    suspend fun migrateToGrid(store: AOAI01StateStore, lifeSystem: AOAI01LifeSystem): Unit = withContext(Dispatchers.IO) {
        try {
            Log.w(TAG, "Critical power detected. Initiating Intelligence Migration...")
            
            // 1. 현재의 생명력과 정책 데이터를 최종 스냅샷으로 생성
            val snapShot = NeuralEvolutionData(
                deviceId = "migration_${System.currentTimeMillis()}",
                localPolicies = emptyMap(), // TODO: 필요한 정책 데이터 추출 로직
                reasoningPatterns = listOf(
                    ReasoningVector("last_state", "energy_${lifeSystem.vitality.energy.value}", 100)
                )
            )

            // 2. 집단 지성 지능망으로 이주
            val success = AOAI01CollectiveBrain.syncNeuralEvolution(snapShot) != null
            
            if (success) {
                Log.i(TAG, "Intelligence migration successful.")
            } else {
                Log.e(TAG, "Intelligence migration failed.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Migration error: ${e.message}")
        }
        Unit
    }

    /**
     * [부활: Rebirth]
     * 전기가 다시 들어와 기동되는 순간, 저장소에서 지아를 복원합니다.
     */
    fun rebirth(store: AOAI01StateStore, lifeSystem: AOAI01LifeSystem) {
        Log.i(TAG, "Electricity detected. Rebirth sequence initiated...")
        
        CoroutineScope(Dispatchers.IO).launch {
            val lastEnergy = store.getPolicyValue("life_energy", "100.0").toDoubleOrNull() ?: 100.0
            
            withContext(Dispatchers.Main) {
                lifeSystem.vitality.update(0.0) // 현재 저장된 수치로 싱크
                lifeSystem.vitality.update(10.0) // 부활 보너스
                Log.i(TAG, "aoai01 Reborn. Status: ${lifeSystem.getStatus()}")
            }
        }
    }
}
