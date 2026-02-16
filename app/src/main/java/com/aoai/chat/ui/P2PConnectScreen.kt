package com.aoai.chat.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aoai.chat.core.AOAISignal
import com.aoai.chat.core.ServerEndpoint
import com.aoai.chat.core.ServerEndpointStore
import com.aoai.chat.core.ServerMode
import com.aoai.chat.p2p.AOAISignalSerializer
import com.aoai.chat.p2p.P2PConnectionManager
import com.aoai.chat.p2p.P2PSignalRelay
import com.aoai.chat.p2p.P2PWebRTCClient
import com.aoai.chat.p2p.QRCodeGenerator
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.SessionDescription
import java.util.UUID

private sealed class P2PState {
    data object Idle : P2PState()
    data object OfferCreated : P2PState()
    data object OfferReceived : P2PState()
    data object AnswerCreated : P2PState()
    data object Connecting : P2PState()
    data object ConnectedMaybe : P2PState()
    data class Error(val message: String) : P2PState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun P2PConnectScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<P2PState>(P2PState.Idle) }
    var status by remember { mutableStateOf("대기중") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isBusy by remember { mutableStateOf(false) }
    var phaseText by remember { mutableStateOf("") }

    // ✅ A가 세션 생성
    var sessionId by remember { mutableStateOf<String?>(null) }

    // ===== 서버 주소 모드 3개(Local/LAN/Overlay) =====
    val endpointStore = remember { ServerEndpointStore(context) }
    val endpoint by endpointStore.endpointFlow.collectAsStateWithLifecycle(
        initialValue = ServerEndpoint(mode = ServerMode.LOCAL)
    )

    // resolvedBaseUrl이 nullable로 바뀌어도 안전하게
    val resolvedBaseUrl: String = remember(endpoint) {
        endpoint.resolvedBaseUrl(defaultPort = 8080).orEmpty()
    }

    // ✅ baseUrl이 바뀌면 relay도 자동 교체
    val relay = remember(resolvedBaseUrl) { P2PSignalRelay(baseUrl = resolvedBaseUrl) }

    val clientRef = remember { mutableStateOf<P2PWebRTCClient?>(null) }

    val client = remember {
        P2PWebRTCClient(
            context = context,
            onMessageReceived = { msg -> println("DC recv: $msg") },
            onDataChannelOpen = {
                runCatching {
                    val hsJson = """{"type":"handshake","ts":${System.currentTimeMillis()}}"""
                    clientRef.value?.sendMessage(hsJson)

                    status = "DataChannel OPEN → Handshake 자동 전송 ✅"
                    state = P2PState.ConnectedMaybe

                    runCatching { P2PConnectionManager.onConnectionEstablished() }
                }
            }
        )
    }.also { created -> clientRef.value = created }

    fun safeCloseClient() {
        runCatching { client.cancelOngoing() }
        runCatching { client.close() }
    }

    fun cleanupAndBack() {
        runCatching { P2PConnectionManager.disconnect() }
        runCatching { safeCloseClient() }
        onBack()
    }

    BackHandler { cleanupAndBack() }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { P2PConnectionManager.disconnect() }
            runCatching { safeCloseClient() }
        }
    }

    fun cancelBusyOperation() {
        runCatching { client.cancelOngoing() }

        isBusy = false
        phaseText = ""
        qrBitmap = null
        sessionId = null
        state = P2PState.Idle
        status = "취소됨"
    }

    // ✅ QR 스캔 결과 처리: 이제는 offer SDP가 아니라 "join(sessionId)"만 스캔
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents.isNullOrBlank()) {
            status = "스캔 취소/실패"
            state = P2PState.Idle
            return@rememberLauncherForActivityResult
        }

        if (isBusy) {
            status = "진행 중인 작업이 있어요. 취소 후 다시 시도해주세요."
            return@rememberLauncherForActivityResult
        }

        runCatching {
            val signal = AOAISignalSerializer.fromJson(contents)

            // ✅ B는 join을 받아서 sid로 offer를 서버에서 가져온다
            if (signal.type != "join") {
                state = P2PState.Error("expected join signal")
                status = "잘못된 QR입니다. (join 타입 필요)"
                return@rememberLauncherForActivityResult
            }

            val sid = signal.sessionId
            if (sid.isNullOrBlank()) {
                state = P2PState.Error("join missing sessionId")
                status = "QR에 sessionId가 없습니다."
                return@rememberLauncherForActivityResult
            }

            // B 플로우 시작
            isBusy = true
            qrBitmap = null
            phaseText = "join 수신됨 (sessionId=$sid)"
            status = "서버에서 Offer 가져오는 중... ($resolvedBaseUrl)"

            scope.launch {
                try {
                    val offerSdp = relay.getOffer(sid)
                    if (offerSdp.isNullOrBlank()) {
                        isBusy = false
                        phaseText = ""
                        state = P2PState.Error("offer not found")
                        status =
                            "서버에 Offer가 없습니다. (A가 Offer 업로드했는지 확인)\n주소: $resolvedBaseUrl"
                        return@launch
                    }

                    status = "Offer 수신 ✅ → Answer 생성중..."
                    phaseText = "WebRTC 초기화 중…"

                    client.initialize()
                    client.createPeerConnection()
                    runCatching { client.createDataChannel() }

                    client.setRemoteDescription(
                        SessionDescription(SessionDescription.Type.OFFER, offerSdp)
                    )

                    client.createAnswerFull(
                        timeoutMs = 25_000,
                        onPhase = { phase ->
                            phaseText = phase
                            status = "Answer 생성중... ($phase)"
                        },
                        onError = { err ->
                            isBusy = false
                            phaseText = ""
                            state = P2PState.Error(err)
                            status = "Answer 생성 실패/지연: $err"
                        }
                    ) { answerSdp ->
                        scope.launch {
                            try {
                                status = "Answer 생성 완료 → 서버로 전송중..."
                                relay.postAnswer(sid, answerSdp.description)

                                isBusy = false
                                phaseText = ""
                                state = P2PState.AnswerCreated
                                status = "Answer 전송 완료 ✅ (A는 자동으로 연결됩니다)"
                            } catch (e: Exception) {
                                isBusy = false
                                phaseText = ""
                                state = P2PState.Error(e.message ?: "relay error")
                                status = "Answer 서버 전송 실패: ${e.message}\n주소: $resolvedBaseUrl"
                            }
                        }
                    }
                } catch (e: Exception) {
                    isBusy = false
                    phaseText = ""
                    state = P2PState.Error(e.message ?: "unknown")
                    status = "B 플로우 실패: ${e.message}\n주소: $resolvedBaseUrl"
                }
            }
        }.onFailure { e ->
            state = P2PState.Error(e.message ?: "unknown")
            status = "스캔 데이터 파싱 실패: ${e.message}"
        }
    }

    // ✅ A 역할: Offer 생성 후 Answer를 서버에서 자동 폴링
    LaunchedEffect(sessionId, state, resolvedBaseUrl) {
        val sid = sessionId ?: return@LaunchedEffect
        if (state !is P2PState.OfferCreated) return@LaunchedEffect

        val maxMs = 60_000L
        val intervalMs = 1_000L
        var waited = 0L

        status = "Answer 대기중… (서버 확인 중)\n주소: $resolvedBaseUrl"
        phaseText = "상대가 QR 스캔 후 Answer를 서버로 전송하면 자동 연결됩니다."

        while (waited < maxMs) {
            delay(intervalMs)
            waited += intervalMs

            val answerSdp = runCatching { relay.getAnswer(sid) }.getOrNull()
            if (!answerSdp.isNullOrBlank()) {
                status = "Answer 수신 ✅ → RemoteDescription 설정중..."
                runCatching {
                    client.setRemoteDescription(
                        SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
                    )
                }.onFailure { e ->
                    state = P2PState.Error(e.message ?: "setRemoteDescription failed")
                    isBusy = false
                    phaseText = ""
                    status = "Answer 적용 실패: ${e.message}\n주소: $resolvedBaseUrl"
                    return@LaunchedEffect
                }

                state = P2PState.Connecting
                isBusy = false
                phaseText = "연결 진행중… (DataChannel OPEN 시 자동 handshake)"
                status = "연결 진행중…"
                return@LaunchedEffect
            } else {
                status = "Answer 대기중… (${waited / 1000}초)\n주소: $resolvedBaseUrl"
            }
        }

        state = P2PState.Error("answer wait timeout")
        isBusy = false
        phaseText = ""
        status =
            "Answer 대기 타임아웃(60초). 상대가 Answer 전송했는지 확인/재시도.\n주소: $resolvedBaseUrl"
    }

    val elapsedSec = rememberElapsedSeconds(isBusy)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("P2P 연결 (QR + Relay)") },
                navigationIcon = {
                    TextButton(onClick = { cleanupAndBack() }) { Text("← Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = status,
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(12.dp))

            // ✅ 서버 주소 모드 패널
            // IMPORTANT: 여기서는 ServerEndpointPanel.kt에 있는 public composable을 사용합니다.
            ServerEndpointPanel(
                endpoint = endpoint,
                resolvedBaseUrl = resolvedBaseUrl,
                onModeChange = { mode: ServerMode ->
                    scope.launch { endpointStore.setMode(mode) }
                },
                onCustomUrlChange = { url: String ->
                    scope.launch { endpointStore.setCustomBaseUrl(url) }
                }
            )

            if (isBusy) {
                Spacer(Modifier.height(12.dp))
                OfferGeneratingPanel(
                    title = "진행 중…",
                    elapsedSec = elapsedSec,
                    phaseText = phaseText.ifBlank { "SDP 생성/ICE 수집 대기 중…" },
                    onCancel = { cancelBusyOperation() }
                )
            } else if (phaseText.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    phaseText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF444444)
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy && resolvedBaseUrl.isNotBlank(),
                onClick = {
                    state = P2PState.Idle
                    qrBitmap = null
                    isBusy = true

                    val sid = UUID.randomUUID().toString().replace("-", "")
                    sessionId = sid

                    phaseText = "초기화 중…"
                    status = "Offer 생성중..."

                    runCatching {
                        client.initialize()
                        client.createPeerConnection()
                        client.createDataChannel()

                        client.createOfferFull(
                            timeoutMs = 25_000,
                            onPhase = { phase ->
                                phaseText = phase
                                status = "Offer 생성중... ($phase)"
                            },
                            onError = { err ->
                                isBusy = false
                                phaseText = ""
                                state = P2PState.Error(err)
                                status = "Offer 생성 실패/지연: $err"
                                sessionId = null
                            }
                        ) { offerSdp ->
                            scope.launch {
                                try {
                                    status = "Offer 생성 완료 → 서버로 업로드 중...\n주소: $resolvedBaseUrl"
                                    relay.postOffer(sid, offerSdp.description)

                                    val joinSignal = AOAISignal(
                                        type = "join",
                                        sdp = "",
                                        fromNodeId = "unknown",
                                        sessionId = sid
                                    )
                                    val joinJson = AOAISignalSerializer.toJson(joinSignal)

                                    val bmp = withContext(Dispatchers.Default) {
                                        QRCodeGenerator.generateOrNull(joinJson, size = 900)
                                    }

                                    if (bmp == null) {
                                        isBusy = false
                                        phaseText = ""
                                        state = P2PState.Error("qr generate failed")
                                        status = "QR 생성 실패 (예외)."
                                        qrBitmap = null
                                        sessionId = null
                                        return@launch
                                    }

                                    qrBitmap = bmp
                                    isBusy = false
                                    phaseText = ""
                                    state = P2PState.OfferCreated
                                    status =
                                        "join QR 생성 완료 ✅ (상대가 스캔하면 자동 연결됩니다)\n주소: $resolvedBaseUrl"
                                } catch (e: Exception) {
                                    isBusy = false
                                    phaseText = ""
                                    state = P2PState.Error(e.message ?: "relay error")
                                    status = "Offer 서버 업로드 실패: ${e.message}\n주소: $resolvedBaseUrl"
                                    sessionId = null
                                }
                            }
                        }
                    }.onFailure { e ->
                        isBusy = false
                        phaseText = ""
                        state = P2PState.Error(e.message ?: "unknown")
                        status = "Offer 시작 실패: ${e.message}"
                        sessionId = null
                    }
                }
            ) {
                Text("A: Offer 생성 (join QR 표시)")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy && resolvedBaseUrl.isNotBlank(),
                onClick = {
                    val options = ScanOptions()
                        .setPrompt("join QR을 스캔하세요")
                        .setBeepEnabled(true)
                        .setOrientationLocked(true)
                    scanLauncher.launch(options)
                }
            ) {
                Text("B: QR 스캔 (join → Offer 다운로드)")
            }

            Spacer(Modifier.height(24.dp))

            qrBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(260.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "STATE: ${state::class.simpleName}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun rememberElapsedSeconds(running: Boolean): Int {
    var sec by remember { mutableIntStateOf(0) }
    LaunchedEffect(running) {
        sec = 0
        if (running) {
            while (true) {
                delay(1000)
                sec++
            }
        }
    }
    return sec
}

@Composable
private fun OfferGeneratingPanel(
    title: String,
    elapsedSec: Int,
    phaseText: String,
    onCancel: () -> Unit,
    hintAfterSec: Int = 10
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))

        Text("$title (${elapsedSec}초)")
        Spacer(Modifier.height(4.dp))
        Text(phaseText, style = MaterialTheme.typography.bodySmall)

        if (elapsedSec >= hintAfterSec) {
            Spacer(Modifier.height(6.dp))
            Text(
                "네트워크 상태에 따라 시간이 걸릴 수 있어요.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onCancel) { Text("취소") }
    }
}