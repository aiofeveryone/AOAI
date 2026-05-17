package com.aoai.chat.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.aoai.chat.BuildConfig
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aoai.chat.R
import com.aoai.chat.core.brain.aoai01.AOAI01Agent
import com.aoai.chat.core.brain.aoai01.AOAI01MasterGuardian
import com.aoai.chat.core.brain.aoai01.NetworkStateInfo
import com.aoai.chat.data.ChatMessage
import com.aoai.chat.data.MediaType
import com.aoai.chat.data.MsgState
import com.aoai.chat.data.Role
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    // 추가 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            Toast.makeText(context, context.getString(R.string.permission_granted_toast), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(R.string.permission_denied_toast), Toast.LENGTH_LONG).show()
        }
    }

    // TTS 관련 설정
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
                    override fun onStart(utteranceId: String?) {
                        scope.launch { isSpeaking = true }
                    }
                    override fun onDone(utteranceId: String?) {
                        scope.launch { isSpeaking = false }
                    }
                    override fun onError(utteranceId: String?) {
                        scope.launch { isSpeaking = false }
                    }
                })
            }
        }
        onDispose { 
            tts?.stop()
            tts?.shutdown() 
            isTtsReady = false
        }
    }

    LaunchedEffect(selectedLocale, isTtsReady) { 
        if (isTtsReady) {
            tts?.language = selectedLocale 
        }
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
                    if (BuildConfig.DEBUG) {
                        Log.d("AOAI_TTS", "Language detection failed: ${e.message}")
                    }
                }

                tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "AOAI_TTS")
            }
        } else { Toast.makeText(context, context.getString(R.string.no_content_to_read), Toast.LENGTH_SHORT).show() }
    }

    var isMasterMode by remember { mutableStateOf(AOAI01MasterGuardian.isOverrideActive()) }
    val isOnline = rememberNetworkAvailable()
    var currentNetworkInfo by remember { mutableStateOf<NetworkStateInfo?>(null) }
    LaunchedEffect(Unit) { NetworkStatus.observeNetworkState(context).collectLatest { currentNetworkInfo = it } }

    var fontSizeMultiplier by remember { mutableStateOf(1f) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && capturedUri != null) { agent.sendWithImage(context.getString(R.string.image_captured_label), capturedUri!!) }
    }

    fun createTempFileUri(extension: String): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile("AOAI_${timeStamp}_", extension, storageDir)
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
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

    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) { listState.animateScrollToItem(messages.lastIndex) }
    }

    LaunchedEffect(Unit) { agent.attachContext(context) }

    fun onSend(text: String) {
        var input = text.trim()
        val currentReplyingTo = replyingTo
        
        if (input.isEmpty() && currentReplyingTo == null) return
        if (!isOnline) return
        
        keyboardController?.hide()

        // 특정 언어 대화 요청 감지
        val langKeywords = listOf("영어", "English", "일본어", "Japanese", "중국어", "Chinese", "베트남어", "Vietnamese", "태국어", "Thai", "인도네시아어", "Indonesian", "프랑스어", "French", "독일어", "German", "스페인어", "Spanish")
        val isLangRequest = (input.contains("말해") || input.contains("대화") || input.lowercase().contains("speak") || input.lowercase().contains("talk")) && langKeywords.any { input.contains(it) }

        if (isLangRequest) {
            input = context.getString(R.string.lang_request_system_prompt, input)
        } else if ((input.contains("번역") || input.contains("translate")) && messages.isNotEmpty()) {
            val lastAssistantMsg = messages.lastOrNull { it.role == Role.ASSISTANT && it.state == MsgState.NORMAL }
            if (lastAssistantMsg != null && input.length < 15) {
                input = context.getString(R.string.translate_request_system_prompt, input, lastAssistantMsg.text)
            }
        }

        if (currentReplyingTo != null) {
            val replyText = currentReplyingTo.text
            input = if (input.isEmpty()) { 
                context.getString(R.string.continue_conversation_prompt, replyText)
            } else { 
                context.getString(R.string.reply_to_prompt, replyText, input)
            }
        }

        if (input.contains("권한") && (input.contains("허용") || input.contains("수락"))) {
            if (input.contains("위치")) { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)) }
        }

        if (input.contains("119") && (input.contains("신고") || input.contains("전화") || input.contains("걸어줘"))) {
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:119"))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.call_failed), Toast.LENGTH_SHORT).show()
            }
        }

        inputText = ""
        replyingTo = null
        haptic.performHapticFeedback(HapticFeedbackType.LongPress) // 전송 햅틱
        
        scope.launch { agent.send(input) }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.about_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.about_subtitle), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.about_description))
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        )
    }

    if (showLanguageDialog) {
        val languageRawList = listOf(
            context.getString(R.string.detect_language) to Locale.getDefault(),
            "Abkhazian" to Locale("ab"), "Acehnese" to Locale("ace"), "Acoli" to Locale("ach"),
            "Afar" to Locale("aa"), "Afrikaans" to Locale("af"), "Akan" to Locale("ak"),
            "Albanian" to Locale("sq"), "Alur" to Locale("alz"), "Amharic" to Locale("am"),
            "Arabic" to Locale("ar"), "Armenian" to Locale("ka"), "Assamese" to Locale("as"),
            "Avaric" to Locale("av"), "Awadhi" to Locale("awa"), "Aymara" to Locale("ay"),
            "Azerbaijani" to Locale("az"), "Balinese" to Locale("ban"), "Baluchi" to Locale("bal"),
            "Bambara" to Locale("bm"), "Bangla" to Locale("bn"), "Baoulé" to Locale("bci"),
            "Bashkir" to Locale("ba"), "Basque" to Locale("eu"), "Batak Karo" to Locale("btx"),
            "Batak Simalungun" to Locale("bts"), "Batak Toba" to Locale("bbc"), "Belarusian" to Locale("be"),
            "Bemba" to Locale("bem"), "Betawi" to Locale("bew"), "Bhojpuri" to Locale("bho"),
            "Bikol" to Locale("bik"), "Bosnian" to Locale("bs"), "Breton" to Locale("br"),
            "Bulgarian" to Locale("bg"), "Buriat" to Locale("bua"), "Burmese" to Locale("my"),
            "Cantonese" to Locale("zh", "HK"), "Catalan" to Locale("ca"), "Cebuano" to Locale("ceb"),
            "Central Kurdish" to Locale("ckb"), "Chamorro" to Locale("ch"), "Chechen" to Locale("ce"),
            "Chiga" to Locale("cgg"), "Chinese (Simplified)" to Locale.SIMPLIFIED_CHINESE,
            "Chinese (Traditional)" to Locale.TRADITIONAL_CHINESE, "Chuukese" to Locale("chk"),
            "Chuvash" to Locale("cv"), "Corsican" to Locale("co"), "Crimean Tatar" to Locale("crh"),
            "Croatian" to Locale("hr"), "Czech" to Locale("cs"), "Danish" to Locale("da"),
            "Dari" to Locale("prs"), "Dinka" to Locale("din"), "Divehi" to Locale("dv"),
            "Dogri" to Locale("doi"), "Dombe" to Locale("dom"), "Dutch" to Locale("nl"),
            "Dyula" to Locale("dyu"), "Dzongkha" to Locale("dz"), "English" to Locale.ENGLISH,
            "Esperanto" to Locale("eo"), "Estonian" to Locale("et"), "Ewe" to Locale("ee"),
            "Faroese" to Locale("fo"), "Fijian" to Locale("fj"), "Filipino" to Locale("fil"),
            "Finnish" to Locale("fi"), "Fon" to Locale("fon"), "French" to Locale.FRENCH,
            "Friulian" to Locale("fur"), "Fulani" to Locale("ff"), "Ga" to Locale("gaa"),
            "Galician" to Locale("gl"), "Ganda" to Locale("lg"), "Georgian" to Locale("ka"),
            "German" to Locale.GERMAN, "Greek" to Locale("el"), "Guarani" to Locale("gn"),
            "Gujarati" to Locale("gu"), "Haitian Creole" to Locale("ht"), "Hakha Chin" to Locale("cnm"),
            "Hausa" to Locale("ha"), "Hawaiian" to Locale("haw"), "Hebrew" to Locale("he"),
            "Hiligaynon" to Locale("hil"), "Hindi" to Locale("hi"), "Hmong" to Locale("hmn"),
            "Hungarian" to Locale("hu"), "Hunsrik" to Locale("hrx"), "Iban" to Locale("iba"),
            "Icelandic" to Locale("is"), "Igbo" to Locale("ig"), "Iloko" to Locale("ilo"),
            "Indonesian" to Locale("id"), "Irish" to Locale("ga"), "Italian" to Locale.ITALIAN,
            "Jamaican Patois" to Locale("jam"), "Japanese" to Locale.JAPANESE, "Javanese" to Locale("jv"),
            "Jingpo" to Locale("kac"), "Kalaallisut" to Locale("kl"), "Kannada" to Locale("kn"),
            "Kanuri" to Locale("kr"), "Kazakh" to Locale("kk"), "Khasi" to Locale("kha"),
            "Khmer" to Locale("km"), "Kinyarwanda" to Locale("rw"), "Kituba" to Locale("ktu"),
            "Kokborok" to Locale("trp"), "Komi" to Locale("kv"), "Kongo" to Locale("kg"),
            "Konkani" to Locale("kok"), "Korean" to Locale.KOREAN, "Krio" to Locale("kri"),
            "Kurdish" to Locale("ku"), "Kyrgyz" to Locale("ky"), "Lao" to Locale("lo"),
            "Latgalian" to Locale("ltg"), "Latin" to Locale("la"), "Latvian" to Locale("lv"),
            "Ligurian" to Locale("lij"), "Limburgish" to Locale("li"), "Lingala" to Locale("ln"),
            "Lithuanian" to Locale("lt"), "Lombard" to Locale("lmo"), "Luo" to Locale("luo"),
            "Luxembourgish" to Locale("lb"), "Macedonian" to Locale("mk"), "Madurese" to Locale("mad"),
            "Maithili" to Locale("mai"), "Makasar" to Locale("mak"), "Malagasy" to Locale("mg"),
            "Malay" to Locale("ms"), "Malay (Arabic)" to Locale("ms", "Arab"), "Malayalam" to Locale("ml"),
            "Maltese" to Locale("mt"), "Mam" to Locale("mam"), "Manipuri (Meitei Mayek)" to Locale("mni"),
            "Manx" to Locale("gv"), "Māori" to Locale("mi"), "Marathi" to Locale("mr"),
            "Marshallese" to Locale("mh"), "Marwari" to Locale("mwr"), "Meadow Mari" to Locale("mhr"),
            "Minangkabau" to Locale("min"), "Mizo" to Locale("lus"), "Mongolian" to Locale("mn"),
            "Morisyen" to Locale("mfe"), "Nahuatl (Eastern Huasteca)" to Locale("nhe"), "Ndau" to Locale("ndc"),
            "Nepalbhasa (Newari)" to Locale("new"), "Nepali" to Locale("ne"), "NKo" to Locale("nqo"),
            "Northern Sami" to Locale("se"), "Northern Sotho" to Locale("nso"), "Norwegian" to Locale("no"),
            "Nuer" to Locale("nus"), "Nyanja" to Locale("ny"), "Occitan" to Locale("oc"),
            "Odia" to Locale("or"), "Oromo" to Locale("om"), "Ossetic" to Locale("os"),
            "Pampanga" to Locale("pam"), "Pangasinan" to Locale("pag"), "Papiamento" to Locale("pap"),
            "Pashto" to Locale("ps"), "Persian" to Locale("fa"), "Polish" to Locale("pl"),
            "Portuguese" to Locale("pt"), "Portuguese (Portugal)" to Locale("pt", "PT"),
            "Punjabi" to Locale("pa"), "Punjabi (Arabic)" to Locale("pa", "Arab"), "Q'eqchi'" to Locale("kek"),
            "Quechua" to Locale("qu"), "Romanian" to Locale("ro"), "Romany" to Locale("rom"),
            "Rundi" to Locale("rn"), "Russian" to Locale("ru"), "Samoan" to Locale("sm"),
            "Sango" to Locale("sg"), "Sanskrit" to Locale("sa"), "Santali (Latin)" to Locale("sat"),
            "Scottish Gaelic" to Locale("gd"), "Serbian" to Locale("sr"), "Seselwa Creole French" to Locale("crs"),
            "Shan" to Locale("shn"), "Shona" to Locale("sn"), "Sicilian" to Locale("scn"),
            "Silesian" to Locale("szl"), "Sindhi" to Locale("sd"), "Sinhala" to Locale("si"),
            "Slovak" to Locale("sk"), "Slovenian" to Locale("sl"), "Somali" to Locale("so"),
            "South Ndebele" to Locale("nr"), "Southern Sotho" to Locale("st"), "Spanish" to Locale("es"),
            "Sundanese" to Locale("su"), "Susu" to Locale("sus"), "Swahili" to Locale("sw"),
            "Swati" to Locale("ss"), "Swedish" to Locale("sv"), "Tahitian" to Locale("ty"),
            "Tajik" to Locale("tg"), "Tamazight" to Locale("ber"), "Tamazight (Tifinagh)" to Locale("ber", "Tfng"),
            "Tamil" to Locale("ta"), "Tatar" to Locale("tt"), "Telugu" to Locale("te"),
            "Tetum" to Locale("tet"), "Thai" to Locale("th"), "Tibetan" to Locale("bo"),
            "Tigrinya" to Locale("ti"), "Tiv" to Locale("tiv"), "Tok Pisin" to Locale("tpi"),
            "Tongan" to Locale("to"), "Tsonga" to Locale("ts"), "Tswana" to Locale("tn"),
            "Tulu" to Locale("tcy"), "Tumbuka" to Locale("tum"), "Turkish" to Locale("tr"),
            "Turkmen" to Locale("tk"), "Tuvinian" to Locale("tyv"), "Udmurt" to Locale("udm"),
            "Ukrainian" to Locale("uk"), "Urdu" to Locale("ur"), "Uyghur" to Locale("ug"),
            "Uzbek" to Locale("uz"), "Venda" to Locale("ve"), "Venetian" to Locale("vec"),
            "Vietnamese" to Locale("vi"), "Waray" to Locale("war"), "Welsh" to Locale("cy"),
            "Western Frisian" to Locale("fy"), "Wolof" to Locale("wo"), "Xhosa" to Locale("xh"),
            "Yakut" to Locale("sah"), "Yiddish" to Locale("yi"), "Yoruba" to Locale("yo"),
            "Yucatec Maya" to Locale("yua"), "Zapotec" to Locale("zap"), "Zulu" to Locale("zu")
        )

        val languageOptions = languageRawList.map { (name, locale) ->
            if (name == context.getString(R.string.detect_language)) name to locale
            else "$name(${locale.getDisplayLanguage(Locale.KOREAN)})" to locale
        }

        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language_settings)) },
            text = {
                Box(modifier = Modifier.heightIn(max = 450.dp)) {
                    LazyColumn {
                        items(languageOptions) { (displayName, locale) ->
                            ListItem(
                                headlineContent = { Text(displayName) },
                                trailingContent = { if (selectedLocale.language == locale.language) { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) } },
                                modifier = Modifier.clickable { selectedLocale = locale; showLanguageDialog = false }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showHistorySheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Text(text = stringResource(R.string.recent_conversations), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    items(uiState.messages.filter { it.role == Role.USER && !it.isHidden }.reversed()) { msg ->
                        ListItem(
                            headlineContent = { Text(text = msg.text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.clickable {
                                val index = messages.indexOfFirst { it.id == msg.id }
                                if (index != -1) { scope.launch { listState.animateScrollToItem(index) } }
                                showHistorySheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_body)) },
            confirmButton = { TextButton(onClick = { showClearDialog = false; agent.clearChat() }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // 인셋 수동 제어
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(), // 상단바 영역 확보
                navigationIcon = { IconButton(onClick = { showHistorySheet = true }) { Icon(Icons.Filled.History, null) } },
                title = {
                    val summary = messages.lastOrNull { it.role == Role.USER && !it.isHidden }?.text ?: stringResource(R.string.new_conversation)
                    Column(modifier = Modifier.clickable { showHistorySheet = true }) {
                        Text(summary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (isMasterMode) stringResource(R.string.master_mode_label) else stringResource(R.string.view_previous_conversations), style = MaterialTheme.typography.labelSmall, color = if (isMasterMode) Color.Red else MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 4.dp)) {
                        IconButton(onClick = { showCommunityMenu = true }) {
                            Icon(Icons.Default.Groups, contentDescription = "Community")
                        }
                        DropdownMenu(
                            expanded = showCommunityMenu,
                            onDismissRequest = { showCommunityMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_about)) },
                                leadingIcon = { Icon(Icons.Default.Info, null) },
                                onClick = {
                                    showCommunityMenu = false
                                    showAboutDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_contribute_github)) },
                                leadingIcon = { Icon(Icons.Default.Code, null) },
                                onClick = {
                                    showCommunityMenu = false
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.github_url))))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.community_discord)) },
                                leadingIcon = { Icon(Icons.Default.Chat, null) },
                                onClick = {
                                    showCommunityMenu = false
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.discord_url))))
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_report_bug)) },
                                leadingIcon = { Icon(Icons.Default.BugReport, null) },
                                onClick = {
                                    showCommunityMenu = false
                                    // Github Issues link or similar
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.github_url) + "/issues")))
                                }
                            )
                        }
                    }
                    Box(modifier = Modifier.padding(end = 8.dp).size(44.dp).clip(RoundedCornerShape(8.dp)).clickable { 
                        agent.clearChatUIOnly() 
                        Toast.makeText(context, context.getString(R.string.chat_cleared_toast), Toast.LENGTH_SHORT).show()
                    }, contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.mipmap.ic_launcher_foreground), contentDescription = "AOAI Logo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
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
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ -> fontSizeMultiplier = (fontSizeMultiplier * zoom).coerceIn(0.8f, 2.5f) }
                }
        ) {
            currentNetworkInfo?.let { info ->
                val icon = if (info.isWifi) "📡" else "📶"
                val statusText = if (info.isOnline) { 
                    "$icon " + stringResource(R.string.network_status_connected) + " ${info.description} ${info.strength}" 
                } else { 
                    stringResource(R.string.network_status_disconnected) 
                }
                Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                    Text(text = statusText, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            if (isSending) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) }

            Box(modifier = Modifier.weight(1f)) {
                SelectionContainer(modifier = Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                    if (messages.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.welcome_message),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.welcome_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    items(messages, key = { it.id }) { msg ->
                        if (msg.isHidden) return@items
                        val isStatusMsg = msg.role == Role.ASSISTANT && (msg.text.contains("📡") || msg.text.contains("📶"))
                        if (isStatusMsg) return@items

                        val align = if (msg.role == Role.USER) Alignment.CenterEnd else Alignment.CenterStart
                        val bubbleColor = if (msg.role == Role.USER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        val textColor = if (msg.role == Role.USER) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        val offsetX = remember(msg.id) { Animatable(0f) }

                        Box(modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }) {
                            Surface(
                                modifier = Modifier
                                    .align(align)
                                    .padding(vertical = 4.dp)
                                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                                    .pointerInput(msg.id) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (offsetX.value > 80f && msg.text != "…") { replyingTo = msg; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                                scope.launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                            },
                                            onHorizontalDrag = { change, dragAmount -> change.consume(); scope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceIn(0f, 150f)) } }
                                        )
                                    }
                                    .combinedClickable(
                                        onClick = { 
                                            focusRequester.requestFocus()
                                            keyboardController?.show()
                                        }, 
                                        onLongClick = { 
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("AOAI Message", msg.text)
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
                                    if (msg.mediaUri != null) { AsyncImage(model = msg.mediaUri, contentDescription = null, modifier = Modifier.sizeIn(maxWidth = 200.dp, maxHeight = 200.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit) }
                                    
                                    if (msg.state == MsgState.LOADING && msg.text == "…") {
                                        // ... (dots animation)
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
                                    } else {
                                        Text(text = msg.text, color = textColor, fontSize = (16 * fontSizeMultiplier).sp, lineHeight = (22 * fontSizeMultiplier).sp)
                                        
                                        // 시간 표시 추가
                                        val timeText = remember(msg.timestamp) {
                                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
                                        }
                                        Text(
                                            text = timeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textColor.copy(alpha = 0.6f),
                                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { startSpeechToText() }) { Icon(Icons.Filled.Mic, null) }
                IconButton(onClick = { speakTargetMessage() }) {
                    Icon(imageVector = if (isSpeaking) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, contentDescription = stringResource(R.string.tts_read_aloud), tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showLanguageDialog = true }) { Icon(Icons.Filled.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) }
            }

            if (replyingTo != null) {
                Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = replyingTo?.text ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp)) }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = inputText, 
                    onValueChange = { 
                        // 삼성 키보드 등에서 숫자 뒤 스페이스바 연타 시 마침표가 자동 생성되는 현상 방지
                        val filteredText = if (it.endsWith(". ") && inputText.endsWith(" ")) {
                            it.dropLast(2) + "  " 
                        } else {
                            it
                        }
                        inputText = filteredText
                    },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.chat_placeholder)) },
                    maxLines = 5, // 최대 5행까지 늘어남
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = { inputText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear text", modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    enabled = isOnline
                )
                Spacer(Modifier.width(8.dp))
                
                if (isSending && inputText.isEmpty()) {
                    IconButton(
                        onClick = { agent.cancelCurrent() }, 
                        modifier = Modifier.size(48.dp), 
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { 
                        Icon(Icons.Filled.Stop, null, tint = MaterialTheme.colorScheme.onError) 
                    }
                } else {
                    if (isOnline && (inputText.isNotEmpty() || replyingTo != null)) {
                        IconButton(
                            onClick = { onSend(inputText) },
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = if (isSending) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.Send, null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }
}
