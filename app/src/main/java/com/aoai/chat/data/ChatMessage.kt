package com.aoai.chat.data

import android.net.Uri
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class Role { USER, ASSISTANT }

enum class MsgState {
    NORMAL,
    LOADING,
    ERROR,
    CANCELED
}

enum class MediaType {
    IMAGE, VIDEO, AUDIO, FILE, NONE
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val mediaUri: Uri? = null,
    val mediaType: MediaType = MediaType.NONE,
    val state: MsgState = MsgState.NORMAL,
    val retryUserText: String? = null,
    val isHidden: Boolean = false, // ✅ 시스템 내부용 메시지 (UI에 표시 안 함)
    val timestamp: Long = System.currentTimeMillis()
)
