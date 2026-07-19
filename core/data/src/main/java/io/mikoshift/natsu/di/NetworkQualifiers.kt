package io.mikoshift.natsu.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedRootRetrofit
