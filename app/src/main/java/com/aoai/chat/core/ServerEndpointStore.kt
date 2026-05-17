package com.aoai.chat.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.endpointDataStore by preferencesDataStore(name = "server_endpoint_prefs")

class ServerEndpointStore(private val context: Context) {

    private object Keys {
        val MODE = stringPreferencesKey("mode")          // LOCAL | LAN | OVERLAY
        val CUSTOM_URL = stringPreferencesKey("custom")  // LAN/OVERLAY baseUrl
    }

    val endpointFlow: Flow<ServerEndpoint> =
        context.endpointDataStore.data.map { prefs ->
            val modeStr = prefs[Keys.MODE] ?: ServerMode.LOCAL.name
            val mode = runCatching { ServerMode.valueOf(modeStr) }.getOrDefault(ServerMode.LOCAL)
            val custom = prefs[Keys.CUSTOM_URL] ?: ""
            ServerEndpoint(mode = mode, customBaseUrl = custom)
        }

    suspend fun setMode(mode: ServerMode) {
        context.endpointDataStore.edit { prefs ->
            prefs[Keys.MODE] = mode.name
        }
    }

    suspend fun setCustomBaseUrl(url: String) {
        context.endpointDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_URL] = url
        }
    }

    suspend fun set(endpoint: ServerEndpoint) {
        context.endpointDataStore.edit { prefs ->
            prefs[Keys.MODE] = endpoint.mode.name
            prefs[Keys.CUSTOM_URL] = endpoint.customBaseUrl
        }
    }
}