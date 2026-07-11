package io.mikoshift.natsu.data.remote

import io.mikoshift.natsu.data.local.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches `Authorization: Bearer <access token>` to outgoing requests, reading the current
 * access token synchronously from [TokenStore].
 *
 * Requests to the backend's unauthenticated `/v1/auth` endpoints (register/login/refresh/
 * password forgot/reset) are passed through untouched, since those calls happen before a
 * session exists (or, in the case of refresh, are handled by [TokenAuthenticator] instead).
 *
 * If a caller already attached an `Authorization` header manually (e.g. via [AuthApi]'s
 * explicit `@Header("Authorization")` parameters), it is replaced rather than duplicated, so
 * this interceptor can be the single source of truth for the header on the authenticated
 * OkHttp client built in [NetworkFactory].
 */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (UNAUTHENTICATED_PATH_SUFFIXES.any { path.endsWith(it) }) {
            return chain.proceed(request)
        }

        val accessToken = tokenStore.getAccessTokenBlocking()
            ?: return chain.proceed(request)

        val authenticatedRequest = request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        return chain.proceed(authenticatedRequest)
    }

    private companion object {
        val UNAUTHENTICATED_PATH_SUFFIXES = listOf(
            "auth/register",
            "auth/login",
            "auth/refresh",
            "auth/password/forgot",
            "auth/password/reset",
        )
    }
}
