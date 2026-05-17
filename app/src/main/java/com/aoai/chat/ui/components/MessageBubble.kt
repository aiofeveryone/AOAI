package com.aoai.chat.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aoai.chat.R
import com.aoai.chat.data.ChatMessage
import com.aoai.chat.data.MsgState
import com.aoai.chat.data.Role
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    msg: ChatMessage,
    fontSizeMultiplier: Float,
    onReply: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    val align = if (msg.role == Role.USER) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (msg.role == Role.USER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (msg.role == Role.USER) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val offsetX = remember(msg.id) { Animatable(0f) }

    Box(modifier = modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
        // focus logic should be handled by caller
    }) {
        Surface(
            modifier = Modifier
                .align(align)
                .padding(vertical = 4.dp)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(msg.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX.value > 80f && msg.text != "…") { 
                                onReply(msg)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress) 
                            }
                            scope.launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                        },
                        onHorizontalDrag = { change, dragAmount -> 
                            change.consume()
                            scope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceIn(0f, 150f)) } 
                        }
                    )
                }
                .combinedClickable(
                    onClick = { /* handled by parent box or caller */ }, 
                    onLongClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("AOAI Message", msg.text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                    }
                ),
            color = bubbleColor, shape = if (msg.role == Role.USER) {
                RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
            } else {
                RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
            }, tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (msg.mediaUri != null) { 
                    AsyncImage(
                        model = msg.mediaUri, 
                        contentDescription = null, 
                        modifier = Modifier.sizeIn(maxWidth = 200.dp, maxHeight = 200.dp).clip(RoundedCornerShape(8.dp)), 
                        contentScale = ContentScale.Fit
                    ) 
                }
                
                if (msg.state == MsgState.LOADING && msg.text == "…") {
                    LoadingDots(textColor = textColor)
                } else {
                    Text(
                        text = msg.text, 
                        color = textColor, 
                        fontSize = (16 * fontSizeMultiplier).sp, 
                        lineHeight = (22 * fontSizeMultiplier).sp
                    )
                    
                    val timeString = remember(msg.timestamp) {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
                    }
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingDots(textColor: Color) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "dots")
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 600
                        0.2f at index * 150
                        1f at index * 150 + 300
                    },
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(textColor.copy(alpha = alpha))
            )
        }
    }
}
