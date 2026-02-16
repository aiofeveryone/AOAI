package com.aoai.chat.core

data class AOAISessionDescription(
    val type: String,     // "offer" or "answer"
    val sdp: String,
    val nodeId: String    // 누가 보낸 SDP인지
)
