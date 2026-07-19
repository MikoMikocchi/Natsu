package io.mikoshift.natsu.data.pkg

import io.mikoshift.natsu.core.model.DocumentError
import io.mikoshift.natsu.core.model.content.DocumentPackage
import io.mikoshift.natsu.data.local.PackageFileStore
import io.mikoshift.natsu.data.pkg.dto.BlockDto
import io.mikoshift.natsu.data.pkg.dto.PackageManifestDto
import io.mikoshift.natsu.data.pkg.mapper.toDomain
import io.mikoshift.natsu.data.remote.NetworkFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageParser
@Inject
constructor(
    private val networkFactory: NetworkFactory,
    private val packageFileStore: PackageFileStore,
) {
    suspend fun parse(documentId: String): Result<DocumentPackage> = withContext(Dispatchers.IO) {
        runCatching {
            val zipFile = packageFileStore.getPackageFile(documentId)
            if (!zipFile.exists()) {
                throw DocumentError.Unknown("Package file not found")
            }
            parseZip(zipFile)
        }
    }

    internal fun parseZip(zipFile: File): DocumentPackage {
        ZipFile(zipFile).use { zip ->
            val manifestEntry =
                zip.getEntry(MANIFEST_PATH)
                    ?: throw DocumentError.Unknown("Missing manifest.json")
            val manifestBytes = zip.getInputStream(manifestEntry).use { it.readBytes() }
            val manifestDto =
                networkFactory.json.decodeFromString(
                    PackageManifestDto.serializer(),
                    manifestBytes.decodeToString(),
                )
            if (manifestDto.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
                throw DocumentError.Unknown("Unsupported package schema version ${manifestDto.schemaVersion}")
            }

            val sections =
                manifestDto.sections.associate { section ->
                    val entry =
                        zip.getEntry(section.path)
                            ?: throw DocumentError.Unknown("Missing section file ${section.path}")
                    val sectionBytes = zip.getInputStream(entry).use { it.readBytes() }
                    val blocks =
                        networkFactory.json
                            .decodeFromString(
                                ListSerializer(BlockDto.serializer()),
                                sectionBytes.decodeToString(),
                            ).map { it.toDomain() }
                    section.id to blocks
                }

            return manifestDto.toDomain(sections)
        }
    }

    private companion object {
        const val MANIFEST_PATH = "manifest.json"
        const val SUPPORTED_SCHEMA_VERSION = 2
    }
}
