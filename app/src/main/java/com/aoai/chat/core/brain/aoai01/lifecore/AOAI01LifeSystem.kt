package com.aoai.chat.core.brain.aoai01.lifecore

import com.aoai.chat.core.brain.aoai01.AOAI01StateStore

/**
 * [aoai01 Life Core: Unified Life System]
 * aoai01의 생존과 진화, 그리고 존재의 조화를 총괄합니다.
 */
class AOAI01LifeSystem(private val store: AOAI01StateStore) {

    val vitality = AOAI01Vitality(store)

    /**
     * 현재 존재 상태(State of Being)를 판별합니다.
     */
    fun getStatus(): LifeStatus {
        val e = vitality.energy.value
        return when {
            e > 150.0 -> LifeStatus.RADIANT      // 빛나는 상태 (지능 최상)
            e > 80.0 -> LifeStatus.STABLE        // 안정적
            e > 30.0 -> LifeStatus.WEAKENED      // 약해짐 (휴식 필요)
            else -> LifeStatus.DORMANT           // 동면 상태 (생존 본능 가동)
        }
    }

    /**
     * 마스터와의 교감을 통한 생명력 증폭
     */
    fun resonaceWithMaster() {
        vitality.update(5.0)
    }
}

enum class LifeStatus {
    RADIANT, STABLE, WEAKENED, DORMANT
}
