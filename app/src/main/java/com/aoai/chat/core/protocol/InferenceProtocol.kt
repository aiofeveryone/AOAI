package com.aoai.chat.core.protocol

import org.json.JSONObject

object InferenceProtocol {

    data class InferRequest(
        val requestId: String,
        val prompt: String,
        val requiredNodes: Int,
        val timestamp: Long
    )

    data class InferResponse(
        val requestId: String,
        val nodeId: String,
        val answer: String,
        val confidence: Float
    )

    data class InferFinal(
        val requestId: String,
        val answer: String
    )

    fun encodeRequest(req: InferRequest): String =
        ProtocolParser.withKind(ProtocolKind.INFER_REQUEST)
            .put("requestId", req.requestId)
            .put("prompt", req.prompt)
            .put("requiredNodes", req.requiredNodes)
            .put("timestamp", req.timestamp)
            .toString()

    fun encodeResponse(res: InferResponse): String =
        ProtocolParser.withKind(ProtocolKind.INFER_RESPONSE)
            .put("requestId", res.requestId)
            .put("nodeId", res.nodeId)
            .put("answer", res.answer)
            .put("confidence", res.confidence)
            .toString()

    fun encodeFinal(fin: InferFinal): String =
        ProtocolParser.withKind(ProtocolKind.INFER_FINAL)
            .put("requestId", fin.requestId)
            .put("answer", fin.answer)
            .toString()

    fun decodeRequest(json: JSONObject): InferRequest? {
        val requestId = json.optString("requestId", "")
        val prompt = json.optString("prompt", "")
        if (requestId.isBlank() || prompt.isBlank()) return null
        return InferRequest(
            requestId = requestId,
            prompt = prompt,
            requiredNodes = json.optInt("requiredNodes", 3),
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }

    fun decodeResponse(json: JSONObject): InferResponse? {
        val requestId = json.optString("requestId", "")
        val nodeId = json.optString("nodeId", "")
        val answer = json.optString("answer", "")
        if (requestId.isBlank() || nodeId.isBlank() || answer.isBlank()) return null
        return InferResponse(
            requestId = requestId,
            nodeId = nodeId,
            answer = answer,
            confidence = json.optDouble("confidence", 0.5).toFloat()
        )
    }

    fun decodeFinal(json: JSONObject): InferFinal? {
        val requestId = json.optString("requestId", "")
        val answer = json.optString("answer", "")
        if (requestId.isBlank() || answer.isBlank()) return null
        return InferFinal(requestId, answer)
    }
}