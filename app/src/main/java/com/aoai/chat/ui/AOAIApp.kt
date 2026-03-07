package com.aoai.chat.ui

import androidx.compose.runtime.Composable
import com.aoai.chat.core.brain.aoai01.AOAI01Agent

@Composable
fun AOAIApp(agent: AOAI01Agent) {
    // ✅ p2p/참여 UI 제거 → 채팅 화면만 유지
    AOAIChatScreen(agent = agent)
}