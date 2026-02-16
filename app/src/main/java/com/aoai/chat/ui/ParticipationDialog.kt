package com.aoai.chat.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import com.aoai.chat.R
import com.aoai.chat.p2p.NodeForegroundService
import com.aoai.chat.p2p.ParticipationStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ParticipationDialog(
    onParticipate: () -> Unit,
    onLater: () -> Unit
) {
    val context = LocalContext.current
    val ioScope = remember { CoroutineScope(Dispatchers.IO) }

    // 권한(정확히는 "알림이 차단되어 있는지") 안내 다이얼로그 표시 상태
    val showNotificationRequiredDialog = remember { mutableStateOf(false) }

    fun areNotificationsEnabled(): Boolean {
        // POST_NOTIFICATIONS 권한/설정 거부는 최종적으로 notificationsEnabled=false로 잡히는 경우가 많음
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openAppNotificationSettings() {
        // 앱 알림 설정 화면으로 이동
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

            // 일부 기기 호환용(예전 방식)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                putExtra("app_package", context.packageName)
                putExtra("app_uid", context.applicationInfo.uid)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // ✅ 1) 메인 참여 다이얼로그
    AlertDialog(
        onDismissRequest = { /* forced choice */ },
        confirmButton = {
            Button(
                onClick = {
                    // ✅ 알림이 차단되어 있으면: 참여 시작 불가 → 안내 후 설정으로 유도
                    if (!areNotificationsEnabled()) {
                        showNotificationRequiredDialog.value = true
                        return@Button
                    }

                    // ✅ 알림 OK → 참여 저장 + 서비스 시작
                    ioScope.launch {
                        ParticipationStore.setEnabled(context, true)
                    }
                    NodeForegroundService.start(context)
                    onParticipate()
                }
            ) {
                Text(text = stringResource(R.string.p2p_join_confirm))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    // "나중에"는 참여 OFF로 명확히 하고 서비스 중지도 같이
                    ioScope.launch {
                        ParticipationStore.setEnabled(context, false)
                    }
                    NodeForegroundService.stop(context)
                    onLater()
                }
            ) {
                Text(text = stringResource(R.string.p2p_join_later))
            }
        },
        title = { Text(text = stringResource(R.string.p2p_join_title)) },
        text = {
            // p2p_join_body 문자열에 "Wi-Fi에서만 참여" 문구를 포함시키는 걸 권장
            Text(text = stringResource(R.string.p2p_join_body))
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )

    // ✅ 2) 알림 권한/설정 필요 안내 다이얼로그
    if (showNotificationRequiredDialog.value) {
        AlertDialog(
            onDismissRequest = { showNotificationRequiredDialog.value = false },
            title = { Text("알림 권한이 필요합니다") },
            text = {
                Text(
                    "참여 모드는 화면이 꺼진 상태에서도 동작하기 때문에 " +
                            "백그라운드 참여 상태를 알리는 알림(지속 알림)이 필요합니다.\n\n" +
                            "설정에서 AOAI의 알림을 허용해 주세요."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showNotificationRequiredDialog.value = false
                    openAppNotificationSettings()
                }) {
                    Text("설정으로 이동")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNotificationRequiredDialog.value = false }) {
                    Text("취소")
                }
            }
        )
    }
}