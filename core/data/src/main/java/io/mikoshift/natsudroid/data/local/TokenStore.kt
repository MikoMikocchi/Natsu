package io.mikoshift.natsudroid.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.mikoshift.natsudroid.core.model.AuthSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore
@Inject
constructor(@ApplicationContext context: Context) {
    private val masterKey =
        MasterKey
            .Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private val prefs: SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    private val _sessionFlow = MutableStateFlow(readSession())
    val sessionFlow: StateFlow<AuthSession?> = _sessionFlow.asStateFlow()

    suspend fun saveSession(session: AuthSession) {
        withContext(Dispatchers.IO) {
            writeSessionToPrefs(session)
        }
        _sessionFlow.value = session
    }

    fun saveSessionBlocking(session: AuthSession) {
        writeSessionToPrefs(session)
        _sessionFlow.value = session
    }

    suspend fun clearSession() {
        withContext(Dispatchers.IO) {
            clearPrefs()
        }
        _sessionFlow.value = null
    }

    fun clearSessionBlocking() {
        clearPrefs()
        _sessionFlow.value = null
    }

    fun getAccessTokenBlocking(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshTokenBlocking(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getSessionBlocking(): AuthSession? = readSession()

    private fun writeSessionToPrefs(session: AuthSession) {
        prefs
            .edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_USER_ID, session.userId)
            .putString(KEY_USER_NAME, session.userName)
            .putString(KEY_USER_EMAIL, session.userEmail)
            .putLong(KEY_SAVED_AT_MS, System.currentTimeMillis())
            .apply()
    }

    private fun clearPrefs() {
        prefs.edit().clear().apply()
    }

    private fun readSession(): AuthSession? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val userId = prefs.getLong(KEY_USER_ID, -1L)
        val userName = prefs.getString(KEY_USER_NAME, null) ?: return null
        val userEmail = prefs.getString(KEY_USER_EMAIL, null) ?: return null
        if (userId < 0L) return null
        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            userName = userName,
            userEmail = userEmail,
        )
    }

    private companion object {
        const val PREFS_FILE_NAME = "natsudroid_auth_prefs"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_SAVED_AT_MS = "saved_at_ms"
    }
}
