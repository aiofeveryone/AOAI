package com.aoai.chat.core

import android.content.Context

object AOAISettings {

    private const val PREF_NAME = "aoai_settings"
    private const val KEY_NODE_MODE = "node_mode"

    fun getNodeMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NODE_MODE, "UNDECIDED") ?: "UNDECIDED"
    }

    fun setParticipating(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NODE_MODE, "PARTICIPATING").apply()
    }
}
