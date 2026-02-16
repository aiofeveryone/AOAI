package com.aoai.chat.data

import java.util.UUID

enum class Role { USER, ASSISTANT }

enum class MsgState {
    NORMAL,
    LOADING,
    ERROR,
    CANCELED
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val state: MsgState = MsgState.NORMAL,
    val retryUserText: String? = null
)