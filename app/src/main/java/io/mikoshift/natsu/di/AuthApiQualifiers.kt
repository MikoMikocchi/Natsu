package io.mikoshift.natsu.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UnauthenticatedAuthApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedAuthApi
