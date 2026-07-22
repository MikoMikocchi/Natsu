package io.mikoshift.natsudroid.data.remote

import io.mikoshift.natsudroid.data.local.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor
@Inject
constructor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (UNAUTHENTICATED_PATH_SUFFIXES.any { path.endsWith(it) }) {
            return chain.proceed(request)
        }

        val accessToken =
            tokenStore.getAccessTokenBlocking()
                ?: return chain.proceed(request)

        val authenticatedRequest =
            request
                .newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()

        return chain.proceed(authenticatedRequest)
    }

    private companion object {
        val UNAUTHENTICATED_PATH_SUFFIXES =
            listOf(
                "auth/register",
                "auth/login",
                "oauth2/token",
                "oauth2/revoke",
                "auth/password/forgot",
                "auth/password/reset",
            )
    }
}
