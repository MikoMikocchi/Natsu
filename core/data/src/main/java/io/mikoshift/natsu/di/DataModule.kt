package io.mikoshift.natsu.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.mikoshift.natsu.data.local.PackageFileStore
import io.mikoshift.natsu.data.local.SyncCursorStore
import io.mikoshift.natsu.data.local.SyncOutboxStore
import io.mikoshift.natsu.data.local.db.DocumentCacheDao
import io.mikoshift.natsu.data.local.db.DocumentDao
import io.mikoshift.natsu.data.local.db.NatsuDatabase
import io.mikoshift.natsu.data.local.db.ReadingProgressDao
import io.mikoshift.natsu.data.local.db.SyncOutboxDao
import io.mikoshift.natsu.data.local.db.SyncStateDao
import io.mikoshift.natsu.data.remote.AuthApi
import io.mikoshift.natsu.data.remote.AuthInterceptor
import io.mikoshift.natsu.data.remote.DictionaryApi
import io.mikoshift.natsu.data.remote.DocumentApi
import io.mikoshift.natsu.data.remote.NetworkFactory
import io.mikoshift.natsu.data.remote.OAuthApi
import io.mikoshift.natsu.data.remote.ReaderSettingApi
import io.mikoshift.natsu.data.remote.TokenAuthenticator
import io.mikoshift.natsu.data.remote.UserInfoApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideNatsuDatabase(
        @ApplicationContext context: Context,
    ): NatsuDatabase = NatsuDatabase.create(context)

    @Provides
    fun provideDocumentDao(database: NatsuDatabase): DocumentDao = database.documentDao()

    @Provides
    fun provideReadingProgressDao(database: NatsuDatabase): ReadingProgressDao =
        database.readingProgressDao()

    @Provides
    fun provideDocumentCacheDao(database: NatsuDatabase): DocumentCacheDao =
        database.documentCacheDao()

    @Provides
    fun provideSyncOutboxDao(database: NatsuDatabase): SyncOutboxDao = database.syncOutboxDao()

    @Provides
    fun provideSyncStateDao(database: NatsuDatabase): SyncStateDao = database.syncStateDao()

    @Provides
    @Singleton
    fun provideSyncCursorStore(syncStateDao: SyncStateDao): SyncCursorStore =
        SyncCursorStore(syncStateDao)

    @Provides
    @Singleton
    fun provideSyncOutboxStore(syncOutboxDao: SyncOutboxDao): SyncOutboxStore =
        SyncOutboxStore(syncOutboxDao)

    @Provides
    @Singleton
    fun providePackageFileStore(
        @ApplicationContext context: Context,
    ): PackageFileStore = PackageFileStore(context)

    @Provides
    @UnauthenticatedAuthApi
    fun provideUnauthenticatedAuthApi(networkFactory: NetworkFactory): AuthApi =
        networkFactory.createAuthApi(
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
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): AuthApi = networkFactory.createAuthenticatedAuthApi(authInterceptor, tokenAuthenticator)

    @Provides
    @AuthenticatedOAuthApi
    fun provideAuthenticatedOAuthApi(
        networkFactory: NetworkFactory,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OAuthApi = networkFactory.createAuthenticatedOAuthApi(authInterceptor, tokenAuthenticator)

    @Provides
    @Singleton
    fun provideUserInfoApi(
        networkFactory: NetworkFactory,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): UserInfoApi = networkFactory.createAuthenticatedUserInfoApi(authInterceptor, tokenAuthenticator)

    @Provides
    @Singleton
    fun provideDocumentApi(
        networkFactory: NetworkFactory,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): DocumentApi = networkFactory.createAuthenticatedDocumentApi(authInterceptor, tokenAuthenticator)

    @Provides
    @Singleton
    fun provideReaderSettingApi(
        networkFactory: NetworkFactory,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): ReaderSettingApi = networkFactory.createAuthenticatedReaderSettingApi(authInterceptor, tokenAuthenticator)

    @Provides
    @Singleton
    fun provideDictionaryApi(
        networkFactory: NetworkFactory,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): DictionaryApi = networkFactory.createAuthenticatedDictionaryApi(authInterceptor, tokenAuthenticator)
}
