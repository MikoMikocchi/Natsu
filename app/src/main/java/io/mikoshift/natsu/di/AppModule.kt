package io.mikoshift.natsu.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.mikoshift.natsu.BuildConfig
import io.mikoshift.natsu.core.common.di.BaseUrl
import io.mikoshift.natsu.core.common.di.IsDebugBuild
import io.mikoshift.natsu.core.common.di.OAuthClientId
import io.mikoshift.natsu.core.common.di.RootBaseUrl

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
