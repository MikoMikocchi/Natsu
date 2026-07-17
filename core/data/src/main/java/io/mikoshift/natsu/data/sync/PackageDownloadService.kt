package io.mikoshift.natsu.data.sync

import io.mikoshift.natsu.core.model.DocumentError
import io.mikoshift.natsu.core.model.DocumentStatus
import io.mikoshift.natsu.data.local.PackageFileStore
import io.mikoshift.natsu.data.local.db.DocumentCacheDao
import io.mikoshift.natsu.data.local.db.DocumentCacheEntity
import io.mikoshift.natsu.data.local.db.DocumentDao
import io.mikoshift.natsu.data.remote.DocumentApi
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class PackageDownloadService @Inject constructor(
    private val documentApi: DocumentApi,
    private val documentDao: DocumentDao,
    private val documentCacheDao: DocumentCacheDao,
    private val packageFileStore: PackageFileStore,
) {
    suspend fun downloadMissingPackages() {
        val pending = documentDao.getDocumentsNeedingPackageDownload()
        for (documentWithRelations in pending) {
            download(documentWithRelations.document.id)
        }
    }

    suspend fun download(documentId: String): Result<Unit> = runCatching {
        val document = documentDao.getById(documentId)
            ?: throw DocumentError.Unknown("Document not found")
        if (document.status != DocumentStatus.READY) {
            throw DocumentError.PackageNotReady
        }
        val sha256 = document.packageSha256
            ?: throw DocumentError.PackageNotReady

        val cache = documentCacheDao.getByDocumentId(documentId)
        if (cache?.cachedPackageSha256 == sha256 && packageFileStore.getPath(documentId) != null) {
            return@runCatching
        }

        val response = documentApi.downloadPackage(documentId)
        if (!response.isSuccessful) {
            throw mapErrorResponse(response)
        }
        val body = response.body() ?: throw DocumentError.Unknown("Empty package response")
        try {
            val path = packageFileStore.save(documentId, body)
            documentCacheDao.upsert(
                DocumentCacheEntity(
                    documentId = documentId,
                    localPackagePath = path,
                    cachedPackageSha256 = sha256,
                ),
            )
        } finally {
            body.close()
        }
    }

    private fun <T> mapErrorResponse(response: Response<T>): DocumentError =
        if (response.code() == 401) {
            DocumentError.Unauthorized
        } else {
            DocumentError.Unknown(response.message())
        }
}
