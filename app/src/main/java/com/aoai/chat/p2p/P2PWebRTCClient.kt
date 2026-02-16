package com.aoai.chat.p2p

import android.content.Context
import kotlinx.coroutines.*
import org.webrtc.*
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class P2PWebRTCClient(
    private val context: Context,
    private val onMessageReceived: (String) -> Unit,
    private val onDataChannelOpen: () -> Unit
) {
    private var factory: PeerConnectionFactory? = null
    private var pc: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pendingJob: Job? = null
    private val closed = AtomicBoolean(false)

    private var eglBase: EglBase? = null

    // ===== Public API =====

    fun initialize() {
        closed.set(false)
        if (factory != null) return

        // ✅ EglBase는 1회만 만들고 재사용
        if (eglBase == null) {
            eglBase = EglBase.create()
        }

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase!!.eglBaseContext,
            /* enableIntelVp8Encoder */ true,
            /* enableH264HighProfile */ true
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase!!.eglBaseContext)

        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun createPeerConnection() {
        val f = factory ?: throw IllegalStateException("PeerConnectionFactory is null. initialize() 먼저 호출하세요.")
        if (pc != null) return

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        pc = f.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState) = Unit
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) = Unit
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) = Unit
            override fun onIceCandidate(candidate: IceCandidate) = Unit
            override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) = Unit
            override fun onAddStream(stream: MediaStream) = Unit
            override fun onRemoveStream(stream: MediaStream) = Unit

            override fun onDataChannel(dc: DataChannel) {
                registerDataChannel(dc)
            }

            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: RtpReceiver, streams: Array<MediaStream>) = Unit
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) = Unit
            override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent) = Unit
        }) ?: throw IllegalStateException("createPeerConnection() failed")
    }

    fun createDataChannel() {
        val p = pc ?: throw IllegalStateException("PeerConnection is null. createPeerConnection() 먼저 호출하세요.")
        if (dataChannel != null) return

        val init = DataChannel.Init()
        val dc = p.createDataChannel("aoai", init)
        registerDataChannel(dc)
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        val p = pc ?: throw IllegalStateException("PeerConnection is null.")
        runCatching {
            p.setRemoteDescription(
                SimpleSdpObserver(
                    onSetFailure = { /* 필요하면 로그 */ }
                ),
                sdp
            )
        }
    }

    fun createOfferFull(
        timeoutMs: Long,
        onPhase: (String) -> Unit,
        onError: (String) -> Unit,
        onSuccess: (SessionDescription) -> Unit
    ) {
        val p = pc ?: run { onError("PeerConnection is null"); return }
        cancelOngoing()

        val constraints = defaultSdpConstraints()

        pendingJob = scope.launch {
            withContext(Dispatchers.Main) { runCatching { onPhase("createOffer()") } }

            val timeoutJob = launch {
                delay(timeoutMs)
                withContext(Dispatchers.Main) { runCatching { onError("timeout ${timeoutMs}ms") } }
            }

            withContext(Dispatchers.Main) {
                runCatching {
                    p.createOffer(
                        SimpleSdpObserver(
                            onCreateSuccess = { desc ->
                                runCatching { onPhase("setLocalDescription()") }

                                p.setLocalDescription(
                                    SimpleSdpObserver(
                                        onSetSuccess = {
                                            timeoutJob.cancel()
                                            runCatching { onSuccess(desc) }
                                        },
                                        onSetFailure = { err ->
                                            timeoutJob.cancel()
                                            runCatching { onError("setLocalDescription failed: $err") }
                                        }
                                    ),
                                    desc
                                )
                            },
                            onCreateFailure = { err ->
                                timeoutJob.cancel()
                                runCatching { onError("createOffer failed: $err") }
                            }
                        ),
                        constraints
                    )
                }.onFailure { e ->
                    timeoutJob.cancel()
                    runCatching { onError("createOffer exception: ${e.message}") }
                }
            }
        }
    }

    fun createAnswerFull(
        timeoutMs: Long,
        onPhase: (String) -> Unit,
        onError: (String) -> Unit,
        onSuccess: (SessionDescription) -> Unit
    ) {
        val p = pc ?: run { onError("PeerConnection is null"); return }
        cancelOngoing()

        val constraints = defaultSdpConstraints()

        pendingJob = scope.launch {
            withContext(Dispatchers.Main) { runCatching { onPhase("createAnswer()") } }

            val timeoutJob = launch {
                delay(timeoutMs)
                withContext(Dispatchers.Main) { runCatching { onError("timeout ${timeoutMs}ms") } }
            }

            withContext(Dispatchers.Main) {
                runCatching {
                    p.createAnswer(
                        SimpleSdpObserver(
                            onCreateSuccess = { desc ->
                                runCatching { onPhase("setLocalDescription()") }

                                p.setLocalDescription(
                                    SimpleSdpObserver(
                                        onSetSuccess = {
                                            timeoutJob.cancel()
                                            runCatching { onSuccess(desc) }
                                        },
                                        onSetFailure = { err ->
                                            timeoutJob.cancel()
                                            runCatching { onError("setLocalDescription failed: $err") }
                                        }
                                    ),
                                    desc
                                )
                            },
                            onCreateFailure = { err ->
                                timeoutJob.cancel()
                                runCatching { onError("createAnswer failed: $err") }
                            }
                        ),
                        constraints
                    )
                }.onFailure { e ->
                    timeoutJob.cancel()
                    runCatching { onError("createAnswer exception: ${e.message}") }
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val dc = dataChannel ?: return
        if (dc.state() != DataChannel.State.OPEN) return
        val buf = DataChannel.Buffer(ByteBuffer.wrap(text.toByteArray(Charsets.UTF_8)), false)
        runCatching { dc.send(buf) }
    }

    fun cancelOngoing() {
        pendingJob?.cancel()
        pendingJob = null
    }

    fun close() {
        if (closed.getAndSet(true)) return
        cancelOngoing()

        runCatching { dataChannel?.unregisterObserver() }
        runCatching { dataChannel?.close() }
        runCatching { pc?.close() }

        dataChannel = null
        pc = null
        factory = null

        runCatching { eglBase?.release() }
        eglBase = null

        scope.cancel()
    }

    // ===== Internal =====

    private fun registerDataChannel(dc: DataChannel) {
        dataChannel = dc
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) = Unit

            override fun onStateChange() {
                if (dc.state() == DataChannel.State.OPEN) {
                    runCatching { onDataChannelOpen() }
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer) {
                val bytes = ByteArray(buffer.data.remaining())
                buffer.data.get(bytes)
                runCatching { onMessageReceived(String(bytes, Charsets.UTF_8)) }
            }
        })
    }

    private fun defaultSdpConstraints(): MediaConstraints {
        return MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
    }
}