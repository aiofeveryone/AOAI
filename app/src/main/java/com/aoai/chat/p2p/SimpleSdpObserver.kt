package com.aoai.chat.p2p

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class SimpleSdpObserver(
    private val onCreateSuccess: ((SessionDescription) -> Unit)? = null,
    private val onSetSuccess: (() -> Unit)? = null,
    private val onCreateFailure: ((String) -> Unit)? = null,
    private val onSetFailure: ((String) -> Unit)? = null
) : SdpObserver {

    override fun onCreateSuccess(desc: SessionDescription?) {
        if (desc != null) onCreateSuccess?.invoke(desc)
    }

    override fun onSetSuccess() {
        onSetSuccess?.invoke()
    }

    override fun onCreateFailure(error: String?) {
        onCreateFailure?.invoke(error ?: "unknown create failure")
    }

    override fun onSetFailure(error: String?) {
        onSetFailure?.invoke(error ?: "unknown set failure")
    }
}