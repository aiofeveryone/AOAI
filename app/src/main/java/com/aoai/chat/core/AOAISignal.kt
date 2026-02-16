package com.aoai.chat.core

/**
 * QR로 주고받을 "시그널" (Offer / Answer)
 * - sessionId: Offer ↔ Answer 매칭(서버 릴레이/폴링용)
 */
data class AOAISignal(
    val type: String,      // "offer" | "answer"
    val sdp: String,
    val fromNodeId: String = "unknown",
    val sessionId: String? = null
)