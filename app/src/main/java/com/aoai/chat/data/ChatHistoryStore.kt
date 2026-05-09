package com.aoai.chat.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class StoredChatMessage(
    val role: Role,
    val text: String,
    val ts: Long
)

object ChatHistoryStore {
    private const val PREF_NAME = "aoai_chat_history"
    private const val KEY_MESSAGES = "messages_json"
    private const val MAX_MESSAGES = 200

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(context: Context): List<StoredChatMessage> {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()

        return runCatching { json.decodeFromString<List<StoredChatMessage>>(raw) }
            .getOrDefault(emptyList())
    }

    fun save(context: Context, messages: List<StoredChatMessage>) {
        val trimmed = messages.takeLast(MAX_MESSAGES)
        val encoded = runCatching { json.encodeToString(trimmed) }.getOrNull() ?: return

        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MESSAGES, encoded).apply()
    }

    fun clear(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_MESSAGES).apply()
    }
}