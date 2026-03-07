package com.aoai.chat.ai

import com.aoai.chat.core.AOAIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * (보관/유지용)
 * 기존 Rule 기반 응답을 AOAIProvider 인터페이스(sendMessage)에 맞춰 유지.
 *
 * ✅ 현재는 placeholder.
 * 나중에 규칙/패턴 기반 답변 로직을 아래 when 블록에 추가하면 됨.
 */
class LocalRuleProvider : AOAIProvider {

    override val name: String = "LOCAL_RULE"

    override suspend fun sendMessage(input: String): String = withContext(Dispatchers.Default) {
        val text = input.trim()
        if (text.isEmpty()) return@withContext ""

        // TODO: 기존 rule 로직이 있으면 여기로 옮기면 됨
        // 예시 템플릿:
        // return@withContext when {
        //     text.startsWith("도움말") -> "사용 방법: ..."
        //     text.contains("버전") -> "현재 버전은 ..."
        //     else -> ""
        // }

        // ✅ 룰이 아직 없으면 빈 문자열로 보내서 상위 fallback이 처리하게 하는게 가장 깔끔함
        ""
    }
}