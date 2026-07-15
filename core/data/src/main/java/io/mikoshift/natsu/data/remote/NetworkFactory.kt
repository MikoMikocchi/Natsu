package io.mikoshift.natsu.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mikoshift.natsu.core.common.di.BaseUrl
import io.mikoshift.natsu.core.common.di.IsDebugBuild
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

@Singleton
class NetworkFactory @Inject constructor(
    @BaseUrl private val baseUrl: String,
    @IsDebugBuild private val isDebugBuild: Boolean,
) {

    val json: Json = Json { ignoreUnknownKeys = true }

    fun createUnauthenticatedOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor())
            .build()

    fun createRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    fun createAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    fun createDocumentApi(retrofit: Retrofit): DocumentApi = retrofit.create(DocumentApi::class.java)

    fun createAuthenticatedOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor())
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()

    fun createAuthenticatedRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    fun createAuthenticatedAuthApi(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): AuthApi = createAuthApi(
        createAuthenticatedRetrofit(createAuthenticatedOkHttpClient(authInterceptor, tokenAuthenticator)),
    )

    fun createAuthenticatedDocumentApi(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): DocumentApi = createDocumentApi(
        createAuthenticatedRetrofit(createAuthenticatedOkHttpClient(authInterceptor, tokenAuthenticator)),
    )

    private fun loggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (isDebugBuild) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
}
