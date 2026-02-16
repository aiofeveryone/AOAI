package com.aoai.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aoai.chat.core.ServerEndpoint
import com.aoai.chat.core.ServerMode

/**
 * ✅ 서버 주소 모드 패널 (공용)
 * - endpoint: 현재 선택 모드/커스텀 정보
 * - resolvedBaseUrl: 실제 요청에 쓰이는 URL (미리보기)
 * - onModeChange: 모드 변경 콜백
 * - onCustomUrlChange: 커스텀 URL 변경 콜백
 *
 * ⚠️ IMPORTANT
 * - 이 함수는 "오직 여기(ServerEndpointPanel.kt)에서만" 정의되어야 함
 * - P2PConnectScreen.kt 등 다른 파일에 동일 시그니처로 중복 정의하면 컴파일 에러(ambiguous) 발생
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerEndpointPanel(
    endpoint: ServerEndpoint,
    resolvedBaseUrl: String,
    onModeChange: (ServerMode) -> Unit,
    onCustomUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text("서버 주소 모드", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // ✅ LOCAL / LAN / OVERLAY
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = endpoint.mode == ServerMode.LOCAL,
                onClick = { onModeChange(ServerMode.LOCAL) },
                label = { Text("LOCAL") }
            )

            FilterChip(
                selected = endpoint.mode == ServerMode.LAN,
                onClick = { onModeChange(ServerMode.LAN) },
                label = { Text("LAN") }
            )

            FilterChip(
                selected = endpoint.mode == ServerMode.OVERLAY,
                onClick = { onModeChange(ServerMode.OVERLAY) },
                label = { Text("OVERLAY") }
            )
        }

        Spacer(Modifier.height(10.dp))

        AssistChip(
            onClick = { /* no-op */ },
            label = { Text("Resolved: $resolvedBaseUrl") }
        )

        Spacer(Modifier.height(10.dp))

        // ✅ 커스텀 입력 (보통 OVERLAY/LAN에서 쓰겠지만, 편의상 항상 노출)
        OutlinedTextField(
            value = endpoint.customBaseUrl.orEmpty(),
            onValueChange = { onCustomUrlChange(it) },
            label = { Text("Custom Base URL (선택)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("예: http://192.168.0.10:8080") }
        )
    }
}