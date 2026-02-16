package com.aoai.chat.p2p

/**
 * P2P 연결/참여 상태를 관리하는 객체
 * - 현재는 Mock (나중에 WebRTC 연결/세션/피어 상태 반영)
 *
 * A안(현재 방식 + unregister에서 reset)
 * - register 시 evaluateAndNotifyEdge() 1회
 * - unregister 시 lastEligible 리셋 + startOnce 플래그 리셋
 * - 서비스가 켜질 때마다 현재 상태 기준 eligible이면 start 1회 보장
 */
object P2PManager {

    /** 현재 P2P 연결 상태 (Mock) */
    @Volatile
    var isConnected: Boolean = false
        private set

    /** "노드가 등록(register)되어 참여 상태인지" */
    @Volatile
    var isRegistered: Boolean = false
        private set

    /** "현재 참여 가능(eligible) 상태인지" */
    @Volatile
    var isEligible: Boolean = false
        private set

    /**
     * 마지막 eligible 시각 (epoch millis)
     * - 로그/쿨타임/통계용으로 유지 가능
     */
    @Volatile
    var lastEligible: Long = 0L
        private set

    /**
     * ✅ 이번 register 사이클에서 eligible-start를 이미 실행했는지 (1회 보장용)
     */
    @Volatile
    private var startedOnceThisCycle: Boolean = false

    fun connect() {
        isConnected = true
    }

    fun disconnect() {
        isConnected = false
        // 연결이 끊기면 등록/참여도 무효로 정리(원치 않으면 이 줄 제거)
        unregister()
    }

    /**
     * register: "내가 P2P 참여자(노드)로 등록됨"
     * ✅ A안: register 시 evaluateAndNotifyEdge() 1회
     */
    fun register() {
        isRegistered = true
        evaluateAndNotifyEdge() // ✅ 서비스 ON 시점에 현재 상태 반영 + start 1회 보장
    }

    /**
     * 현재 상태 기준으로 eligible 판정 + 필요한 동작 트리거
     * - 지금은 Mock: 연결되어 있으면 eligible
     */
    private fun evaluateAndNotifyEdge(nowMillis: Long = System.currentTimeMillis()) {
        val nowEligible = isConnected

        isEligible = nowEligible

        if (nowEligible) {
            lastEligible = nowMillis

            // ✅ eligible이면 start 1회 보장
            if (!startedOnceThisCycle) {
                startedOnceThisCycle = true
                onEligibleStartOnce()
            }
        }
    }

    /**
     * eligible 설정 (외부에서 상태를 강제로 갱신하고 싶을 때)
     * - 필요하면 markEligible()에서도 start 1회 보장을 유지
     */
    fun markEligible(nowMillis: Long = System.currentTimeMillis()) {
        isEligible = true
        lastEligible = nowMillis

        if (!startedOnceThisCycle) {
            startedOnceThisCycle = true
            onEligibleStartOnce()
        }
    }

    /** eligible 해제 */
    fun clearEligible() {
        isEligible = false
    }

    /**
     * unregister: "등록 해제(참여 종료)"
     *
     * ✅ 강추 수정 1 적용:
     * finally에서 lastEligible = 0L 리셋
     * + A안 start 1회 보장 플래그도 같이 리셋
     */
    fun unregister() {
        try {
            // TODO(WebRTC): peer close, signal relay stop, session cleanup 등
        } finally {
            isRegistered = false
            isEligible = false
            lastEligible = 0L                 // ✅ 핵심: unregister()에서 리셋
            startedOnceThisCycle = false      // ✅ 다음 서비스 ON에서 start 1회 다시 보장
        }
    }

    /**
     * eligible 진입 시 1회 실행되는 훅
     * - 여기서 실제 Edge/Participation start를 호출하면 됨
     */
    private fun onEligibleStartOnce() {
        // TODO: startEdgeParticipation()
        // 예) signal relay start, peer accept start, overlay start 등
    }
}