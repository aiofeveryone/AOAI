package com.aoai.chat.core

object FeatureFlags {

    // --- Chat routing ---
    const val ENABLE_SERVERPHONE_FOR_CHAT: Boolean = true
    const val ENABLE_OPENAI_FOR_CHAT: Boolean = true
    const val ENABLE_LOCAL_FALLBACK_FOR_CHAT: Boolean = true
    
    // ✅ Added for AOAIEngine
    const val ENABLE_CLOUDFLARE_PROXY_FOR_CHAT: Boolean = true
}