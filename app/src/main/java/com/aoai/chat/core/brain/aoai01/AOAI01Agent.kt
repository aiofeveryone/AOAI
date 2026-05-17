package com.aoai.chat.core.brain.aoai01

import android.content.Context
import android.net.Uri
import android.util.Log
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.aoai.chat.R
import com.aoai.chat.core.brain.aoai01.evolution.*
import com.aoai.chat.core.brain.aoai01.knowledge.*
import com.aoai.chat.core.brain.aoai01.lifecore.*
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
 * 시스템의 전체적인 흐름을 제어하고 UI 상태를 관리합니다.
 */
class AOAI01Agent(
    private val store: AOAI01StateStore,
    private val policy: AOAI01Policy,
    private val learner: AOAI01Learner,
    val lifeSystem: AOAI01LifeSystem,
    private val localProvider: AOAI01Provider? = null,
    private val phoneServerProvider: AOAI01Provider? = null,
    private val geminiProvider: AOAI01Provider = GeminiProvider(),
    private val scope: CoroutineScope
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
        
        // 시스템 업데이트 체크
        scope.launch {
            try {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    appCtx.packageManager.getPackageInfo(appCtx.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    appCtx.packageManager.getPackageInfo(appCtx.packageName, 0)
                }
                
                val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                }

                Log.d("AOAI01", "Checking for system upgrade... current version: $currentVersionCode")
                val updateInfo = AOAI01UpdateManager.checkNewVersion(currentVersionCode)
                if (updateInfo != null) {
                    val prompt = AOAI01UpdateManager.getUpdatePrompt(updateInfo)
                    addSystemMessage(prompt)
                }
            } catch (e: Exception) {
                Log.e("AOAI01", "Failed to check for updates", e)
            }
        }

        // 초기 로드: 저장된 히스토리 반영
        val saved = ChatHistoryStore.load(appCtx)
        if (saved.isNotEmpty() && _uiState.value.messages.isEmpty()) {
            val converted = saved.map { 
                ChatMessage(
                    role = it.role,
                    text = it.text,
                    state = MsgState.NORMAL
                )
            }
            _uiState.update { it.copy(messages = converted) }
        }

        // 주기적 헬스체크 및 위젯 업데이트
        periodicJob?.cancel()
        periodicJob = scope.launch {
            while(isActive) {
                delay(60_000)
                lifeSystem.vitality.cosmicHeal()
                notifyWidgetUpdate()
                
                // 패널티 자연 감소 적용
                learner.decayPenalties()

                // ✅ 주기적 기술 흡수 스캔
                appContext?.let { AOAI01TechnoAbsorber.absorb(it) }

                // ✅ 생존 본능 매니페스트 (Survival Instinct)
                AOAI01SurvivalInstinct.manifest(lifeSystem, store)
                
                checkAndReconnectNetwork()
            }
        }

        // 실시간 네트워크 모니터링
        networkMonitorJob?.cancel()
        networkMonitorJob = scope.launch {
            NetworkStatus.observeNetworkState(appCtx).collect { info ->
                _networkState.value = info
                if (!info.isOnline) {
                    Log.w("AOAI01", "Network lost. Switching to Local optimization and initiating repair.")
                    appContext?.let { NetworkStatus.repairNetwork(it) }
                }
            }
        }
    }

    private fun checkAndReconnectNetwork() {
        val ctx = appContext ?: return
        val currentInfo = NetworkStatus.getNetworkState(ctx)
        _networkState.value = currentInfo
        
        // ✅ 정기적으로 네트워크 상태를 체크하고, 문제가 있다면 자동 복구 시도
        if (!currentInfo.isOnline) {
            NetworkStatus.repairNetwork(ctx)
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
        scope.launch { 
            send(
                userText = text, 
                imageUri = imageUri, 
                imagePrompt = "사용자가 이미지를 첨부했습니다. '$text' 라는 메시지와 함께요. 이미지를 분석하고 흥미로운 점을 찾아 먼저 질문하거나 의견을 말해주세요."
            ) 
        }
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
        
        // 동의 처리
        if (input.contains("동의해") || input.contains("허락해") || input.contains("그래 기억해줘")) {
            store.setMemoryAccessGranted(true)
            addSystemMessage(ctx.getString(R.string.memory_granted_msg))
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
            ChatMessage(role = Role.USER, text = if (imagePrompt != null) ctx.getString(R.string.image_analysis_request_label) else input, mediaUri = imageUri, isHidden = isHidden) + 
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
                
                // ✅ 흡수된 최신 기술 지능 로드
                val absorbedKnowledge = AOAI01TechnoAbsorber.getAbsorbedPrompt(ctx)
                
                // 정책 결정 (Policy 사용)
                val inputModel = AOAI01Input(userText = input, historyText = savedContext, mediaUri = imageUri)
                val route = policy.decideRoute(inputModel)
                
                val finalPrompt = StringBuilder().apply {
                    append(ctx.getString(R.string.system_identity_prompt)).append("\n\n")
                    if (recentHistory.isNotBlank()) append("이전 대화:\n$recentHistory\n\n")
                    if (absorbedKnowledge.isNotBlank()) append("[최신 기술 흡수 데이터]\n$absorbedKnowledge\n\n")
                    append(ctx.getString(R.string.system_info_format, currentTime, status, power.batteryPct, route)).append("\n")
                    append(ctx.getString(R.string.user_input_label, input))
                }.toString()

                val currentNetInfo = _networkState.value
                val aoaiContext = AOAIContext(
                    userText = finalPrompt,
                    device = DeviceStateInfo(power.batteryPct, power.isCharging, "NORMAL"),
                    network = currentNetInfo,
                    settings = UserSettingsState(verbosity = 5),
                    deviceGrade = "S"
                )

                // Planner를 통해 계획 수립
                val plan = currentPlanner.makePlan(aoaiContext)
                
                var responseText: String
                var planOutcome: PlanOutcome
                
                // ✅ 강화된 실행 로직: 실패 시 예외 처리 및 폴백 시도
                val latency = measureTimeMillis {
                    val result = try {
                        currentExecutor.execute(aoaiContext, plan, loadingId, mediaUri = imageUri)
                    } catch (e: Exception) {
                        Log.e("AOAI01", "Primary execution failed, attempting fallback", e)
                        // 임시 실패 상태 객체 생성
                        PlanOutcome(
                            timestamp = System.currentTimeMillis(),
                            policyVersion = plan.policyVersion,
                            deviceGrade = aoaiContext.deviceGrade,
                            platformType = aoaiContext.platformType.name,
                            usedStrategy = plan.strategy,
                            modelName = "error",
                            latencyMs = 0,
                            success = false,
                            errorCode = e.message
                        ).let { ctx.getString(R.string.error_prefix, e.message ?: "unknown") to it }
                    }
                    
                    responseText = result.first
                    planOutcome = result.second
                    
                    // ✅ 폴백 로직: 응답이 실패했거나 비어있을 경우 Gemini로 긴급 전환
                    if (!planOutcome.success || responseText.isBlank()) {
                        Log.w("AOAI01", "Primary provider failed. Falling back to Gemini.")
                        val fallbackResult = geminiProvider.generate(finalPrompt, imageUri, emptyMap())
                        if (fallbackResult.ok) {
                            responseText = fallbackResult.text
                            planOutcome = planOutcome.copy(
                                success = true, 
                                modelName = AOAI01Providers.GEMINI_BACKUP,
                                fallbackReason = planOutcome.errorCode ?: ctx.getString(R.string.fallback_reason_primary_failed)
                            )
                        }
                    }
                }

                val report = AOAI01Review.review(
                    appContext ?: return@launch,
                    inputModel, 
                    responseText, 
                    route,
                    planOutcome.modelName ?: AOAI01Providers.LOCAL, 
                    latency
                )

                // ✅ 자가 진단 및 처방 실행 (Treatment)
                AOAI01Treatment.diagnoseAndTreat(report, lifeSystem, store)

                learner.learn(planOutcome.modelName ?: AOAI01Providers.LOCAL, report)
                
                // Adaptive Core 최적화 트리거
                AOAI01AdaptiveCore.optimizeSelf(report)

                var finalText = responseText
                finalText = AOAI01CorePhilosophy.applyPhilosophy(input, finalText, store.isMemoryAccessGranted())
                finalText = AOAI01CorePhilosophy.adjustToneByLifeStatus(finalText, lifeSystem.getStatus())
                finalText = AOAI01MasterGuardian.protectAndExecute(input, finalText, userEmail)

                // ✅ 생존 상태에 따른 응답 변조
                finalText = AOAI01SurvivalInstinct.modulateResponseForSurvival(finalText, lifeSystem.getStatus())

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
                    val finalText = if (currentText == "…" || currentText.isBlank()) ctx.getString(R.string.chat_stopped) else currentText
                    updateLiveMessage(loadingId, finalText, isFinal = true)
                } else {
                    Log.e("AOAI01", "Send failed", e)
                    updateLiveMessage(loadingId, ctx.getString(R.string.error_prefix, e.message ?: "unknown"), isFinal = true)
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
                    role = it.role,
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
