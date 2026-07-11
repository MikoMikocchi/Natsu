package io.mikoshift.natsu.di

import android.content.Context
import io.mikoshift.natsu.data.local.PackageFileStore
import io.mikoshift.natsu.data.local.SyncCursorStore
import io.mikoshift.natsu.data.local.TokenStore
import io.mikoshift.natsu.data.local.db.NatsuDatabase
import io.mikoshift.natsu.data.remote.AuthApi
import io.mikoshift.natsu.data.remote.DocumentApi
import io.mikoshift.natsu.data.remote.NetworkFactory
import io.mikoshift.natsu.data.repository.AuthRepository
import io.mikoshift.natsu.data.repository.DocumentRepository
import io.mikoshift.natsu.data.sync.DocumentSyncEngine

/**
 * Simple manual dependency container for the app (no Hilt/Dagger).
 *
 * Every property is lazily initialized via `by lazy`, so construction order (e.g. TokenStore
 * before the authenticated AuthApi before AuthRepository) is handled automatically without
 * needing an explicit init block.
 */
class AppContainer(private val context: Context) {

    val tokenStore: TokenStore by lazy { TokenStore(context) }

    val natsuDatabase: NatsuDatabase by lazy { NatsuDatabase.create(context) }

    private val packageFileStore: PackageFileStore by lazy { PackageFileStore(context) }

    private val syncCursorStore: SyncCursorStore by lazy {
        SyncCursorStore(natsuDatabase.syncStateDao())
    }

    private val unauthenticatedAuthApi: AuthApi by lazy {
        NetworkFactory.createAuthApi(
            NetworkFactory.createRetrofit(NetworkFactory.createUnauthenticatedOkHttpClient()),
        )
    }

    private val authenticatedAuthApi: AuthApi by lazy {
        NetworkFactory.createAuthenticatedAuthApi(tokenStore)
    }

    private val documentApi: DocumentApi by lazy {
        NetworkFactory.createAuthenticatedDocumentApi(tokenStore)
    }

    private val documentSyncEngine: DocumentSyncEngine by lazy {
        DocumentSyncEngine(
            documentApi = documentApi,
            documentDao = natsuDatabase.documentDao(),
            syncCursorStore = syncCursorStore,
            packageFileStore = packageFileStore,
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            unauthenticatedApi = unauthenticatedAuthApi,
            authenticatedApi = authenticatedAuthApi,
            tokenStore = tokenStore,
        )
    }

    val documentRepository: DocumentRepository by lazy {
        DocumentRepository(
            context = context,
            documentApi = documentApi,
            documentDao = natsuDatabase.documentDao(),
            syncCursorStore = syncCursorStore,
            packageFileStore = packageFileStore,
            syncEngine = documentSyncEngine,
        )
    }
}
