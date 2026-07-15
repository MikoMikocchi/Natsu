package io.mikoshift.natsu.data.remote

import io.mikoshift.natsu.data.local.TokenStore
import io.mikoshift.natsu.data.mapper.toSession
import io.mikoshift.natsu.data.remote.dto.RefreshRequest
import io.mikoshift.natsu.di.UnauthenticatedAuthApi
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    @UnauthenticatedAuthApi private val refreshApi: AuthApi,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
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

            runBlocking { tokenStore.saveSession(authResponse.toSession()) }

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
