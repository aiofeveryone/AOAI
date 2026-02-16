package com.aoai.chat.ai

import com.aoai.chat.core.AOAIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LocalProvider
 *
 * AOAI의 기본 로컬 처리 엔진.
 * 네트워크 없이 항상 동작.
 */

class LocalProvider : AOAIProvider {

    override val name: String = "LOCAL"

    override suspend fun sendMessage(input: String): String {

        return withContext(Dispatchers.Default) {

            when {
                input.contains("정체") ->
                    "나는 AOAI의 로컬 프로바이더야. P2P가 연결되면 분산 처리도 가능해."

                input.contains("도움") ->
                    "로컬, P2P, 모델 연결 중 어떤 구조부터 확장할까?"

                input.contains("p2p", ignoreCase = true) ->
                    "P2P 노드 연결은 아직 준비 중이야."

                else ->
                    "Local response: $input"
            }
        }
    }
}
