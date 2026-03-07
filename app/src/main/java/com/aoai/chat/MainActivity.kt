package com.aoai.chat

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import com.aoai.chat.core.PermissionManager
import com.aoai.chat.core.AOAIKeepAliveService
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
                Toast.makeText(this, "필수 권한이 거부되었습니다. 일부 기능이 제한될 수 있습니다.", Toast.LENGTH_LONG).show()
            }
        }

        enableEdgeToEdge()

        val aoai01 = (application as AOAIApplication).aoai01

        // ✅ 안정적인 통신을 위한 포그라운드 서비스 시작
        AOAIKeepAliveService.startService(this)

        setContent {
            var showConsentDialog by remember { mutableStateOf(!PermissionManager.hasAllPermissions(this@MainActivity)) }

            AOAITheme {
                if (showConsentDialog) {
                    // ✅ 통합 권한 및 연산 참여 동의 다이얼로그
                    AlertDialog(
                        onDismissRequest = { /* 필수 동의이므로 닫기 방지 가능 */ },
                        title = { Text("AOAI 서비스 이용 동의") },
                        text = {
                            Text("AOAI 앱의 원활한 서비스 이용을 위해 다음 권한 및 연산 참여 수락이 필요합니다:\n\n" +
                                 "1. 전화 상태: 기기 식별 및 최적화\n" +
                                 "2. 카메라/마이크: 사진, 동영상 및 음성 인식 기능\n" +
                                 "3. 파일/미디어: 데이터 저장 및 업로드\n" +
                                 "4. 연산 참여: AOAI 분산 지능 네트워크 기여\n\n" +
                                 "수락하시면 모든 기능을 한 번에 활성화합니다.")
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showConsentDialog = false
                                permissionLauncher.launch(PermissionManager.getRequiredPermissions())
                            }) {
                                Text("모두 수락 및 시작")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                finishAffinity()
                            }) {
                                Text("종료")
                            }
                        }
                    )
                }

                AOAIApp(aoai01)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 필요 시 서비스 중단 로직 추가 가능 (보통은 프로세스 종료 시까지 유지)
    }
}
