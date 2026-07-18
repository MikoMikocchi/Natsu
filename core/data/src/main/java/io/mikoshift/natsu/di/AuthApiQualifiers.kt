package io.mikoshift.natsu.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UnauthenticatedAuthApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedAuthApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UnauthenticatedOAuthApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedOAuthApi
