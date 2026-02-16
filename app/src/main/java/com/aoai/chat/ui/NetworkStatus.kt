package com.aoai.chat.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberNetworkAvailable(): Boolean {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(checkOnline(context)) }

    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
            }

            override fun onLost(network: Network) {
                // 잃은 네트워크가 기본 경로였는지까지 따지면 더 정확하지만,
                // MVP에서는 "현재 온라인인지 재확인"으로 충분
                isOnline = checkOnline(context)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                isOnline = checkOnline(context)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        runCatching { cm.registerNetworkCallback(request, callback) }

        onDispose {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }

    return isOnline
}

private fun checkOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false

    val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    // VALIDATED는 기기/환경에 따라 false일 수 있어 MVP에선 hasInternet 위주로 판단
    return hasInternet || validated
}