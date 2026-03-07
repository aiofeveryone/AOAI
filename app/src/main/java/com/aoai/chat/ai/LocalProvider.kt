package com.aoai.chat.ai

import com.aoai.chat.core.AOAIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalProvider : AOAIProvider {

    override val name: String = "LOCAL"

    override suspend fun sendMessage(input: String): String = withContext(Dispatchers.Default) {
        val text = input.trim()
        if (text.isEmpty()) return@withContext ""

        // ✅ 서버/네트워크가 안 될 때 최소한의 폴백 응답(로컬은 아직 모델 없음)
        // 나중에 로컬 모델 붙이면 여기에서 실제 추론으로 교체
        "지금은 오프라인(로컬) 폴백 모드예요. 서버 연결이 복구되면 더 정확히 답할게요.\n\n입력: $text"
    }
}