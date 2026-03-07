package com.aoai.chat.ai

import com.aoai.chat.core.FeatureFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class AOAIEngine {

    private val localProvider = LocalProvider()

    // ✅ Cloudflare Worker 프록시(서버 키 보관) provider
    private val proxyProvider = CloudflareProxyProvider()

    /**
     * ✅ 단순화 버전:
     * - p2p 제거
     * - OpenAIProvider 제거
     * - Cloudflare Worker 프록시 우선, 실패 시 Local fallback (옵션)
     */
    suspend fun reply(input: String): String = withContext(Dispatchers.IO) {
        // ✅ 전체 타임아웃을 60초로 연장
        withTimeout(60_000L) {

            // 1) Cloudflare 프록시(외부 두뇌) 우선
            if (FeatureFlags.ENABLE_CLOUDFLARE_PROXY_FOR_CHAT) {
                return@withTimeout runCatching { proxyProvider.sendMessage(input) }
                    .getOrElse { "네트워크 연결 시간 초과 또는 프록시 오류: ${it.message ?: "unknown"}" }
                    .ifBlank { "프록시 연결 실패: 응답이 비어 있습니다." }
            }

            // 2) 로컬 fallback
            if (FeatureFlags.ENABLE_LOCAL_FALLBACK_FOR_CHAT) {
                return@withTimeout runCatching { localProvider.sendMessage(input) }
                    .getOrElse { "로컬 처리 실패: ${it.message ?: "unknown"}" }
            }

            return@withTimeout "현재 채팅 엔진이 비활성화되어 있습니다."
        }
    }
}
