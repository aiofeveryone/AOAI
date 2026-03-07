package com.aoai.chat.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.aoai.chat.core.brain.aoai01.NetworkStateInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

object NetworkStatus {

    fun observeNetworkState(context: Context): Flow<NetworkStateInfo> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getNetworkState(context))
            }

            override fun onLost(network: Network) {
                trySend(getNetworkState(context))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(getNetworkState(context))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) // ✅ 검증된 네트워크만 보도록 기준 강화
            .build()

        try {
            cm.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.e("NetworkStatus", "Failed to register callback", e)
        }

        awaitClose {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }.onStart {
        emit(getNetworkState(context))
    }.distinctUntilChanged()

    fun getNetworkState(context: Context): NetworkStateInfo {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return NetworkStateInfo(isOnline = false, description = "연결 없음")
            val caps = cm.getNetworkCapabilities(network) ?: return NetworkStateInfo(isOnline = false, description = "연결 없음")

            // ✅ 인터넷 연결 및 검증 여부를 함께 확인하여 판단 기준 완화
            val isOnline = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                         caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                         
            val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isMetered = !isWifi 

            // 수치(Percentage) 계산 로직
            val pct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val signal = caps.linkDownstreamBandwidthKbps
                val calculated = (signal / 200).coerceIn(0, 100)
                calculated
            } else {
                if(isOnline) 85 else 0 // ✅ 온라인 아닐 시 0으로 처리
            }

            val strengthLabel = when {
                pct >= 90 -> "훌륭함"
                pct >= 60 -> "양호"
                pct >= 30 -> "불량"
                else -> "매우 불량"
            }

            NetworkStateInfo(
                isOnline = isOnline,
                isWifi = isWifi,
                isMetered = isMetered,
                strength = if (isOnline) "$pct%" else "",
                description = if (isOnline) strengthLabel else "연결 없음"
            )
        } catch (e: Exception) {
            NetworkStateInfo(isOnline = false, description = "오류 발생")
        }
    }
}

@Composable
fun rememberNetworkAvailable(): Boolean {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(true) } // ✅ 기본값을 true로 변경하여 초기 깜빡임 방지

    LaunchedEffect(Unit) {
        NetworkStatus.observeNetworkState(context).collect {
            isOnline = it.isOnline
        }
    }

    return isOnline
}
