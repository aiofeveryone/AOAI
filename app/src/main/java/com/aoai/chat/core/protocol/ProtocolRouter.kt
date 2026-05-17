package com.aoai.chat.core.protocol

class ProtocolRouter(
    private val onHandshake: (raw: String) -> Unit = {},
    private val onSignal: (raw: String) -> Unit = {},

    private val onInferRequest: (InferenceProtocol.InferRequest) -> Unit = {},
    private val onInferResponse: (InferenceProtocol.InferResponse) -> Unit = {},
    private val onInferFinal: (InferenceProtocol.InferFinal) -> Unit = {},

    private val onHello: (raw: String) -> Unit = {},
    private val onPeerList: (raw: String) -> Unit = {},
    private val onPing: (raw: String) -> Unit = {},
    private val onPong: (raw: String) -> Unit = {},
) {
    fun handle(raw: String) {
        val env = ProtocolParser.parse(raw) ?: return

        when (env.kind) {
            ProtocolKind.HANDSHAKE -> onHandshake(raw)
            ProtocolKind.SIGNAL -> onSignal(raw)

            ProtocolKind.INFER_REQUEST ->
                InferenceProtocol.decodeRequest(env.json)?.let(onInferRequest)

            ProtocolKind.INFER_RESPONSE ->
                InferenceProtocol.decodeResponse(env.json)?.let(onInferResponse)

            ProtocolKind.INFER_FINAL ->
                InferenceProtocol.decodeFinal(env.json)?.let(onInferFinal)

            ProtocolKind.HELLO -> onHello(raw)
            ProtocolKind.PEER_LIST -> onPeerList(raw)
            ProtocolKind.PING -> onPing(raw)
            ProtocolKind.PONG -> onPong(raw)
        }
    }
}