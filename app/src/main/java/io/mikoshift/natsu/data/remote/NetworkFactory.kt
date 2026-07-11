package io.mikoshift.natsu.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mikoshift.natsu.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * Builds the unauthenticated networking stack (Json, OkHttpClient, Retrofit, AuthApi).
 *
 * Split into small, reusable pieces (rather than one giant function) so a later subtask can
 * add a second, *authenticated* OkHttpClient/Retrofit instance (e.g. with a token-attaching
 * interceptor) without having to rewrite this factory.
 */
object NetworkFactory {

    /** Single shared [Json] instance; forward-compatible with unknown backend fields. */
    val json: Json by lazy {
        Json { ignoreUnknownKeys = true }
    }

    /**
     * Logging interceptor that logs full request/response bodies in debug builds only, and
     * is silent in release builds.
     */
    private fun loggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    /** Plain (unauthenticated) OkHttpClient, suitable for register/login/refresh/etc. */
    fun createUnauthenticatedOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor())
            .build()

    /** Builds a Retrofit instance from the given [OkHttpClient] and [NetworkConfig.BASE_URL]. */
    fun createRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    /** Builds an [AuthApi] backed by the given [Retrofit] instance. */
    fun createAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    /**
     * Convenience: the unauthenticated [AuthApi], built from a fresh unauthenticated
     * OkHttpClient + Retrofit instance.
     */
    val authApi: AuthApi by lazy {
        createAuthApi(createRetrofit(createUnauthenticatedOkHttpClient()))
    }
}
