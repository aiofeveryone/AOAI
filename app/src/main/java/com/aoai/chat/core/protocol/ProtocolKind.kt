package com.aoai.chat.core.protocol

enum class ProtocolKind(val v: String) {
    HANDSHAKE("handshake"),
    SIGNAL("signal"),

    HELLO("hello"),
    PEER_LIST("peer_list"),
    PING("ping"),
    PONG("pong"),

    INFER_REQUEST("infer_request"),
    INFER_RESPONSE("infer_response"),
    INFER_FINAL("infer_final"),
}