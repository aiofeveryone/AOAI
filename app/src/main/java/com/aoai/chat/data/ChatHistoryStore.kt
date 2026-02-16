package com.aoai.chat.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class StoredChatMessage(
    val role: String,   // "USER" | "ASSISTANT"
    val text: String,
    val ts: Long
)

object ChatHistoryStore {

    private const val PREF_NAME = "aoai_chat_history"
    private const val KEY_MESSAGES = "messages_json"
    private const val MAX_MESSAGES = 200

    private val gson = Gson()

    fun load(context: Context): List<StoredChatMessage> {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()

        return runCatching {
            val type = object : TypeToken<List<StoredChatMessage>>() {}.type
            gson.fromJson<List<StoredChatMessage>>(json, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, messages: List<StoredChatMessage>) {
        val trimmed = if (messages.size > MAX_MESSAGES) {
            messages.takeLast(MAX_MESSAGES)
        } else {
            messages
        }

        val json = runCatching { gson.toJson(trimmed) }.getOrNull() ?: return

        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MESSAGES, json).apply()
    }

    fun clear(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_MESSAGES).apply()
    }
}