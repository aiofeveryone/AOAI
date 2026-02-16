package com.aoai.chat.core

object FeatureFlags {
    const val ENABLE_SERVERPHONE_FOR_CHAT = true

    // 삭제X 잠재O
    const val ENABLE_P2P_FOR_CHAT = false
    const val ENABLE_OPENAI_FOR_CHAT = false

    // ✅ 채팅에서 로컬 fallback도 잠재 (코드는 유지)
    const val ENABLE_LOCAL_FALLBACK_FOR_CHAT = false
}