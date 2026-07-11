package io.mikoshift.natsu.di

import android.content.Context
import io.mikoshift.natsu.data.local.TokenStore
import io.mikoshift.natsu.data.remote.AuthApi
import io.mikoshift.natsu.data.remote.NetworkFactory
import io.mikoshift.natsu.data.repository.AuthRepository

/**
 * Simple manual dependency container for the app (no Hilt/Dagger).
 *
 * Every property is lazily initialized via `by lazy`, so construction order (e.g. TokenStore
 * before the authenticated AuthApi before AuthRepository) is handled automatically without
 * needing an explicit init block.
 */
class AppContainer(private val context: Context) {

    val tokenStore: TokenStore by lazy { TokenStore(context) }

    private val unauthenticatedAuthApi: AuthApi by lazy {
        NetworkFactory.createAuthApi(
            NetworkFactory.createRetrofit(NetworkFactory.createUnauthenticatedOkHttpClient()),
        )
    }

    private val authenticatedAuthApi: AuthApi by lazy {
        NetworkFactory.createAuthenticatedAuthApi(tokenStore)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            unauthenticatedApi = unauthenticatedAuthApi,
            authenticatedApi = authenticatedAuthApi,
            tokenStore = tokenStore,
        )
    }
}
