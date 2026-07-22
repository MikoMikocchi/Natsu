package io.mikoshift.natsudroid.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.mikoshift.natsudroid.BuildConfig
import io.mikoshift.natsudroid.core.common.di.BaseUrl
import io.mikoshift.natsudroid.core.common.di.IsDebugBuild
import io.mikoshift.natsudroid.core.common.di.OAuthClientId
import io.mikoshift.natsudroid.core.common.di.RootBaseUrl

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @BaseUrl
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    @Provides
    @RootBaseUrl
    fun provideRootBaseUrl(): String = BuildConfig.ROOT_BASE_URL

    @Provides
    @OAuthClientId
    fun provideOAuthClientId(): String = BuildConfig.OAUTH_CLIENT_ID

    @Provides
    @IsDebugBuild
    fun provideIsDebugBuild(): Boolean = BuildConfig.DEBUG
}
