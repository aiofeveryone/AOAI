package com.aoai.chat.p2p

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Wi-Fi only gatekeeper for AOAI participation node.
 *
 * - isWifiValidatedNow(): 현재 "Wi-Fi + 인터넷 가능(검증됨)" 여부
 * - register()/unregister(): NetworkCallback 기반 실시간 변화 감지
 *
 * ✅ Edge-trigger 정책:
 * - ineligible -> eligible "진입" 시에만 onWifiEligible() 1회 호출
 * - eligible -> ineligible "이탈" 시에만 onWifiIneligible() 1회 호출
 * - 상태 유지 중(eligible 유지 / ineligible 유지)에는 무시
 */
object NetworkGatekeeper {

    interface Listener {
        /** Wi-Fi 참여 가능 상태로 "진입" */
        fun onWifiEligible()

        /** Wi-Fi 참여 불가 상태로 "이탈/진입" */
        fun onWifiIneligible()
    }

    private var callback: ConnectivityManager.NetworkCallback? = null

    // ✅ Edge-trigger를 위한 "직전 상태" 기억
    // - 초기값 false(=ineligible)로 두고 register 시 현재 상태를 1회 평가해 동기화
    private val lastEligible = AtomicBoolean(false)

    /**
     * 현재 즉시 상태 체크:
     * - Wi-Fi인지
     * - 실제 인터넷 사용 가능(VALIDATED)인지
     */
    fun isWifiValidatedNow(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return isWifiEligibleByCaps(caps)
    }

    /**
     * 실시간 감시 시작.
     * 이미 등록되어 있으면 중복 등록하지 않음.
     *
     * ✅ register 직후 1회 "현재 상태"로 동기화하되,
     *   Edge-trigger 규칙(진입/이탈)로만 통지한다.
     */
    @Synchronized
    fun register(context: Context, listener: Listener) {
        if (callback != null) return

        val appCtx = context.applicationContext
        val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // ✅ 현재 상태 1회 평가 + (필요 시에만) 엣지 통지
        evaluateAndNotifyEdge(cm, listener)

        val cb = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                // onAvailable만으로는 Wi-Fi인지 보장 안됨 -> capabilities 기반 최종 판단
                evaluateAndNotifyEdge(cm, listener)
            }

            override fun onLost(network: Network) {
                evaluateAndNotifyEdge(cm, listener)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                // Wi-Fi ↔ Cellular 전환, VALIDATED 변동 등을 여기서 잡는 게 핵심
                evaluateAndNotifyEdge(cm, listener)
            }
        }

        callback = cb
        cm.registerDefaultNetworkCallback(cb)
    }

    /**
     * 실시간 감시 중단.
     *
     * ✅ A안(강추 수정 1):
     * unregister 시 lastEligible=false로 reset
     * -> 서비스/참여 모드가 다시 켜질 때, 현재 eligible이면 onWifiEligible() "1회 보장"
     */
    @Synchronized
    fun unregister(context: Context) {
        val cb = callback ?: run {
            // 콜백이 없어도 A안 정책상 리셋은 수행해 두는 게 안전
            lastEligible.set(false)
            return
        }

        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        try {
            cm.unregisterNetworkCallback(cb)
        } catch (_: Exception) {
            // ignore
        } finally {
            callback = null
            lastEligible.set(false) // ✅ 핵심: unregister()에서 리셋 (강추 수정 1)
        }
    }

    /**
     * ✅ Edge-trigger 평가/통지:
     * - 현재 eligible 계산
     * - 직전 상태(prev)와 비교해 false->true / true->false 일 때만 리스너 호출
     */
    private fun evaluateAndNotifyEdge(cm: ConnectivityManager, listener: Listener) {
        val active = cm.activeNetwork
        val caps = if (active != null) cm.getNetworkCapabilities(active) else null
        val eligibleNow = (caps != null && isWifiEligibleByCaps(caps))

        val prev = lastEligible.getAndSet(eligibleNow)

        // false -> true : eligible 진입
        if (!prev && eligibleNow) {
            listener.onWifiEligible()
            return
        }

        // true -> false : eligible 이탈 (ineligible 진입)
        if (prev && !eligibleNow) {
            listener.onWifiIneligible()
            return
        }

        // true->true / false->false : 상태 유지 중이므로 무시
    }

    /**
     * "Wi-Fi only + 인터넷 가능" 기준.
     *
     * - TRANSPORT_WIFI: Wi-Fi 연결
     * - NET_CAPABILITY_INTERNET: 인터넷 기능 보유
     * - NET_CAPABILITY_VALIDATED: 실제 인터넷 통신 가능(캡티브 포털 등 제외)
     *
     * 참고: VALIDATED는 OS가 실제로 인터넷을 확인한 상태라, 연결 직후 잠깐 false일 수 있음.
     */
    private fun isWifiEligibleByCaps(caps: NetworkCapabilities): Boolean {
        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        val validated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            true
        }

        return isWifi && hasInternet && validated
    }
}