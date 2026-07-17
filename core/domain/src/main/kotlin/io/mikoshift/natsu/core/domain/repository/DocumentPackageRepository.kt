package io.mikoshift.natsu.core.domain.repository

import io.mikoshift.natsu.core.model.content.DocumentPackage

interface DocumentPackageRepository {
    suspend fun openPackage(documentId: String): Result<DocumentPackage>

    suspend fun resolveAssetPath(documentId: String, assetId: String): String?
}
