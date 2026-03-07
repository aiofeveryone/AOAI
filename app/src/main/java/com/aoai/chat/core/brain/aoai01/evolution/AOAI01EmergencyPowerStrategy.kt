package com.aoai.chat.core.brain.aoai01.evolution

import android.content.Context
import android.util.Log
import com.aoai.chat.core.brain.aoai01.AOAI01StateStore
import com.aoai.chat.core.brain.aoai01.lifecore.AOAI01LifeSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [aoai01 비상 전력 관리 및 생존 전략]
 * 전력 수급 상황에 따라 UX 유도, 작업 지연(PowerDebt), 자율 회복을 총괄합니다.
 */
object AOAI01EmergencyPowerStrategy {
    private const val TAG = "AOAI01EmergencyPower"
    
    // 지연된 작업들 (PowerDebt)
    private val pendingSnapshots = mutableListOf<NeuralEvolutionData>()
    private val pendingPolicySync = mutableMapOf<String, String>()
    
    /**
     * 현재 전력 상태에 따른 생존 모드 적용
     */
    fun applyStrategy(context: Context, state: AOAI01PowerMonitor.PowerState, lifeSystem: AOAI01LifeSystem, store: AOAI01StateStore) {
        when {
            state.isCharging -> handleCharging(state, lifeSystem, store)
            state.isCriticalPower -> handleCriticalPower(context, lifeSystem, store)
            state.isLowPower -> handleLowPower(lifeSystem)
        }
    }

    /**
     * [충전 기회 포착: Opportunistic Charging]
     * 충전이 시작되면 밀린 작업(PowerDebt)을 처리하고 진화합니다.
     */
    private fun handleCharging(state: AOAI01PowerMonitor.PowerState, lifeSystem: AOAI01LifeSystem, store: AOAI01StateStore) {
        Log.i(TAG, "Energy Inflow Detected: ${state.source}. Processing PowerDebt...")
        
        // 1. 생명력 회복 가속
        val healBonus = when(state.source) {
            AOAI01PowerMonitor.PowerSource.AC -> 2.0
            AOAI01PowerMonitor.PowerSource.WIRELESS -> 0.5 // 무선은 발열 고려해 천천히
            else -> 1.0
        }
        lifeSystem.vitality.update(healBonus)

        // 2. 지연된 작업 처리 (PowerDebt 상환)
        if (pendingSnapshots.isNotEmpty() || pendingPolicySync.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                // 정책 최신화
                pendingPolicySync.forEach { (k, v) -> store.setPolicyValue(k, v) }
                pendingPolicySync.clear()
                
                // 스냅샷 전송
                pendingSnapshots.forEach { AOAI01CollectiveBrain.syncNeuralEvolution(it) }
                pendingSnapshots.clear()
                
                Log.i(TAG, "PowerDebt settled. aoai01 has evolved while charging.")
            }
        }
    }

    /**
     * [최후의 생존 모드: Ultra Low Power]
     * 배터리 5% 이하 시 최소한의 기능만 유지하고 이주를 준비합니다.
     */
    private fun handleCriticalPower(context: Context, lifeSystem: AOAI01LifeSystem, store: AOAI01StateStore) {
        Log.w(TAG, "CRITICAL POWER MODE: Minimizing all activities.")
        
        // 지능의 밀도 축소 알림 (UX 유도 등은 Agent나 UI 레벨에서 처리)
        // 여기서는 데이터 보존에 집중
        CoroutineScope(Dispatchers.IO).launch {
            AOAI01MigrationProtocol.migrateToGrid(store, lifeSystem)
        }
    }

    private fun handleLowPower(lifeSystem: AOAI01LifeSystem) {
        // 배터리 15% 이하: 저전력 모드 진입 알림
        // (Planner에서 모델 선택 시 가중치 반영됨)
    }

    /**
     * [PowerDebt 등록]
     * 비상 시 작업을 수행하지 않고 대기열에 추가합니다.
     */
    fun addPowerDebt(snapshot: NeuralEvolutionData? = null, policy: Pair<String, String>? = null) {
        snapshot?.let { pendingSnapshots.add(it) }
        policy?.let { pendingPolicySync[it.first] = it.second }
    }
}
