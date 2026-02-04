package com.example.genggaminmobile.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class PreferencesManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val LAST_LOGIN_TIME = longPreferencesKey("last_login_time")
        const val SESSION_TIMEOUT = 30 * 60 * 1000L // 30 Minutes
    }

    val authToken: Flow<String?> =
        context.dataStore.data
            .map { preferences ->
                preferences[AUTH_TOKEN]
            }

    val lastLoginTime: Flow<Long> =
        context.dataStore.data
            .map { preferences ->
                preferences[LAST_LOGIN_TIME] ?: 0L
            }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
            preferences[LAST_LOGIN_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * Updates the last activity time to extend the session.
     * Call this whenever a successful interaction happens if you want "inactivity" timeout.
     * If the user wants 30 mins since LOGIN, don't call this.
     */
    suspend fun updateLastActivityTime() {
        context.dataStore.edit { preferences ->
            preferences[LAST_LOGIN_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
