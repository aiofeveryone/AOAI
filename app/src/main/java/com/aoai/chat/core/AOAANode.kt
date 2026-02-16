package com.aoai.chat.core

import java.util.UUID

data class AOAANode(

    val nodeId: String = UUID.randomUUID().toString(),

    val deviceName: String,

    val isOnline: Boolean = false,

    val supportsLocalAI: Boolean = true,

    val supportsP2P: Boolean = true,

    val lastLatencyMs: Long = 0L
)
