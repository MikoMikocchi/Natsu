package io.mikoshift.natsu.data.repository

import io.mikoshift.natsu.core.domain.repository.DocumentPackageRepository
import io.mikoshift.natsu.core.model.content.DocumentPackage
import io.mikoshift.natsu.data.local.PackageFileStore
import io.mikoshift.natsu.data.pkg.PackageAssetStore
import io.mikoshift.natsu.data.pkg.PackageParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentPackageRepositoryImpl @Inject constructor(
    private val packageParser: PackageParser,
    private val packageFileStore: PackageFileStore,
    private val packageAssetStore: PackageAssetStore,
) : DocumentPackageRepository {

    override suspend fun openPackage(documentId: String): Result<DocumentPackage> =
        packageParser.parse(documentId)

    override suspend fun resolveAssetPath(documentId: String, assetId: String): String? {
        packageAssetStore.resolveCachedAsset(documentId, assetId)?.let { return it }
        val zipFile = packageFileStore.getPackageFile(documentId)
        if (!zipFile.exists()) return null
        return packageAssetStore.ensureAssetExtracted(documentId, zipFile, assetId)?.absolutePath
    }
}
