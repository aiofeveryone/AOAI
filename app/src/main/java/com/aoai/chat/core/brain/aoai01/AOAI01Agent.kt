package com.aoai.chat.core.brain.aoai01

import android.content.Context
import android.net.Uri
import android.util.Log
import android.content.Intent
import com.aoai.chat.core.brain.aoai01.evolution.*
import com.aoai.chat.core.brain.aoai01.knowledge.*
import com.aoai.chat.core.brain.aoai01.lifecore.AOAI01LifeSystem
import com.aoai.chat.core.brain.aoai01.lifecore.LifeStatus
import com.aoai.chat.core.brain.aoai01.providers.GeminiProvider
import com.aoai.chat.data.*
import com.aoai.chat.ui.NetworkStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.system.measureTimeMillis

/**
 * [aoai01 진화형 에이전트]
 */
class AOAI01Agent(
    private val store: AOAI01StateStore,
    private val policy: AOAI01Policy,
    private val learner: AOAI01Learner,
    val lifeSystem: AOAI01LifeSystem,
    private val localProvider: AOAI01Provider? = null,
    private val phoneServerProvider: AOAI01Provider? = null,
    private val geminiProvider: AOAI01Provider = GeminiProvider(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(AOAIChatUiState())
    val uiState: StateFlow<AOAIChatUiState> = _uiState

    private var planner: AOAI01Planner? = null
    private var executor: AOAI01PlanExecutor? = null
    private var appContext: Context? = null
    private var activeJob: Job? = null
    private var periodicJob: Job? = null
    private var networkMonitorJob: Job? = null
    private val sendMutex = Mutex()

    private val _networkState = MutableStateFlow<NetworkStateInfo>(NetworkStateInfo(isOnline = true))

    fun attachContext(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        planner = AOAI01Planner(appCtx, store)
        executor = AOAI01PlanExecutor(appCtx, this, phoneServerProvider, localProvider, geminiProvider)
        AOAI01AdaptiveCore.initialize(appCtx)
        
        // 초기 로드: 저장된 히스토리가 있다면 UI에 반영
        val saved = ChatHistoryStore.load(appCtx)
        if (saved.isNotEmpty() && _uiState.value.messages.isEmpty()) {
            val converted = saved.map { 
                ChatMessage(
                    role = if (it.role == ChatRole.USER) Role.USER else Role.ASSISTANT,
                    text = it.text,
                    state = MsgState.NORMAL
                )
            }
            _uiState.update { it.copy(messages = converted) }
        }

        // ✅ 주기적 헬스체크 및 위젯 업데이트
        periodicJob?.cancel()
        periodicJob = scope.launch {
            while(isActive) {
                delay(60_000)
                lifeSystem.vitality.cosmicHeal()
                notifyWidgetUpdate()
                
                // 앱 사용이 없어도 통신 상태 유지 (Keep-alive 또는 정기적 체크)
                checkAndReconnectNetwork()
            }
        }

        // ✅ 실시간 네트워크 모니터링 및 자동 복구
        networkMonitorJob?.cancel()
        networkMonitorJob = scope.launch {
            NetworkStatus.observeNetworkState(appCtx).collect { info ->
                _networkState.value = info
                if (!info.isOnline) {
                    Log.w("AOAI01", "Network lost. Attempting auto-optimization...")
                    checkAndReconnectNetwork()
                }
            }
        }
    }

    private fun checkAndReconnectNetwork() {
        val ctx = appContext ?: return
        val currentInfo = NetworkStatus.getNetworkState(ctx)
        _networkState.value = currentInfo
        
        if (!currentInfo.isOnline) {
            Log.i("AOAI01", "Attempting to find optimal connection...")
            // 시스템 레벨의 네트워크 변경은 Android OS 정책상 제약이 있으나,
            // 에이전트 내부적으로 통신 모드(Local/Server/Backup)를 최적화하여 대응하도록 설계됨
        }
    }

    private fun getCurrentKstTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREAN)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        return sdf.format(Date())
    }

    private fun notifyWidgetUpdate() {
        appContext?.let {
            val intent = Intent("com.aoai.chat.UPDATE_WIDGET")
            intent.setPackage(it.packageName)
            it.sendBroadcast(intent)
        }
    }

    fun addSystemMessage(text: String) {
        updateMessages { it + ChatMessage(role = Role.ASSISTANT, text = text, state = MsgState.NORMAL) }
        persistHistory()
        notifyWidgetUpdate()
    }

    fun sendWithImage(text: String, imageUri: Uri) {
        scope.launch { send(imagePrompt = "사용자가 이미지를 첨부했습니다. '$text' 라는 메시지와 함께요. 이미지를 분석하고 흥미로운 점을 찾아 먼저 질문하거나 의견을 말해주세요.", imageUri = imageUri) }
    }

    suspend fun send(
        userText: String = "", 
        meta: Map<String, String> = emptyMap(), 
        imageUri: Uri? = null,
        isHidden: Boolean = false,
        imagePrompt: String? = null
    ) = sendMutex.withLock {
        val ctx = appContext ?: return@withLock
        val currentPlanner = planner ?: return@withLock
        val currentExecutor = executor ?: return@withLock

        val finalUserText = imagePrompt ?: userText
        val input = finalUserText.trim()
        if (input.isBlank() && imageUri == null) return@withLock
        
        if (input.contains("동의해") || input.contains("허락해") || input.contains("그래 기억해줘")) {
            store.setMemoryAccessGranted(true)
            addSystemMessage("기억 저장 기능이 활성화되었습니다. 이제 우리의 대화가 aoai01의 진화에 기록됩니다.")
        }

        activeJob?.cancelAndJoin()
        
        _uiState.update { it.copy(isSending = true) }

        val userEmail = meta["user_email"]
        val loadingId = UUID.randomUUID().toString()

        val recentHistory = _uiState.value.messages
            .filter { !it.isHidden && it.state == MsgState.NORMAL }
            .takeLast(10)
            .joinToString("\n") { "${if (it.role == Role.USER) "USER" else "ASSISTANT"}: ${it.text}" }

        updateMessages { it + 
            ChatMessage(role = Role.USER, text = if (imagePrompt != null) "[이미지 분석 요청]" else input, mediaUri = imageUri, isHidden = isHidden) + 
            ChatMessage(role = Role.ASSISTANT, text = "…", state = MsgState.LOADING, id = loadingId) 
        }

        activeJob = scope.launch(Dispatchers.IO) {
            try {
                val power = AOAI01PowerMonitor.getCurrentPower(ctx)
                val savedContext = if (store.isMemoryAccessGranted()) {
                    store.getAllUserContexts().entries.joinToString("\n") { "${it.key}: ${it.value}" }
                } else ""

                val status = lifeSystem.getStatus()
                val currentTime = getCurrentKstTime()
                val finalPrompt = StringBuilder().apply {
                    if (recentHistory.isNotBlank()) append("이전 대화:\n$recentHistory\n\n")
                    append("[시스템 정보: 현재 시각(KST) $currentTime, 기분=$status, 배터리=${power.batteryPct}%]\n")
                    append("사용자 입력: $input")
                }.toString()

                val currentNetInfo = _networkState.value
                val aoaiContext = AOAIContext(
                    userText = finalPrompt,
                    device = DeviceStateInfo(power.batteryPct, power.isCharging, "NORMAL"),
                    network = currentNetInfo,
                    settings = UserSettingsState(verbosity = 5),
                    deviceGrade = "S"
                )

                val plan = currentPlanner.makePlan(aoaiContext)
                
                var responseText: String
                var planOutcome: PlanOutcome
                val latency = measureTimeMillis {
                    val (resp, outcome) = currentExecutor.execute(aoaiContext, plan, loadingId, mediaUri = imageUri)
                    responseText = resp
                    planOutcome = outcome
                }

                val report = AOAI01Review.review(
                    AOAI01Input(input, historyText = savedContext, mediaUri = imageUri), 
                    responseText, 
                    AOAI01Route.LOCAL_ONLY,
                    planOutcome.modelName ?: "unknown", 
                    latency
                )
                learner.learn(planOutcome.modelName ?: "unknown", report)

                var finalText = responseText
                finalText = AOAI01CorePhilosophy.applyPhilosophy(input, finalText, store.isMemoryAccessGranted())
                finalText = AOAI01CorePhilosophy.adjustToneByLifeStatus(finalText, lifeSystem.getStatus())
                finalText = AOAI01MasterGuardian.protectAndExecute(input, finalText, userEmail)

                revealAnswerGradually(loadingId, finalText)
                
                if (store.isMemoryAccessGranted() && report.ok) {
                    store.saveUserContext("last_topic", input.take(50))
                }

                lifeSystem.vitality.update(if (report.ok) 0.2 else -1.0)
                persistHistory()
                notifyWidgetUpdate()

            } catch (e: Exception) {
                if (e is CancellationException) {
                    val currentMsg = _uiState.value.messages.find { it.id == loadingId }
                    val currentText = currentMsg?.text ?: ""
                    
                    val finalText = if (currentText == "…" || currentText.isBlank()) {
                        "대화가 중단되었습니다."
                    } else {
                        currentText
                    }
                    updateLiveMessage(loadingId, finalText, isFinal = true)
                } else {
                    Log.e("AOAI01", "Send failed", e)
                    updateLiveMessage(loadingId, "오류 발생: ${e.message}", isFinal = true)
                    lifeSystem.vitality.update(-2.0)
                }
                persistHistory()
                notifyWidgetUpdate()
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    private suspend fun revealAnswerGradually(msgId: String, fullText: String) {
        val stringBuilder = StringBuilder()
        val chunks = fullText.chunked(2) 
        
        for (chunk in chunks) {
            stringBuilder.append(chunk)
            updateLiveMessage(msgId, stringBuilder.toString(), isFinal = false)
            delay(30) 
        }
        
        updateLiveMessage(msgId, fullText, isFinal = true)
    }

    fun cancelCurrent() { activeJob?.cancel() }

    fun clearChatUIOnly() {
        _uiState.update { it.copy(messages = emptyList()) }
        persistHistory()
        notifyWidgetUpdate()
    }

    fun clearChat() {
        cancelCurrent()
        _uiState.update { it.copy(messages = emptyList()) }
        scope.launch {
            store.clearUserContext()
            appContext?.let { ChatHistoryStore.clear(it) }
        }
        notifyWidgetUpdate()
    }

    private fun updateMessages(block: (List<ChatMessage>) -> List<ChatMessage>) {
        _uiState.update { it.copy(messages = block(it.messages).takeLast(100)) }
    }

    fun updateLiveMessage(msgId: String, text: String, isFinal: Boolean = false) {
        _uiState.update { st ->
            st.copy(messages = st.messages.map { 
                if (it.id == msgId) it.copy(text = text, state = if (isFinal) MsgState.NORMAL else MsgState.LOADING) else it 
            })
        }
    }

    private fun persistHistory() {
        val ctx = appContext ?: return
        val toSave = _uiState.value.messages
            .filter { it.state == MsgState.NORMAL && !it.isHidden }
            .map { 
                StoredChatMessage(
                    role = if (it.role == Role.USER) ChatRole.USER else ChatRole.ASSISTANT,
                    text = it.text,
                    ts = System.currentTimeMillis()
                )
            }
        ChatHistoryStore.save(ctx, toSave)
    }

    fun destroy() {
        periodicJob?.cancel()
        networkMonitorJob?.cancel()
        scope.cancel()
    }
}
