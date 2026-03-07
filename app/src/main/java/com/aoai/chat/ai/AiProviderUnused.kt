package com.aoai.chat.ai

/**
 * (보관용)
 * 현재 앱은 AOAIEngine + AOAIProvider(sendMessage) 경로를 사용 중.
 * 향후 ChatSession/ChatMessage 기반으로 다시 마이그레이션할 때 쓰려고 남겨둠.
 */
@Deprecated("Unused for now. Kept for future migration.")
interface AiProviderUnused {
    val name: String
    suspend fun canHandle(session: Any, messages: List<Any>): Boolean
    suspend fun reply(session: Any, messages: List<Any>): Any
}