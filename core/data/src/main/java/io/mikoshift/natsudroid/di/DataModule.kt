package io.mikoshift.natsudroid.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.mikoshift.natsudroid.data.local.PackageFileStore
import io.mikoshift.natsudroid.data.local.SyncCursorStore
import io.mikoshift.natsudroid.data.local.SyncOutboxStore
import io.mikoshift.natsudroid.data.local.db.DocumentCacheDao
import io.mikoshift.natsudroid.data.local.db.DocumentDao
import io.mikoshift.natsudroid.data.local.db.NatsudroidDatabase
import io.mikoshift.natsudroid.data.local.db.ReadingProgressDao
import io.mikoshift.natsudroid.data.local.db.SyncOutboxDao
import io.mikoshift.natsudroid.data.local.db.SyncStateDao
import io.mikoshift.natsudroid.data.remote.AuthApi
import io.mikoshift.natsudroid.data.remote.AuthInterceptor
import io.mikoshift.natsudroid.data.remote.DictionaryApi
import io.mikoshift.natsudroid.data.remote.DocumentApi
import io.mikoshift.natsudroid.data.remote.NetworkFactory
import io.mikoshift.natsudroid.data.remote.OAuthApi
import io.mikoshift.natsudroid.data.remote.ReaderSettingApi
import io.mikoshift.natsudroid.data.remote.TokenAuthenticator
import io.mikoshift.natsudroid.data.remote.UserInfoApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideNatsudroidDatabase(@ApplicationContext context: Context): NatsudroidDatabase = NatsudroidDatabase.create(context)

    @Provides
    fun provideDocumentDao(database: NatsudroidDatabase): DocumentDao = database.documentDao()

    @Provides
    fun provideReadingProgressDao(database: NatsudroidDatabase): ReadingProgressDao = database.readingProgressDao()

    @Provides
    fun provideDocumentCacheDao(database: NatsudroidDatabase): DocumentCacheDao = database.documentCacheDao()

    @Provides
    fun provideSyncOutboxDao(database: NatsudroidDatabase): SyncOutboxDao = database.syncOutboxDao()

    @Provides
    fun provideSyncStateDao(database: NatsudroidDatabase): SyncStateDao = database.syncStateDao()

    @Provides
    @Singleton
    fun provideSyncCursorStore(syncStateDao: SyncStateDao): SyncCursorStore = SyncCursorStore(syncStateDao)

    @Provides
    @Singleton
    fun provideSyncOutboxStore(syncOutboxDao: SyncOutboxDao): SyncOutboxStore = SyncOutboxStore(syncOutboxDao)

    @Provides
    @Singleton
    fun providePackageFileStore(@ApplicationContext context: Context): PackageFileStore = PackageFileStore(context)

    @Provides
    @Singleton
    @AuthenticatedOkHttpClient
    fun provideAuthenticatedOkHttpClient(
        networkFactory: NetworkFactory,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = networkFactory.createAuthenticatedOkHttpClient(authInterceptor, tokenAuthenticator)

    @Provides
    @Singleton
    @AuthenticatedRetrofit
    fun provideAuthenticatedRetrofit(
        networkFactory: NetworkFactory,
        @AuthenticatedOkHttpClient okHttpClient: OkHttpClient,
    ): Retrofit = networkFactory.createRetrofit(okHttpClient)

    @Provides
    @Singleton
    @AuthenticatedRootRetrofit
    fun provideAuthenticatedRootRetrofit(
        networkFactory: NetworkFactory,
        @AuthenticatedOkHttpClient okHttpClient: OkHttpClient,
    ): Retrofit = networkFactory.createRootRetrofit(okHttpClient)

    @Provides
    @UnauthenticatedAuthApi
    fun provideUnauthenticatedAuthApi(networkFactory: NetworkFactory): AuthApi = networkFactory.createAuthApi(
        networkFactory.createRetrofit(networkFactory.createUnauthenticatedOkHttpClient()),
    )

    @Provides
    @UnauthenticatedOAuthApi
    fun provideUnauthenticatedOAuthApi(networkFactory: NetworkFactory): OAuthApi =
        networkFactory.createUnauthenticatedOAuthApi()

    @Provides
    @AuthenticatedAuthApi
    fun provideAuthenticatedAuthApi(
        networkFactory: NetworkFactory,
        @AuthenticatedRetrofit retrofit: Retrofit,
    ): AuthApi = networkFactory.createAuthApi(retrofit)

    @Provides
    @AuthenticatedOAuthApi
    fun provideAuthenticatedOAuthApi(
        networkFactory: NetworkFactory,
        @AuthenticatedRootRetrofit retrofit: Retrofit,
    ): OAuthApi = networkFactory.createOAuthApi(retrofit)

    @Provides
    @Singleton
    fun provideUserInfoApi(
        networkFactory: NetworkFactory,
        @AuthenticatedRootRetrofit retrofit: Retrofit,
    ): UserInfoApi = networkFactory.createUserInfoApi(retrofit)

    @Provides
    @Singleton
    fun provideDocumentApi(networkFactory: NetworkFactory, @AuthenticatedRetrofit retrofit: Retrofit): DocumentApi =
        networkFactory.createDocumentApi(retrofit)

    @Provides
    @Singleton
    fun provideReaderSettingApi(
        networkFactory: NetworkFactory,
        @AuthenticatedRetrofit retrofit: Retrofit,
    ): ReaderSettingApi = networkFactory.createReaderSettingApi(retrofit)

    @Provides
    @Singleton
    fun provideDictionaryApi(
        networkFactory: NetworkFactory,
        @AuthenticatedRetrofit retrofit: Retrofit,
    ): DictionaryApi = networkFactory.createDictionaryApi(retrofit)
}
