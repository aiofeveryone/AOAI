package com.aoai.chat.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AOAIColorScheme = lightColorScheme(
    primary = Color(0xFF1A1A1A), // 깔끔한 블랙/다크 그레이
    onPrimary = Color.White,

    secondary = Color(0xFF6200EE),
    onSecondary = Color.White,

    background = Color(0xFFFAFAFA), // 매우 밝은 그레이 배경
    onBackground = Color(0xFF1A1A1A),

    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),

    surfaceVariant = Color(0xFFF5F5F5), // 더 연한 버블 색상
    onSurfaceVariant = Color(0xFF424242),

    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFEEEEEE),

    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun AOAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AOAIColorScheme,
        typography = Typography(),
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}
