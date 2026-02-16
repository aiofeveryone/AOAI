package com.aoai.chat.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AOAIColorScheme = lightColorScheme(
    primary = Color(0xFF6A4FB3),
    onPrimary = Color.White,

    // 보조색(필수급)
    secondary = Color(0xFF5A46A0),
    onSecondary = Color.White,

    // 배경/서피스
    background = Color.White,
    onBackground = Color.Black,

    surface = Color.White,
    onSurface = Color.Black,

    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF2B2B2B),

    // UI 구성요소 기본색(필수급)
    outline = Color(0xFFCCCCCC),

    // 에러 처리(필수급)
    error = Color(0xFFB3261E),
    onError = Color.White,

    // M3 tonal/elevation tint
    surfaceTint = Color(0xFF6A4FB3)
)

@Composable
fun AOAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AOAIColorScheme,
        typography = Typography(),
        content = content
    )
}