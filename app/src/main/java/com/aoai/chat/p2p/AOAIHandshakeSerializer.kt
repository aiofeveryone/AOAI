package com.aoai.chat.p2p

import com.aoai.chat.core.AOAIHandshake
import com.aoai.chat.core.protocol.ProtocolKind
import com.aoai.chat.core.protocol.ProtocolParser
import org.json.JSONObject

object AOAIHandshakeSerializer {

    fun toJson(hs: AOAIHandshake): String {
        // ✅ kind 포함 (신규 버전 메시지)
        val json = ProtocolParser.withKind(ProtocolKind.HANDSHAKE)
            .put("protocolVersion", hs.protocolVersion)
            .put("nodeId", hs.nodeId)
            .put("deviceName", hs.deviceName)
            .put("supportsLocalAI", hs.supportsLocalAI)
            .put("supportsP2P", hs.supportsP2P)
            .put("timestamp", hs.timestamp)

        return json.toString()
    }

    fun fromJson(raw: String): AOAIHandshake {
        val json = JSONObject(raw)

        // ✅ 하위 호환: kind가 있으면 검증, 없으면 통과
        val kind = json.optString("kind", "")
        if (kind.isNotBlank()) {
            require(kind == ProtocolKind.HANDSHAKE.v) { "Not a handshake message: kind=$kind" }
        }

        // ✅ 필드 파싱 (구버전/누락 방어)
        val protocolVersion = json.optString("protocolVersion", "1.0")
        val nodeId = json.getString("nodeId") // 핵심 필드는 필수
        val deviceName = json.optString("deviceName", "unknown")
        val supportsLocalAI = json.optBoolean("supportsLocalAI", false)
        val supportsP2P = json.optBoolean("supportsP2P", true)
        val timestamp = json.optLong("timestamp", System.currentTimeMillis())

        return AOAIHandshake(
            protocolVersion = protocolVersion,
            nodeId = nodeId,
            deviceName = deviceName,
            supportsLocalAI = supportsLocalAI,
            supportsP2P = supportsP2P,
            timestamp = timestamp
        )
    }
}