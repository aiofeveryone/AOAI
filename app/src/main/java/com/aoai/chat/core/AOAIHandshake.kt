package com.aoai.chat.core

data class AOAIHandshake(

    val protocolVersion: String = "1.0",

    val nodeId: String,

    val deviceName: String,

    val supportsLocalAI: Boolean,

    val supportsP2P: Boolean,

    val timestamp: Long = System.currentTimeMillis()
)
