package io.mikoshift.natsu.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mikoshift.natsu.core.common.di.BaseUrl
import io.mikoshift.natsu.core.common.di.IsDebugBuild
import io.mikoshift.natsu.core.common.di.RootBaseUrl
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
    @RootBaseUrl private val rootBaseUrl: String,
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

    fun createRootRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(rootBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    fun createAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    fun createOAuthApi(retrofit: Retrofit): OAuthApi = retrofit.create(OAuthApi::class.java)

    fun createUserInfoApi(retrofit: Retrofit): UserInfoApi = retrofit.create(UserInfoApi::class.java)

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

    fun createAuthenticatedRootRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(rootBaseUrl)
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

    fun createAuthenticatedReaderSettingApi(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): ReaderSettingApi = createReaderSettingApi(
        createAuthenticatedRetrofit(createAuthenticatedOkHttpClient(authInterceptor, tokenAuthenticator)),
    )

    fun createAuthenticatedDictionaryApi(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): DictionaryApi = createDictionaryApi(
        createAuthenticatedRetrofit(createAuthenticatedOkHttpClient(authInterceptor, tokenAuthenticator)),
    )

    fun createDictionaryApi(retrofit: Retrofit): DictionaryApi = retrofit.create(DictionaryApi::class.java)

    fun createReaderSettingApi(retrofit: Retrofit): ReaderSettingApi =
        retrofit.create(ReaderSettingApi::class.java)

    fun createUnauthenticatedOAuthApi(): OAuthApi = createOAuthApi(
        createRootRetrofit(createUnauthenticatedOkHttpClient()),
    )

    fun createAuthenticatedUserInfoApi(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): UserInfoApi = createUserInfoApi(
        createAuthenticatedRootRetrofit(createAuthenticatedOkHttpClient(authInterceptor, tokenAuthenticator)),
    )

    fun createAuthenticatedOAuthApi(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OAuthApi = createOAuthApi(
        createAuthenticatedRootRetrofit(createAuthenticatedOkHttpClient(authInterceptor, tokenAuthenticator)),
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
