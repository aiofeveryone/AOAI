package com.aoai.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.aoai.chat.ai.AOAIEngine
import com.aoai.chat.p2p.AOAANodeManager
import com.aoai.chat.p2p.NodeForegroundService
import com.aoai.chat.p2p.ParticipationStore
import com.aoai.chat.ui.AOAIApp
import com.aoai.chat.ui.AOAITheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // ✅ Activity 재생성 대비: lazy로 생성 (MVP 안전장치)
    private val engine by lazy { AOAIEngine() }

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ✅ Android 13+ 알림 권한 런처
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // 권한 허용 후, 참여가 저장되어 있으면 서비스 자동 시작
            if (granted) {
                activityScope.launch {
                    val enabled = ParticipationStore.enabledFlow(this@MainActivity).first()
                    if (enabled) {
                        NodeForegroundService.start(this@MainActivity)
                    }
                }
            }
            // 거부된 경우: 서비스 시작하지 않음 (크래시 방지)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // ✅ 노드 매니저는 앱 컨텍스트로 초기화하는 게 안전
        AOAANodeManager.initialize(applicationContext)

        // ✅ (중요) Android 13+ 알림 권한 확인/요청
        // - 권한 없는데 서비스가 startForeground를 하면 일부 버전에서 즉시 크래시 가능
        ensureNotificationPermissionIfNeeded()

        setContent {
            AOAITheme {
                AOAIApp(engine)
            }
        }
    }

    private fun ensureNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            // 사용자에게 1회 요청
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // 이미 허용: 참여 저장 상태면 바로 서비스 시작 가능
            activityScope.launch {
                val enabled = ParticipationStore.enabledFlow(this@MainActivity).first()
                if (enabled) {
                    NodeForegroundService.start(this@MainActivity)
                }
            }
        }
    }
}