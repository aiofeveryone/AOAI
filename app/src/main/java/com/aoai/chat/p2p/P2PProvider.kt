package com.aoai.chat.p2p

import com.aoai.chat.core.AOAIProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class P2PProvider : AOAIProvider {

    override val name: String = "P2P"

    override suspend fun sendMessage(input: String): String {

        if (!P2PManager.isConnected) {
            // 연결 안 되어 있으면 실패 처리
            return ""
        }

        return withContext(Dispatchers.IO) {
            delay(400)
            "P2P node response: $input"
        }
    }
}
