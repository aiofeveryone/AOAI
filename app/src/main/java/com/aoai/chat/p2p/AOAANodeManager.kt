package com.aoai.chat.p2p

import android.content.Context
import android.os.Build
import com.aoai.chat.core.AOAANode
import com.aoai.chat.core.AOAIHandshake
import java.util.UUID

object AOAANodeManager {

    private const val PREF_NAME = "aoai_node_pref"
    private const val KEY_NODE_ID = "node_id"

    private val peerLock = Any()
    private val peerNodes = mutableListOf<AOAANode>()

    @Volatile
    var selfNode: AOAANode? = null
        private set

    /**
     * ✅ AOAIApp.kt에서 호출하는 진입점
     * - 기존 코드가 startNode(context)를 호출해도 컴파일/동작하도록 제공
     * - 현재 단계에서 "노드 시작"은 initialize가 담당(노드ID 생성/유지 + selfNode 생성)
     */
    fun startNode(context: Context) {
        initialize(context)
    }

    /**
     * 🔥 앱 시작 시 호출
     * Node ID 영구 유지
     * 여러 번 호출돼도 안전하게(idempotent)
     */
    fun initialize(context: Context) {
        // 이미 초기화 됐으면 재초기화하지 않음
        if (selfNode != null) return

        val prefs = context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        var nodeId = prefs.getString(KEY_NODE_ID, null)
        if (nodeId == null) {
            nodeId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_NODE_ID, nodeId).apply()
        }

        val deviceName = "AOAI-${Build.MODEL}"

        selfNode = AOAANode(
            nodeId = nodeId,
            deviceName = deviceName,
            isOnline = true,
            supportsLocalAI = true,
            supportsP2P = true,
            lastLatencyMs = 0L
        )
    }

    /**
     * 🤝 Handshake 생성 (상대 노드로 전송용)
     */
    fun createHandshake(): AOAIHandshake {
        val node = selfNode ?: throw IllegalStateException("Node not initialized")

        return AOAIHandshake(
            protocolVersion = "1.0",
            nodeId = node.nodeId,
            deviceName = node.deviceName,
            supportsLocalAI = node.supportsLocalAI,
            supportsP2P = node.supportsP2P,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * 🤝 Handshake 수신 처리
     */
    fun receiveHandshake(handshake: AOAIHandshake) {
        val me = selfNode ?: return

        // ✅ 자기 자신이면 무시
        if (handshake.nodeId == me.nodeId) return

        val peerNode = AOAANode(
            nodeId = handshake.nodeId,
            deviceName = handshake.deviceName,
            isOnline = true,
            supportsLocalAI = handshake.supportsLocalAI,
            supportsP2P = handshake.supportsP2P,
            lastLatencyMs = 0L
        )

        addPeer(peerNode)
    }

    /**
     * Peer 추가 (동시성 보호)
     */
    private fun addPeer(node: AOAANode) = synchronized(peerLock) {
        peerNodes.removeAll { it.nodeId == node.nodeId }
        peerNodes.add(node)
    }

    /**
     * 현재 연결된 Peer 목록 반환 (동시성 보호)
     */
    fun getPeers(): List<AOAANode> = synchronized(peerLock) {
        peerNodes.toList()
    }

    /**
     * 가장 빠른 Peer 선택 (latency 기준, 동시성 보호)
     */
    fun getBestPeer(): AOAANode? = synchronized(peerLock) {
        peerNodes
            .filter { it.isOnline }
            .minByOrNull { it.lastLatencyMs }
    }
}