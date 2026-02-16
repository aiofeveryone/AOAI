package com.aoai.chat.core.protocol

import org.json.JSONObject

data class ProtocolEnvelope(
    val kind: ProtocolKind,
    val json: JSONObject
)

object ProtocolParser {
    private const val KEY_KIND = "kind"

    fun parse(raw: String): ProtocolEnvelope? {
        return runCatching {
            val json = JSONObject(raw)
            val kindStr = json.optString(KEY_KIND, "")
            val kind = ProtocolKind.entries.firstOrNull { it.v == kindStr } ?: return null
            ProtocolEnvelope(kind, json)
        }.getOrNull()
    }

    fun withKind(kind: ProtocolKind): JSONObject {
        return JSONObject().put(KEY_KIND, kind.v)
    }
}