package com.aoai.chat.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aoai.chat.ai.AOAIEngine
import com.aoai.chat.data.ChatHistoryStore
import com.aoai.chat.data.ChatMessage
import com.aoai.chat.data.MsgState
import com.aoai.chat.data.Role
import com.aoai.chat.data.StoredChatMessage
import kotlinx.coroutines.*
import java.util.UUID

private const val MAX_UI_MESSAGES = 200
private const val P2P_PREFIX = "P2P node response:"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AOAIChatScreen(
    engine: AOAIEngine,
    onOpenP2P: () -> Unit,
    showParticipateButton: Boolean,
    onParticipateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var activeJob by remember { mutableStateOf<Job?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    val messages = remember { mutableStateListOf<ChatMessage>() }

    // 네트워크 상태
    val isOnline = rememberNetworkAvailable()

    // ==========================
    // 채팅 복원
    // ==========================
    LaunchedEffect(Unit) {
        val stored = ChatHistoryStore.load(context)
        messages.addAll(
            stored.map {
                ChatMessage(
                    role = if (it.role == Role.USER.name) Role.USER else Role.ASSISTANT,
                    text = it.text,
                    state = MsgState.NORMAL
                )
            }
        )
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    fun persist() {
        ChatHistoryStore.save(
            context,
            messages
                .filter { it.state == MsgState.NORMAL }
                .takeLast(MAX_UI_MESSAGES)
                .map {
                    StoredChatMessage(
                        role = it.role.name,
                        text = it.text,
                        ts = System.currentTimeMillis()
                    )
                }
        )
    }

    fun trimIfNeeded() {
        if (messages.size > MAX_UI_MESSAGES) {
            val removeCount = messages.size - MAX_UI_MESSAGES
            repeat(removeCount) { messages.removeAt(0) }
        }
    }

    fun cancelActiveRequest() {
        activeJob?.cancel()
        activeJob = null
        isSending = false
    }

    fun clearAllHistory() {
        cancelActiveRequest()
        messages.clear()
        ChatHistoryStore.clear(context)
        inputText = ""
    }

    fun sendMessage(text: String) {
        val input = text.trim()
        if (input.isEmpty()) return
        if (!isOnline || isSending) return

        // USER 메시지
        messages += ChatMessage(role = Role.USER, text = input, state = MsgState.NORMAL)
        trimIfNeeded()
        persist()

        inputText = ""

        // LOADING
        val loadingIndex = messages.size
        messages += ChatMessage(
            role = Role.ASSISTANT,
            text = "…",
            state = MsgState.LOADING,
            retryUserText = input
        )
        trimIfNeeded()

        isSending = true

        activeJob = scope.launch {
            try {
                Log.d("AOAI", "engine.reply() call: $input")
                val answer = engine.reply(input)

                messages[loadingIndex] = ChatMessage(
                    role = Role.ASSISTANT,
                    text = answer.ifBlank { "(빈 응답)" },
                    state = MsgState.NORMAL,
                    retryUserText = input
                )
                trimIfNeeded()
                persist()

            } catch (e: CancellationException) {
                messages[loadingIndex] = ChatMessage(
                    role = Role.ASSISTANT,
                    text = "요청이 취소되었습니다.",
                    state = MsgState.CANCELED,
                    retryUserText = input
                )
            } catch (e: Exception) {
                messages[loadingIndex] = ChatMessage(
                    role = Role.ASSISTANT,
                    text = "요청 실패",
                    state = MsgState.ERROR,
                    retryUserText = input
                )
            } finally {
                isSending = false
                activeJob = null
                if (messages.isNotEmpty()) {
                    runCatching { listState.animateScrollToItem(messages.lastIndex) }
                }
            }
        }
    }

    // ==========================
    // 전체 삭제 다이얼로그
    // ==========================
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("대화 전체 삭제") },
            text = { Text("저장된 최근 대화가 모두 삭제됩니다. 계속할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        clearAllHistory()
                    }
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("취소") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AOAI Chat") },
                actions = {
                    TextButton(onClick = onOpenP2P) { Text("P2P") }
                    if (showParticipateButton) {
                        TextButton(onClick = onParticipateClick) { Text("참여") }
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "대화 전체 삭제")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            if (!isOnline) {
                Text(
                    text = "오프라인: 네트워크에 연결되면 전송할 수 있습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
            }

            if (isSending) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val align =
                        if (msg.role == Role.USER) Alignment.CenterEnd
                        else Alignment.CenterStart

                    val isP2P =
                        msg.role == Role.ASSISTANT &&
                                msg.state == MsgState.NORMAL &&
                                msg.text.startsWith(P2P_PREFIX)

                    val bubbleColor = when {
                        msg.role == Role.USER -> MaterialTheme.colorScheme.primary
                        isP2P -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    val textColor = when {
                        msg.role == Role.USER -> MaterialTheme.colorScheme.onPrimary
                        isP2P -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .align(align)
                                .padding(vertical = 4.dp),
                            color = bubbleColor,
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val displayText =
                                    if (isP2P)
                                        msg.text.removePrefix(P2P_PREFIX).trimStart()
                                    else msg.text

                                Text(text = displayText, color = textColor)

                                val canRetry =
                                    msg.role == Role.ASSISTANT &&
                                            msg.state == MsgState.ERROR &&
                                            msg.retryUserText != null &&
                                            !isSending &&
                                            isOnline

                                if (canRetry) {
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(onClick = { sendMessage(msg.retryUserText!!) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "재전송",
                                            tint = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("무엇이든 물어보세요") },
                    enabled = !isSending && isOnline
                )

                Spacer(Modifier.width(8.dp))

                if (isSending) {
                    IconButton(onClick = { cancelActiveRequest() }) {
                        Icon(Icons.Filled.Close, contentDescription = "취소")
                    }
                } else {
                    IconButton(
                        onClick = { sendMessage(inputText) },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "전송",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}