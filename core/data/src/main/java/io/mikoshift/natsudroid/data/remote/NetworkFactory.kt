package io.mikoshift.natsudroid.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mikoshift.natsudroid.core.common.di.BaseUrl
import io.mikoshift.natsudroid.core.common.di.IsDebugBuild
import io.mikoshift.natsudroid.core.common.di.RootBaseUrl
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkFactory
@Inject
constructor(
    @BaseUrl private val baseUrl: String,
    @RootBaseUrl private val rootBaseUrl: String,
    @IsDebugBuild private val isDebugBuild: Boolean,
) {
    val json: Json = Json { ignoreUnknownKeys = true }

    fun createUnauthenticatedOkHttpClient(): OkHttpClient = OkHttpClient
        .Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor())
        .build()

    fun createRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    fun createRootRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit
            .Builder()
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
    ): OkHttpClient = OkHttpClient
        .Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor())
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .build()

    fun createDictionaryApi(retrofit: Retrofit): DictionaryApi = retrofit.create(DictionaryApi::class.java)

    fun createReaderSettingApi(retrofit: Retrofit): ReaderSettingApi = retrofit.create(ReaderSettingApi::class.java)

    fun createUnauthenticatedOAuthApi(): OAuthApi = createOAuthApi(
        createRootRetrofit(createUnauthenticatedOkHttpClient()),
    )

    private fun loggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level =
            if (isDebugBuild) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
    }
}
