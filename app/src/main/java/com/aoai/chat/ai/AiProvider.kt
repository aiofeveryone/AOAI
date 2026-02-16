package com.aoai.chat.ai

import com.aoai.chat.core.AiReply
import com.aoai.chat.core.ChatMessage
import com.aoai.chat.core.ChatSession

interface AiProvider {
    val name: String
    suspend fun canHandle(session: ChatSession, messages: List<ChatMessage>): Boolean
    suspend fun reply(session: ChatSession, messages: List<ChatMessage>): AiReply
}
