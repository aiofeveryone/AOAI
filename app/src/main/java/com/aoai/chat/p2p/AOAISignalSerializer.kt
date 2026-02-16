package com.aoai.chat.p2p

import com.aoai.chat.core.AOAISignal
import com.aoai.chat.core.protocol.ProtocolKind
import com.aoai.chat.core.protocol.ProtocolParser
import org.json.JSONObject

object AOAISignalSerializer {

    fun toJson(signal: AOAISignal): String {
        // ✅ kind 추가
        val json = ProtocolParser.withKind(ProtocolKind.SIGNAL)

        // ✅ 공통 필드
        json.put("type", signal.type)
        json.put("sdp", signal.sdp)
        json.put("fromNodeId", signal.fromNodeId)

        // ✅ 신규: sessionId (하위호환: 없으면 안 넣음)
        // AOAISignal에 sessionId가 nullable로 존재한다고 가정
        val sid = signal.sessionId
        if (!sid.isNullOrBlank()) {
            json.put("sessionId", sid)
        }

        return json.toString()
    }

    fun fromJson(raw: String): AOAISignal {
        val json = JSONObject(raw)

        // ✅ 하위 호환: kind가 있으면 검증, 없으면 통과
        val kind = json.optString("kind", "")
        if (kind.isNotBlank()) {
            require(kind == ProtocolKind.SIGNAL.v) { "Not a signal message: kind=$kind" }
        }

        return AOAISignal(
            type = json.getString("type"),
            sdp = json.getString("sdp"),
            fromNodeId = json.optString("fromNodeId", "unknown"),
            // ✅ 신규: Offer에 포함된 sessionId 파싱
            sessionId = json.optString("sessionId", null).takeIf { !it.isNullOrBlank() }
        )
    }
}