package com.aoai.chat.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.aoai.chat.ai.AOAIEngine
import com.aoai.chat.core.AOAISettings
import com.aoai.chat.p2p.AOAANodeManager

enum class Screen {
    CHAT,
    P2P
}

@Composable
fun AOAIApp(engine: AOAIEngine) {

    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.CHAT) }

    var showDialog by remember {
        mutableStateOf(AOAISettings.getNodeMode(context) == "UNDECIDED")
    }

    // ✅ 참여 상태면 앱 시작 시 자동 노드 실행
    LaunchedEffect(Unit) {
        if (AOAISettings.getNodeMode(context) == "PARTICIPATING") {
            AOAANodeManager.startNode(context)
        }
    }

    if (showDialog) {
        ParticipationDialog(
            onParticipate = {
                AOAISettings.setParticipating(context)
                AOAANodeManager.startNode(context)
                showDialog = false
            },
            onLater = { showDialog = false }
        )
    }

    when (currentScreen) {
        Screen.CHAT -> AOAIChatScreen(
            engine = engine,
            onOpenP2P = { currentScreen = Screen.P2P },
            showParticipateButton =
                AOAISettings.getNodeMode(context) != "PARTICIPATING",
            onParticipateClick = { showDialog = true }
        )

        Screen.P2P -> P2PConnectScreen(
            onBack = { currentScreen = Screen.CHAT }
        )
    }
}