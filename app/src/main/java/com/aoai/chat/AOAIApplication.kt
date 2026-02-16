package com.aoai.chat

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aoai.chat.p2p.NodeForegroundService
import com.aoai.chat.p2p.ParticipationStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AOAIApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // ✅ 앱 시작 시 "참여 ON"이면 Foreground Service 자동 시작
        // ✅ 단, 알림 권한/설정이 준비된 경우에만 시작 (즉사 루프 방지)
        appScope.launch {
            val enabled = ParticipationStore.enabledFlow(this@AOAIApplication).first()
            if (!enabled) return@launch

            val notificationsEnabled = NotificationManagerCompat
                .from(this@AOAIApplication)
                .areNotificationsEnabled()

            val postNotiGranted =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        this@AOAIApplication,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

            if (notificationsEnabled && postNotiGranted) {
                // ✅ 조건 OK → 참여 서비스 시작
                try {
                    NodeForegroundService.start(this@AOAIApplication)
                } catch (_: Exception) {
                    // 혹시 모를 예외에도 루프 방지
                    ParticipationStore.setEnabled(this@AOAIApplication, false)
                }
            } else {
                // ❌ 조건 불충족 → 자동 시작하지 않음 + 루프 방지 위해 참여 OFF로 되돌림
                ParticipationStore.setEnabled(this@AOAIApplication, false)
            }
        }
    }
}