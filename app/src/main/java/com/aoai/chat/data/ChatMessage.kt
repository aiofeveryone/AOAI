package com.aoai.chat.data

import android.net.Uri
import java.util.UUID

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
    @Deprecated("Use mediaUri and mediaType")
    val imageUri: Uri? = null
)
