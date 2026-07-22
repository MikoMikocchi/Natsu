package io.mikoshift.natsudroid.core.domain.repository

import io.mikoshift.natsudroid.core.model.content.DocumentPackage

interface DocumentPackageRepository {
    suspend fun openPackage(documentId: String): Result<DocumentPackage>

    suspend fun resolveAssetPath(documentId: String, assetId: String): String?
}
