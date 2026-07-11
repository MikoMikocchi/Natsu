package io.mikoshift.natsu.data.remote

import io.mikoshift.natsu.data.local.TokenStore
import io.mikoshift.natsu.data.remote.dto.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Handles automatic access-token refresh when a request comes back with a 401.
 *
 * This runs on the authenticated OkHttp client (see [NetworkFactory]), so it must call the
 * refresh endpoint through a *separate*, plain/unauthenticated Retrofit [AuthApi] instance
 * (built here from [NetworkFactory]'s unauthenticated builder functions) rather than the
 * authenticated client — reusing the authenticated client would recurse back into this same
 * Authenticator.
 *
 * [Authenticator.authenticate] is a blocking, synchronous OkHttp callback by contract, so the
 * suspend calls into [AuthApi.refresh] and [TokenStore] are bridged with [runBlocking]. This is
 * one of the few legitimate uses of `runBlocking` in this codebase.
 */
class TokenAuthenticator(private val tokenStore: TokenStore) : Authenticator {

    private val refreshApi: AuthApi by lazy {
        NetworkFactory.createAuthApi(
            NetworkFactory.createRetrofit(NetworkFactory.createUnauthenticatedOkHttpClient()),
        )
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Avoid infinite retry loops: if we already tried to refresh once for this call
        // (2+ prior responses chained), give up and let the 401 surface to the caller.
        if (responseChainLength(response) >= 2) {
            return null
        }

        val refreshToken = tokenStore.getRefreshTokenBlocking() ?: return null

        return try {
            val refreshResponse = runBlocking {
                refreshApi.refresh(RefreshRequest(refreshToken = refreshToken))
            }

            val authResponse = refreshResponse.body()
            if (!refreshResponse.isSuccessful || authResponse == null) {
                runBlocking { tokenStore.clearSession() }
                return null
            }

            runBlocking { tokenStore.saveSession(authResponse) }

            response.request.newBuilder()
                .header("Authorization", "Bearer ${authResponse.token}")
                .build()
        } catch (_: Exception) {
            runBlocking { tokenStore.clearSession() }
            null
        }
    }

    private fun responseChainLength(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
