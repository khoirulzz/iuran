package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

val Context.dataStore by preferencesDataStore(name = "gempala_prefs")

class SessionStore(private val context: Context) {
    private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    private val USER_ROLE = stringPreferencesKey("user_role")
    private val USER_ID = stringPreferencesKey("user_id")
    private val USER_NAME = stringPreferencesKey("user_name")
    private val DEVICE_ID = stringPreferencesKey("device_id")
    private val LAST_LOCAL_SEQUENCE = longPreferencesKey("last_local_sequence")
    
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    val userRole: Flow<String?> = context.dataStore.data.map { it[USER_ROLE] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val deviceId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DEVICE_ID] ?: UUID.randomUUID().toString().take(8).also { newId ->
            // In a real app we'd save this right away in a suspend block
        }
    }
    
    suspend fun getDeviceId(): String {
        var id = ""
        context.dataStore.edit { prefs ->
            id = prefs[DEVICE_ID] ?: UUID.randomUUID().toString().take(8).also {
                prefs[DEVICE_ID] = it
            }
        }
        return id
    }
    
    suspend fun reserveNextSequence(): Long {
        var reserved = 0L
        context.dataStore.edit { prefs ->
            reserved = (prefs[LAST_LOCAL_SEQUENCE] ?: 0L) + 1L
            prefs[LAST_LOCAL_SEQUENCE] = reserved
        }
        return reserved
    }

    suspend fun getSessionSnapshot(): SessionSnapshot {
        val prefs = context.dataStore.data.first()
        return SessionSnapshot(
            isLoggedIn = prefs[IS_LOGGED_IN] ?: false,
            userRole = prefs[USER_ROLE],
            userId = prefs[USER_ID],
            userName = prefs[USER_NAME]
        )
    }

    suspend fun saveSession(role: String, id: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = true
            prefs[USER_ROLE] = role
            prefs[USER_ID] = id
            prefs[USER_NAME] = name
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = false
            prefs.remove(USER_ROLE)
            prefs.remove(USER_ID)
            prefs.remove(USER_NAME)
        }
    }
}

data class SessionSnapshot(
    val isLoggedIn: Boolean,
    val userRole: String?,
    val userId: String?,
    val userName: String?
)

