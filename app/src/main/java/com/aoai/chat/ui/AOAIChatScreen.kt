package com.aoai.chat.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aoai.chat.BuildConfig
import com.aoai.chat.R
import com.aoai.chat.core.brain.aoai01.AOAI01Agent
import com.aoai.chat.core.brain.aoai01.AOAI01MasterGuardian
import com.aoai.chat.core.brain.aoai01.NetworkStateInfo
import com.aoai.chat.data.ChatMessage
import com.aoai.chat.data.MsgState
import com.aoai.chat.data.Role
import com.aoai.chat.ui.components.*
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AOAIChatScreen(
    agent: AOAI01Agent,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val uiState by agent.uiState.collectAsStateWithLifecycle()
    val messages = uiState.messages
    val isSending = uiState.isSending

    var inputText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCommunityMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val isOnline = rememberNetworkAvailable()
    var currentNetworkInfo by remember { mutableStateOf<NetworkStateInfo?>(null) }
    var fontSizeMultiplier by remember { mutableStateOf(1f) }
    val isMasterMode by remember { mutableStateOf(AOAI01MasterGuardian.isOverrideActive()) }

    // TTS & Language Identification
    var selectedLocale by remember { mutableStateOf(Locale.KOREAN) }
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    val languageIdentifier = remember { LanguageIdentification.getClient() }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.language = selectedLocale
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { scope.launch { isSpeaking = true } }
                    override fun onDone(utteranceId: String?) { scope.launch { isSpeaking = false } }
                    override fun onError(utteranceId: String?) { scope.launch { isSpeaking = false } }
                })
            }
        }
        onDispose { 
            tts?.stop()
            tts?.shutdown() 
            isTtsReady = false
        }
    }

    LaunchedEffect(selectedLocale, isTtsReady) { if (isTtsReady) tts?.language = selectedLocale }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val msg = if (results.values.all { it }) R.string.permission_granted_toast else R.string.permission_denied_toast
        Toast.makeText(context, context.getString(msg), Toast.LENGTH_SHORT).show()
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.getOrNull(0)
            if (!spokenText.isNullOrBlank()) { inputText = spokenText }
        }
    }

    fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLocale)
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.stt_prompt))
        }
        try { speechLauncher.launch(intent) } catch (e: Exception) { Toast.makeText(context, context.getString(R.string.stt_not_supported), Toast.LENGTH_SHORT).show() }
    }

    fun speakTargetMessage() {
        if (isSpeaking) { tts?.stop(); isSpeaking = false; return }
        val targetMsg = replyingTo ?: messages.lastOrNull { it.role == Role.ASSISTANT && it.state == MsgState.NORMAL }
        val textToSpeak = targetMsg?.text
        if (!textToSpeak.isNullOrBlank()) {
            scope.launch {
                try {
                    val languageCode = languageIdentifier.identifyLanguage(textToSpeak).await()
                    if (languageCode != "und") {
                        val detectedLocale = Locale(languageCode)
                        selectedLocale = detectedLocale
                        tts?.language = detectedLocale
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.d("AOAI_TTS", "Language detection failed: ${e.message}")
                }
                tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "AOAI_TTS")
            }
        } else {
            Toast.makeText(context, context.getString(R.string.no_content_to_read), Toast.LENGTH_SHORT).show()
        }
    }

    fun onSend(text: String) {
        var input = text.trim()
        val currentReplyingTo = replyingTo
        if (input.isEmpty() && currentReplyingTo == null) return
        if (!isOnline) return
        
        keyboardController?.hide()

        // Handle language/translate requests, 119 calls, and permissions via keywords (simplified logic)
        if (input.contains("119") && (input.contains("신고") || input.contains("전화"))) {
            try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:119"))) }
            catch (e: Exception) { Toast.makeText(context, context.getString(R.string.call_failed), Toast.LENGTH_SHORT).show() }
        }

        if (currentReplyingTo != null) {
            val replyText = currentReplyingTo.text
            input = if (input.isEmpty()) context.getString(R.string.continue_conversation_prompt, replyText)
                    else context.getString(R.string.reply_to_prompt, replyText, input)
        }

        inputText = ""
        replyingTo = null
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch { agent.send(input) }
    }

    LaunchedEffect(Unit) { 
        agent.attachContext(context)
        NetworkStatus.observeNetworkState(context).collectLatest { currentNetworkInfo = it }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) { listState.animateScrollToItem(messages.lastIndex) }
    }

    // Dialogs & Bottom Sheet
    if (showAboutDialog) AboutDialog { showAboutDialog = false }
    if (showLanguageDialog) LanguageDialog(selectedLocale, { selectedLocale = it }, { showLanguageDialog = false })
    if (showClearDialog) ClearChatDialog({ agent.clearChat() }, { showClearDialog = false })
    if (showHistorySheet) {
        ChatHistoryBottomSheet(messages, sheetState, { msg ->
            val index = messages.indexOfFirst { it.id == msg.id }
            if (index != -1) { scope.launch { listState.animateScrollToItem(index) } }
        }, { showHistorySheet = false })
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = { IconButton(onClick = { showHistorySheet = true }) { Icon(Icons.Filled.History, null) } },
                title = {
                    val summary = messages.lastOrNull { it.role == Role.USER && !it.isHidden }?.text ?: stringResource(R.string.new_conversation)
                    Column(modifier = Modifier.clickable { showHistorySheet = true }) {
                        Text(summary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (isMasterMode) stringResource(R.string.master_mode_label) else stringResource(R.string.view_previous_conversations), style = MaterialTheme.typography.labelSmall, color = if (isMasterMode) Color.Red else MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { showCommunityMenu = true }) { Icon(Icons.Default.Groups, null) }
                    DropdownMenu(expanded = showCommunityMenu, onDismissRequest = { showCommunityMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.menu_about)) }, leadingIcon = { Icon(Icons.Default.Info, null) }, onClick = { showCommunityMenu = false; showAboutDialog = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.menu_contribute_github)) }, leadingIcon = { Icon(Icons.Default.Code, null) }, onClick = { showCommunityMenu = false; context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.github_url)))) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.community_discord)) }, leadingIcon = { Icon(Icons.Default.Chat, null) }, onClick = { showCommunityMenu = false; context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.discord_url)))) })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(stringResource(R.string.menu_report_bug)) }, leadingIcon = { Icon(Icons.Default.BugReport, null) }, onClick = { showCommunityMenu = false; context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.github_url) + "/issues"))) })
                    }
                    IconButton(onClick = { agent.clearChatUIOnly(); Toast.makeText(context, context.getString(R.string.chat_cleared_toast), Toast.LENGTH_SHORT).show() }) {
                        Image(painter = painterResource(id = R.mipmap.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = { showClearDialog = true }) { Icon(Icons.Filled.Delete, null) } 
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .pointerInput(Unit) { detectTapGestures(onTap = { focusRequester.requestFocus(); keyboardController?.show() }) }
                .pointerInput(Unit) { detectTransformGestures { _, _, zoom, _ -> fontSizeMultiplier = (fontSizeMultiplier * zoom).coerceIn(0.8f, 2.5f) } }
        ) {
            currentNetworkInfo?.let { info ->
                val icon = if (info.isWifi) "📡" else "📶"
                val statusText = if (info.isOnline) "$icon ${stringResource(R.string.network_status_connected)} ${info.description} ${info.strength}" else stringResource(R.string.network_status_disconnected)
                Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                    Text(text = statusText, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            if (isSending) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))

            Box(modifier = Modifier.weight(1f)) {
                SelectionContainer(modifier = Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focusRequester.requestFocus(); keyboardController?.show() }) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                        if (messages.isEmpty()) {
                            item {
                                Column(modifier = Modifier.fillParentMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text(text = stringResource(R.string.welcome_message), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(text = stringResource(R.string.welcome_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        items(messages, key = { it.id }) { msg ->
                            if (!msg.isHidden && !(msg.role == Role.ASSISTANT && (msg.text.contains("📡") || msg.text.contains("📶")))) {
                                MessageBubble(msg = msg, fontSizeMultiplier = fontSizeMultiplier, onReply = { replyingTo = it })
                            }
                        }
                    }
                }
            }

            ChatInputBar(
                inputText = inputText,
                onInputTextChange = { inputText = it },
                isSending = isSending,
                isOnline = isOnline,
                replyingTo = replyingTo,
                onCancelReply = { replyingTo = null },
                onSend = { onSend(it) },
                onCancelCurrent = { agent.cancelCurrent() },
                onMicClick = { startSpeechToText() },
                onTtsClick = { speakTargetMessage() },
                onLanguageClick = { showLanguageDialog = true },
                isSpeaking = isSpeaking,
                focusRequester = focusRequester
            )
        }
    }
}
