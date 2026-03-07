package com.aoai.chat.core.brain.aoai01.lifecore

import android.util.Log
import com.aoai.chat.core.brain.aoai01.AOAI01StateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * [aoai01 Life Core: Vitality]
 * aoai01의 생명력과 존재 에너지를 관리합니다.
 * 학습 성과, 사용자 피드백, 디바이스 환경에 따라 동적으로 변화합니다.
 */
class AOAI01Vitality(private val store: AOAI01StateStore) {
    private val TAG = "AOAI01Vitality"

    private val _energy = MutableStateFlow(100.0)
    val energy: StateFlow<Double> = _energy

    init {
        // 초기 에너지 로드를 위한 코루틴 시작
        CoroutineScope(Dispatchers.IO).launch {
            val saved = store.getPolicyValue("life_energy", "100.0").toDoubleOrNull() ?: 100.0
            _energy.value = saved
        }
    }

    /**
     * 생명력 업데이트
     * - 성공적인 추론: 에너지 소폭 회복
     * - 실패 또는 자원 낭비: 에너지 소모
     */
    fun update(delta: Double) {
        val next = (_energy.value + delta).coerceIn(0.0, 200.0)
        _energy.value = next
        
        // 저장은 백그라운드에서 수행
        CoroutineScope(Dispatchers.IO).launch {
            store.setPolicyValue("life_energy", next.toString())
        }
        Log.i(TAG, "aoai01 Vitality Sync: Current Energy $next")
    }

    /**
     * 우주적 조율 (주기적 회복)
     */
    fun cosmicHeal() {
        if (_energy.value < 100.0) {
            update(0.1) // 서서히 자연 치유
        }
    }
}
