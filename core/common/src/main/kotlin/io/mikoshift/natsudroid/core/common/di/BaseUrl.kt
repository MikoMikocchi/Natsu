package io.mikoshift.natsudroid.core.common.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RootBaseUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OAuthClientId

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsDebugBuild
