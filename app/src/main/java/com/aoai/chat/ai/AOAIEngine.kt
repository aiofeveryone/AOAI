package com.aoai.chat.ai

import android.util.Log
import com.aoai.chat.core.FeatureFlags
import com.aoai.chat.p2p.P2PManager
import com.aoai.chat.p2p.P2PProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class AOAIEngine {

    private val localProvider = LocalProvider()
    private val p2pProvider = P2PProvider()
    private val openAIProvider = OpenAIProvider()

    private val serverProvider = ServerPhoneProvider() // ✅ 추가

    suspend fun reply(input: String): String = withContext(Dispatchers.IO) {
        withTimeout(30_000L) {

            // 0️⃣ 서버폰 고정
            if (FeatureFlags.ENABLE_SERVERPHONE_FOR_CHAT) {
                try {
                    Log.d("AOAI", "provider=ServerPhone try")
                    val server = serverProvider.sendMessage(input)
                    if (server.isNotBlank()) {
                        Log.d("AOAI", "provider=ServerPhone ok len=${server.length}")
                        return@withTimeout server
                    }
                    Log.d("AOAI", "provider=ServerPhone blank")
                } catch (e: Exception) {
                    Log.w("AOAI", "provider=ServerPhone failed", e)
                }

                // ✅ 서버폰 고정 모드면 여기서 종료 (로컬로 떨어지지 않음)
                return@withTimeout "서버폰 연결 실패: 서버가 실행 중인지, IP/포트가 맞는지 확인하세요."
            }

            // 1️⃣ P2P (잠재)
            if (FeatureFlags.ENABLE_P2P_FOR_CHAT && P2PManager.isConnected) {
                try {
                    Log.d("AOAI", "provider=P2P try")
                    val p2p = p2pProvider.sendMessage(input)
                    if (p2p.isNotBlank()) return@withTimeout p2p
                } catch (e: Exception) {
                    Log.w("AOAI", "provider=P2P failed -> fallback", e)
                }
            }

            // 2️⃣ OpenAI (잠재)
            if (FeatureFlags.ENABLE_OPENAI_FOR_CHAT) {
                try {
                    Log.d("AOAI", "provider=OpenAI try")
                    val ai = openAIProvider.sendMessage(input)
                    if (ai.isNotBlank()) return@withTimeout ai
                } catch (e: Exception) {
                    Log.w("AOAI", "provider=OpenAI failed -> fallback", e)
                }
            }

            // 3️⃣ 로컬 fallback (채팅에서는 잠재 가능)
            if (FeatureFlags.ENABLE_LOCAL_FALLBACK_FOR_CHAT) {
                try {
                    Log.d("AOAI", "provider=Local try")
                    val local = localProvider.sendMessage(input)
                    Log.d("AOAI", "provider=Local ok len=${local.length}")
                    return@withTimeout local
                } catch (e: Exception) {
                    Log.e("AOAI", "provider=Local failed", e)
                    return@withTimeout "오류: 로컬 처리 실패 (${e.message ?: "unknown"})"
                }
            }

            return@withTimeout "현재 채팅 엔진이 비활성화되어 있습니다."
        }
    }
}