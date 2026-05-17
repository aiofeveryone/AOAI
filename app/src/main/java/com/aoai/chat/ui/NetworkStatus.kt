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
import com.aoai.chat.BuildConfig
import com.aoai.chat.core.brain.aoai01.NetworkStateInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

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
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        try {
            cm.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
            // Unused
        }

        awaitClose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
                // Ignore
            }
        }
    }.distinctUntilChanged()

    fun getNetworkState(context: Context): NetworkStateInfo {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return NetworkStateInfo(isOnline = false, description = "연결 없음")
            val caps = cm.getNetworkCapabilities(network) ?: return NetworkStateInfo(isOnline = false, description = "연결 없음")

            val isOnline = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                         caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                         
            val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isMetered = !isWifi 

            val pct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val signal = caps.linkDownstreamBandwidthKbps
                val calculated = (signal / 200).coerceIn(0, 100)
                calculated
            } else {
                if(isOnline) 85 else 0
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
        } catch (_: Exception) {
            NetworkStateInfo(isOnline = false, description = "오류 발생")
        }
    }

    fun repairNetwork(context: Context) {
        try {
            if (BuildConfig.DEBUG) {
                Log.d("NetworkStatus", "Initiating autonomous network repair...")
            }
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

            cm.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (BuildConfig.DEBUG) {
                        Log.d("NetworkStatus", "Network repair successful: ${network.networkHandle}")
                    }
                    cm.bindProcessToNetwork(network)
                }
            })
        } catch (_: Exception) {
            // Unused
        }
    }
}

@Composable
fun rememberNetworkAvailable(): Boolean {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        NetworkStatus.observeNetworkState(context).collect {
            isOnline = it.isOnline
        }
    }

    return isOnline
}
