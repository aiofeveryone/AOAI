package com.aoai.chat.ai

import com.aoai.chat.core.AiReply
import com.aoai.chat.core.ChatMessage
import com.aoai.chat.core.ChatSession
import kotlin.system.measureTimeMillis

class LocalRuleProvider : AiProvider {
    override val name: String = "local-rule"

    override suspend fun canHandle(session: ChatSession, messages: List<ChatMessage>): Boolean {
        return true // 지금은 항상 처리
    }

    override suspend fun reply(session: ChatSession, messages: List<ChatMessage>): AiReply {
        val last = messages.lastOrNull()?.text.orEmpty()
        var out = ""
        val t = measureTimeMillis {
            out = when {
                last.contains("정체", ignoreCase = true) -> "나는 AOAI의 로컬 엔진이야. 앞으로 P2P/로컬모델로 진화할 거야."
                last.contains("도움", ignoreCase = true) -> "무엇을 도와줄까? (UI/로컬모델/P2P 중 우선순위를 정해보자)"
                else -> "AOAI local response: $last"
            }
        }
        return AiReply(text = out, provider = name, latencyMs = t)
    }
}
