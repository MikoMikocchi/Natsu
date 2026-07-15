package io.mikoshift.natsu.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.mikoshift.natsu.core.domain.repository.AuthRepository
import io.mikoshift.natsu.core.domain.repository.DocumentRepository
import io.mikoshift.natsu.data.repository.AuthRepositoryImpl
import io.mikoshift.natsu.data.repository.DocumentRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(impl: DocumentRepositoryImpl): DocumentRepository
}
