package io.mikoshift.natsu.data.remote

import io.mikoshift.natsu.core.common.di.OAuthClientId
import io.mikoshift.natsu.data.local.TokenStore
import io.mikoshift.natsu.data.mapper.mergeTokens
import io.mikoshift.natsu.di.UnauthenticatedOAuthApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    @UnauthenticatedOAuthApi private val oauthApi: OAuthApi,
    @OAuthClientId private val clientId: String,
) : Authenticator {

    private val refreshMutex = Mutex()
    private var ongoingRefresh: CompletableDeferred<String?>? = null

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseChainLength(response) >= 2) {
            return null
        }

        val accessToken = runBlocking { refreshAccessToken() } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
    }

    private suspend fun refreshAccessToken(): String? {
        val role = refreshMutex.withLock {
            ongoingRefresh?.let { return@withLock RefreshRole.Follower(it) }

            val deferred = CompletableDeferred<String?>()
            ongoingRefresh = deferred
            RefreshRole.Leader(deferred)
        }

        return when (role) {
            is RefreshRole.Follower -> role.deferred.await()
            is RefreshRole.Leader -> {
                val accessToken = try {
                    performRefresh()
                } catch (_: Exception) {
                    tokenStore.clearSessionBlocking()
                    null
                }

                role.deferred.complete(accessToken)
                refreshMutex.withLock {
                    if (ongoingRefresh === role.deferred) {
                        ongoingRefresh = null
                    }
                }
                accessToken
            }
        }
    }

    private suspend fun performRefresh(): String? {
        val existingSession = tokenStore.getSessionBlocking() ?: run {
            tokenStore.clearSessionBlocking()
            return null
        }
        val refreshToken = existingSession.refreshToken

        val refreshResponse = oauthApi.refresh(
            clientId = clientId,
            refreshToken = refreshToken,
        )
        val tokenResponse = refreshResponse.body()
        if (!refreshResponse.isSuccessful || tokenResponse == null) {
            tokenStore.clearSessionBlocking()
            return null
        }

        val updatedSession = tokenResponse.mergeTokens(existingSession)
        tokenStore.saveSessionBlocking(updatedSession)
        return updatedSession.accessToken
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

    private sealed interface RefreshRole {
        data class Leader(val deferred: CompletableDeferred<String?>) : RefreshRole
        data class Follower(val deferred: CompletableDeferred<String?>) : RefreshRole
    }
}
