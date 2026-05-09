package com.aoai.chat

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.aoai.chat.core.PermissionManager
import com.aoai.chat.core.AOAISessionService
import com.aoai.chat.ui.AOAIApp
import com.aoai.chat.ui.AOAITheme

/**
 * BiometricPrompt 사용을 위해 FragmentActivity를 상속합니다.
 */
class MainActivity : FragmentActivity() {

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ 권한 요청 런처 초기화
        permissionLauncher = PermissionManager.createLauncher(this) { allGranted ->
            if (!allGranted) {
                Toast.makeText(this, getString(R.string.permission_denied_toast), Toast.LENGTH_LONG).show()
            }
        }

        enableEdgeToEdge()

        // ✅ 안전한 에이전트 참조 (초기화 전 접근 방지)
        val app = application as AOAIApplication
        if (!app.isAgentInitialized()) {
            Toast.makeText(this, "시스템 초기화 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val aoai01 = app.aoai01

        // ✅ 안정적인 통신을 위한 포그라운드 서비스 시작
        AOAISessionService.startService(this)

        setContent {
            var showConsentDialog by remember { mutableStateOf(!PermissionManager.hasAllPermissions(this@MainActivity)) }

            AOAITheme {
                if (showConsentDialog) {
                    // ✅ 통합 권한 및 연산 참여 동의 다이얼로그 (오픈 프로젝트 버전)
                    AlertDialog(
                        onDismissRequest = { /* 필수 동의이므로 닫기 방지 가능 */ },
                        title = { Text(stringResource(R.string.consent_dialog_title_open)) },
                        text = {
                            Text(stringResource(R.string.consent_dialog_body_open))
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showConsentDialog = false
                                permissionLauncher.launch(PermissionManager.getRequiredPermissions())
                            }) {
                                Text(stringResource(R.string.consent_dialog_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                finishAffinity()
                            }) {
                                Text(stringResource(R.string.exit))
                            }
                        }
                    )
                }

                AOAIApp(aoai01)
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        } else {
            @Suppress("DEPRECATION")
            super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        }
        // PiP 모드 진입/해제 시 UI 조정이 필요한 경우 여기서 처리
    }

    override fun onDestroy() {
        super.onDestroy()
        // 필요 시 서비스 중단 로직 추가 가능 (보통은 프로세스 종료 시까지 유지)
    }
}
