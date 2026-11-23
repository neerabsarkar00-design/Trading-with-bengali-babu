package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "bangali_babu_prefs")

class SessionManager(private val context: Context) {

    private object Keys {
        val LOGGED_IN_USER_ID = longPreferencesKey("logged_in_user_id")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val CUSTOM_LOGO_PATH = stringPreferencesKey("custom_logo_path")
    }

    val loggedInUserIdFlow: Flow<Long?> = context.appDataStore.data.map { prefs ->
        val id = prefs[Keys.LOGGED_IN_USER_ID] ?: -1L
        if (id > 0L) id else null
    }

    val isDarkModeFlow: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[Keys.DARK_MODE] ?: true
    }

    val customLogoPathFlow: Flow<String?> = context.appDataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_LOGO_PATH]?.takeIf { it.isNotBlank() }
    }

    suspend fun saveSession(userId: Long) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.LOGGED_IN_USER_ID] = userId
        }
    }

    suspend fun clearSession() {
        context.appDataStore.edit { prefs ->
            prefs.remove(Keys.LOGGED_IN_USER_ID)
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun setCustomLogoPath(path: String?) {
        context.appDataStore.edit { prefs ->
            if (path.isNullOrBlank()) {
                prefs.remove(Keys.CUSTOM_LOGO_PATH)
            } else {
                prefs[Keys.CUSTOM_LOGO_PATH] = path
            }
        }
    }
}
