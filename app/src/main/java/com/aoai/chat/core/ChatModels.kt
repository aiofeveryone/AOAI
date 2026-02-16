package com.aoai.chat.core

import java.util.UUID

enum class Role { USER, AOAI, SYSTEM }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val createdAtMs: Long = System.currentTimeMillis()
)

data class AiReply(
    val text: String,
    val provider: String,
    val latencyMs: Long
)
