package io.mikoshift.natsu.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.mikoshift.natsu.data.remote.dto.AuthResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * The currently authenticated session, derived from what the backend's [AuthResponse]
 * actually provides (see AuthResponses.kt). The backend does not hand back an explicit
 * token expiry, so none is modeled here.
 */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
    val userName: String,
    val userEmail: String,
)

/**
 * Persists the current auth session (tokens + basic user info) in an
 * [EncryptedSharedPreferences] file, and exposes it reactively via [sessionFlow].
 *
 * [getAccessTokenBlocking] / [getRefreshTokenBlocking] are provided as plain synchronous
 * reads (not suspend) because a later subtask needs to read tokens from an OkHttp
 * Interceptor/Authenticator, which run on background threads but cannot call suspend
 * functions. Reading a local EncryptedSharedPreferences value is cheap enough that this
 * doesn't need to go through coroutines.
 */
class TokenStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _sessionFlow = MutableStateFlow(readSession())
    val sessionFlow: StateFlow<AuthSession?> = _sessionFlow.asStateFlow()

    suspend fun saveSession(auth: AuthResponse) {
        withContext(Dispatchers.IO) {
            prefs.edit()
                .putString(KEY_ACCESS_TOKEN, auth.token)
                .putString(KEY_REFRESH_TOKEN, auth.refreshToken)
                .putLong(KEY_USER_ID, auth.user.id)
                .putString(KEY_USER_NAME, auth.user.name)
                .putString(KEY_USER_EMAIL, auth.user.email)
                .putLong(KEY_SAVED_AT_MS, System.currentTimeMillis())
                .apply()
        }
        _sessionFlow.value = AuthSession(
            accessToken = auth.token,
            refreshToken = auth.refreshToken,
            userId = auth.user.id,
            userName = auth.user.name,
            userEmail = auth.user.email,
        )
    }

    suspend fun clearSession() {
        withContext(Dispatchers.IO) {
            prefs.edit().clear().apply()
        }
        _sessionFlow.value = null
    }

    fun getAccessTokenBlocking(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshTokenBlocking(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

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
        const val PREFS_FILE_NAME = "natsu_auth_prefs"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_SAVED_AT_MS = "saved_at_ms"
    }
}
