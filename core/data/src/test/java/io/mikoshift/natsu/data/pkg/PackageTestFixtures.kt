package io.mikoshift.natsu.data.pkg

import io.mikoshift.natsu.data.pkg.dto.ManifestSectionDto
import io.mikoshift.natsu.data.pkg.dto.PackageManifestDto
import io.mikoshift.natsu.data.pkg.dto.TocNodeDto
import io.mikoshift.natsu.data.remote.dto.SourceFormat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PackageTestFixtures {
    private val json = Json { ignoreUnknownKeys = true }

    fun sampleZipBytes(): ByteArray {
        val sectionBytes =
            """
            [
              {"type":"paragraph","id":"section-0-b0","text":"Hello world.","marks":[]},
              {"type":"heading","id":"section-0-b1","level":1,"text":"Chapter One","marks":[]},
              {"type":"image","id":"section-0-b2","asset_id":"abc123","alt":"a picture"}
            ]
            """.trimIndent().toByteArray(StandardCharsets.UTF_8)
        val checksum = sha256Hex(sectionBytes)

        val manifest =
            PackageManifestDto(
                schemaVersion = 2,
                title = "My Book",
                authors = listOf("Jane Author"),
                language = "en",
                coverAssetId = "abc123",
                sourceFormat = SourceFormat.EPUB,
                toc = listOf(TocNodeDto(title = "Chapter One", sectionId = "section-0")),
                sections =
                listOf(
                    ManifestSectionDto(
                        id = "section-0",
                        title = "Chapter One",
                        path = "sections/section-0.json",
                        wordCount = 4,
                        checksum = checksum,
                    ),
                ),
            )
        val manifestBytes =
            json
                .encodeToString(manifest)
                .toByteArray(StandardCharsets.UTF_8)
        val assetBytes = byteArrayOf(1, 2, 3)

        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer, StandardCharsets.UTF_8).use { zip ->
            writeEntry(zip, "sections/section-0.json", sectionBytes)
            writeEntry(zip, "assets/abc123.png", assetBytes)
            writeEntry(zip, "manifest.json", manifestBytes)
        }
        return buffer.toByteArray()
    }

    private fun writeEntry(zip: ZipOutputStream, path: String, content: ByteArray) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content)
        zip.closeEntry()
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
