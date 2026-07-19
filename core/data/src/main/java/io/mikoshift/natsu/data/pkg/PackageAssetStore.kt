package io.mikoshift.natsu.data.pkg

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageAssetStore
@Inject
constructor(@ApplicationContext private val context: Context) {
    private val assetsRoot: File = File(context.filesDir, "package-assets").apply { mkdirs() }

    suspend fun ensureAssetExtracted(documentId: String, packageZip: File, assetId: String): File? =
        withContext(Dispatchers.IO) {
            val documentDir = File(assetsRoot, documentId).apply { mkdirs() }
            val existing =
                documentDir
                    .listFiles()
                    ?.firstOrNull { file -> file.name.startsWith(assetId) }
            if (existing != null && existing.exists()) {
                return@withContext existing
            }

            ZipFile(packageZip).use { zip ->
                val entry =
                    zip
                        .entries()
                        .asSequence()
                        .firstOrNull { zipEntry -> zipEntry.name.startsWith("assets/$assetId") }
                        ?: return@withContext null
                val target = File(documentDir, entry.name.substringAfterLast('/'))
                zip.getInputStream(entry).use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                target
            }
        }

    fun resolveCachedAsset(documentId: String, assetId: String): String? {
        val documentDir = File(assetsRoot, documentId)
        if (!documentDir.exists()) return null
        return documentDir
            .listFiles()
            ?.firstOrNull { file -> file.name.startsWith(assetId) }
            ?.absolutePath
    }

    fun deleteDocumentAssets(documentId: String) {
        File(assetsRoot, documentId).deleteRecursively()
    }

    fun deleteAll() {
        assetsRoot.listFiles()?.forEach { it.deleteRecursively() }
    }
}
