package io.mikoshift.natsu.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.mikoshift.natsu.data.local.PackageFileStore
import io.mikoshift.natsu.data.local.SyncCursorStore
import io.mikoshift.natsu.data.local.TokenStore
import io.mikoshift.natsu.data.local.db.DocumentDao
import io.mikoshift.natsu.data.local.db.NatsuDatabase
import io.mikoshift.natsu.data.local.db.SyncStateDao
import io.mikoshift.natsu.data.remote.AuthApi
import io.mikoshift.natsu.data.remote.AuthInterceptor
import io.mikoshift.natsu.data.remote.DocumentApi
import io.mikoshift.natsu.data.remote.NetworkFactory
import io.mikoshift.natsu.data.remote.TokenAuthenticator
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
    fun provideSyncStateDao(database: NatsuDatabase): SyncStateDao = database.syncStateDao()

    @Provides
    @Singleton
    fun provideSyncCursorStore(syncStateDao: SyncStateDao): SyncCursorStore =
        SyncCursorStore(syncStateDao)

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
    @AuthenticatedAuthApi
    fun provideAuthenticatedAuthApi(
        networkFactory: NetworkFactory,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): AuthApi = networkFactory.createAuthenticatedAuthApi(authInterceptor, tokenAuthenticator)

    @Provides
    @Singleton
    fun provideDocumentApi(
        networkFactory: NetworkFactory,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): DocumentApi = networkFactory.createAuthenticatedDocumentApi(authInterceptor, tokenAuthenticator)
}
