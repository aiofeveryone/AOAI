package com.aoai.chat.ui.p2p

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 서버 주소/모드 선택 패널
 *
 * - endpoint: 현재 선택된 모드 (예: "AUTO", "LOCAL", "CUSTOM")
 * - resolvedBaseUrl: 실제로 사용될 baseUrl(미리보기)
 * - onModeChange: 모드 변경 콜백
 * - onCustomUrlChange: 커스텀 URL 입력 콜백
 */
@Composable
fun ServerEndpointPanel(
    endpoint: String,
    resolvedBaseUrl: String,
    onModeChange: (String) -> Unit,
    onCustomUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "서버 주소 모드",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        // 모드 선택 (문자열로 처리해서 기존 Store 타입과 충돌 최소화)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = endpoint == "AUTO",
                onClick = { onModeChange("AUTO") },
                label = { Text("AUTO") }
            )
            FilterChip(
                selected = endpoint == "LOCAL",
                onClick = { onModeChange("LOCAL") },
                label = { Text("LOCAL") }
            )
            FilterChip(
                selected = endpoint == "CUSTOM",
                onClick = { onModeChange("CUSTOM") },
                label = { Text("CUSTOM") }
            )
        }

        Spacer(Modifier.height(10.dp))

        // CUSTOM일 때만 입력 노출
        if (endpoint == "CUSTOM") {
            OutlinedTextField(
                value = resolvedBaseUrl, // 일단 현재값을 보여주되
                onValueChange = { onCustomUrlChange(it) },
                label = { Text("Custom Base URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "예: http://192.168.0.10:8080",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            // 미리보기 표시
            AssistChip(
                onClick = { /* no-op */ },
                label = { Text("Resolved: $resolvedBaseUrl") }
            )
        }
    }
}