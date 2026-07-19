package io.mikoshift.natsu.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.mikoshift.natsu.core.domain.repository.AuthRepository
import io.mikoshift.natsu.core.domain.repository.DictionaryRepository
import io.mikoshift.natsu.core.domain.repository.DocumentPackageRepository
import io.mikoshift.natsu.core.domain.repository.DocumentRepository
import io.mikoshift.natsu.core.domain.repository.ReaderSettingRepository
import io.mikoshift.natsu.core.domain.repository.SyncStatusRepository
import io.mikoshift.natsu.data.repository.AuthRepositoryImpl
import io.mikoshift.natsu.data.repository.DictionaryRepositoryImpl
import io.mikoshift.natsu.data.repository.DocumentPackageRepositoryImpl
import io.mikoshift.natsu.data.repository.DocumentRepositoryImpl
import io.mikoshift.natsu.data.repository.ReaderSettingRepositoryImpl
import io.mikoshift.natsu.data.repository.SyncStatusRepositoryImpl
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

    @Binds
    @Singleton
    abstract fun bindDocumentPackageRepository(impl: DocumentPackageRepositoryImpl): DocumentPackageRepository

    @Binds
    @Singleton
    abstract fun bindSyncStatusRepository(impl: SyncStatusRepositoryImpl): SyncStatusRepository

    @Binds
    @Singleton
    abstract fun bindReaderSettingRepository(impl: ReaderSettingRepositoryImpl): ReaderSettingRepository

    @Binds
    @Singleton
    abstract fun bindDictionaryRepository(impl: DictionaryRepositoryImpl): DictionaryRepository
}
