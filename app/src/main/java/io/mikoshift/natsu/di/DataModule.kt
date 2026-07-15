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
import io.mikoshift.natsu.data.remote.DocumentApi
import io.mikoshift.natsu.data.remote.NetworkFactory
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
    fun provideUnauthenticatedAuthApi(): AuthApi =
        NetworkFactory.createAuthApi(
            NetworkFactory.createRetrofit(NetworkFactory.createUnauthenticatedOkHttpClient()),
        )

    @Provides
    @AuthenticatedAuthApi
    fun provideAuthenticatedAuthApi(tokenStore: TokenStore): AuthApi =
        NetworkFactory.createAuthenticatedAuthApi(tokenStore)

    @Provides
    @Singleton
    fun provideDocumentApi(tokenStore: TokenStore): DocumentApi =
        NetworkFactory.createAuthenticatedDocumentApi(tokenStore)
}
