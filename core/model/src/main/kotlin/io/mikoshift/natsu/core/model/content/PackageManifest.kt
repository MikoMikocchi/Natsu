package io.mikoshift.natsu.core.model.content

import io.mikoshift.natsu.core.model.SourceFormat

data class PackageManifest(
    val schemaVersion: Int,
    val title: String,
    val authors: List<String>,
    val language: String?,
    val coverAssetId: String?,
    val sourceFormat: SourceFormat,
    val toc: List<TocNode>,
    val sections: List<ManifestSection>,
)

data class TocNode(val title: String, val sectionId: String, val children: List<TocNode> = emptyList())

data class ManifestSection(
    val id: String,
    val title: String?,
    val path: String,
    val wordCount: Int,
    val checksum: String,
)
