package com.aoai.chat.p2p

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.participationDataStore by preferencesDataStore(name = "participation_prefs")

object ParticipationStore {
    private val KEY_ENABLED = booleanPreferencesKey("participation_enabled")

    /** 관찰용 (UI에서 상태 표시할 때 유용) */
    fun enabledFlow(context: Context): Flow<Boolean> {
        return context.participationDataStore.data.map { prefs: Preferences ->
            prefs[KEY_ENABLED] ?: false
        }
    }

    /** 저장 */
    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.participationDataStore.edit { prefs ->
            prefs[KEY_ENABLED] = enabled
        }
    }
}