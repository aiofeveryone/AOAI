package com.aoai.chat.p2p

import android.content.Context
import android.util.Log
import com.aoai.chat.core.AOAIHandshake
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

object P2PConnectionManager {

    private const val TAG = "P2PConnectionManager"

    /**
     * "연결됨" (peer와 WebRTC 세션/채널이 실제로 붙은 상태)
     * - WebRTC 연결 + DataChannel open 이후 true로 만드는 것을 권장
     */
    @Volatile
    var isConnected: Boolean = false
        private set

    /**
     * "노드 실행 중" (Foreground Service가 노드 역할을 수행하도록 켜진 상태)
     */
    private val nodeRunning = AtomicBoolean(false)

    /**
     * 연결 성립 시 Handshake를 이미 보냈는지(중복 전송 방지)
     * - 연결이 끊기면 false로 리셋
     */
    private val handshakeSent = AtomicBoolean(false)

    /**
     * 외부(서비스/화면/엔진)에서 상태 변화를 받기 위한 리스너
     */
    interface Listener {
        fun onNodeRunningChanged(running: Boolean) {}
        fun onConnectionChanged(connected: Boolean) {}
        fun onHandshakeReceived(handshake: AOAIHandshake) {}
        fun onError(where: String, t: Throwable) {}
    }

    private val listeners = CopyOnWriteArraySet<Listener>()

    fun addListener(l: Listener) {
        listeners.add(l)
    }

    fun removeListener(l: Listener) {
        listeners.remove(l)
    }

    fun isNodeRunning(): Boolean = nodeRunning.get()

    /**
     * 실제 WebRTC/Relay를 붙일 때 연결할 훅
     * - 지금은 Mock 상태라 no-op
     */
    data class Hooks(
        val startNodeStack: (Context) -> Unit = {},
        val stopNodeStack: () -> Unit = {},
        val sendData: (String) -> Unit = {},          // WebRTC DataChannel.send(json)
        val closeConnection: () -> Unit = {}          // PeerConnection/DataChannel close
    )

    @Volatile
    var hooks: Hooks = Hooks()

    /**
     * Foreground Service 표준 진입점: 노드 모드 시작
     */
    fun startNode(context: Context) {
        if (!nodeRunning.compareAndSet(false, true)) return

        val appCtx = context.applicationContext
        safeRun("startNode") {
            hooks.startNodeStack(appCtx)
            notifyNodeRunning(true)
            Log.i(TAG, "Node started")
        }.onFailure {
            nodeRunning.set(false)
            notifyNodeRunning(false)
        }
    }

    /**
     * Foreground Service 표준 진입점: 노드 모드 종료
     */
    fun stopNode() {
        if (!nodeRunning.compareAndSet(true, false)) return

        safeRun("stopNode") {
            // 필요하면 노드 종료 시 연결까지 끊고 싶을 수 있음:
            // disconnect() 호출을 정책으로 둘지 고민 가능 (여기선 분리 유지)
            hooks.stopNodeStack()
            notifyNodeRunning(false)
            Log.i(TAG, "Node stopped")
        }.onFailure {
            // 종료 중 예외는 치명적이지 않으므로 상태는 "중지"로 유지
            notifyNodeRunning(false)
        }
    }

    /**
     * 🔥 WebRTC 연결 성립 시점에 호출
     * - 권장: DataChannel open 이후 호출
     */
    fun onConnectionEstablished() {
        // 중복 호출 방어
        if (isConnected) return

        isConnected = true
        handshakeSent.set(false)

        // 엔진이 보는 연결 상태 동기화
        P2PManager.connect()
        notifyConnection(true)

        Log.i(TAG, "Connection established")

        // 연결되자마자 handshake 1회 송신
        sendHandshakeOnce()
    }

    /**
     * 🔥 네트워크(데이터채널)에서 메시지 수신 시 호출
     */
    fun onMessageReceived(json: String) {
        safeRun("onMessageReceived") {
            val handshake: AOAIHandshake = AOAIHandshakeSerializer.fromJson(json)
            Log.i(TAG, "Received handshake from ${handshake.deviceName}")

            // NodeManager에 등록
            AOAANodeManager.receiveHandshake(handshake)

            // 외부 리스너 통지
            listeners.forEach { it.onHandshakeReceived(handshake) }
        }
    }

    /**
     * ✅ 연결 단절 처리
     * - PeerConnection/DataChannel close 이후/직전에 호출해도 됨
     */
    fun disconnect() {
        if (!isConnected) return

        safeRun("disconnect") {
            isConnected = false
            handshakeSent.set(false)

            // 엔진 상태 동기화
            P2PManager.disconnect()
            notifyConnection(false)

            // 실제 종료 훅
            hooks.closeConnection()

            Log.i(TAG, "Disconnected")
        }
    }

    /**
     * Handshake를 단 한 번만 보냄 (연결당 1회)
     */
    private fun sendHandshakeOnce() {
        if (!isConnected) return
        if (!handshakeSent.compareAndSet(false, true)) return

        safeRun("sendHandshakeOnce") {
            val handshake = AOAANodeManager.createHandshake()
            val json = AOAIHandshakeSerializer.toJson(handshake)

            Log.i(TAG, "Sending handshake JSON")
            hooks.sendData(json)
        }.onFailure {
            // 실패했다면 재시도 가능하게 false로 돌려놓기
            handshakeSent.set(false)
        }
    }

    /**
     * ===== Mock 지원 =====
     * 지금처럼 "보내면 바로 받는" 시뮬레이션이 필요할 때만 사용.
     */
    fun sendToPeerMock(json: String) {
        Log.d(TAG, "Mock send → $json")
        onMessageReceived(json)
    }

    /**
     * ===== 내부 유틸 =====
     */
    private fun notifyNodeRunning(running: Boolean) {
        listeners.forEach { it.onNodeRunningChanged(running) }
    }

    private fun notifyConnection(connected: Boolean) {
        listeners.forEach { it.onConnectionChanged(connected) }
    }

    private fun safeRun(where: String, block: () -> Unit): Result<Unit> {
        return runCatching(block).onFailure { t ->
            Log.e(TAG, "Error at $where: ${t.message}", t)
            listeners.forEach { it.onError(where, t) }
        }
    }
}