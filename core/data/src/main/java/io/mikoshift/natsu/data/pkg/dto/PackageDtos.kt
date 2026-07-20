package io.mikoshift.natsu.data.pkg.dto

import io.mikoshift.natsu.data.remote.dto.SourceFormat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class PackageManifestDto(
    @SerialName("schema_version") val schemaVersion: Int,
    val title: String,
    val authors: List<String> = emptyList(),
    val language: String? = null,
    @SerialName("cover_asset_id") val coverAssetId: String? = null,
    @SerialName("source_format") val sourceFormat: SourceFormat,
    val toc: List<TocNodeDto> = emptyList(),
    val sections: List<ManifestSectionDto> = emptyList(),
)

@Serializable
data class TocNodeDto(
    val title: String? = null,
    @SerialName("section_id") val sectionId: String? = null,
    val children: List<TocNodeDto> = emptyList(),
)

@Serializable
data class ManifestSectionDto(
    val id: String,
    val title: String? = null,
    val path: String,
    @SerialName("word_count") val wordCount: Int = 0,
    val checksum: String,
)

@Serializable
data class MarkDto(val type: MarkTypeDto, val start: Int, val end: Int)

@Serializable
enum class MarkTypeDto {
    @SerialName("BOLD")
    BOLD,

    @SerialName("ITALIC")
    ITALIC,
}

@Serializable
@JsonClassDiscriminator("type")
sealed interface BlockDto {
    val id: String
}

@Serializable
@SerialName("paragraph")
data class ParagraphBlockDto(override val id: String, val text: String, val marks: List<MarkDto> = emptyList()) :
    BlockDto

@Serializable
@SerialName("heading")
data class HeadingBlockDto(
    override val id: String,
    val level: Int,
    val text: String,
    val marks: List<MarkDto> = emptyList(),
) : BlockDto

@Serializable
@SerialName("image")
data class ImageBlockDto(
    override val id: String,
    @SerialName("asset_id") val assetId: String,
    val alt: String? = null,
) : BlockDto

@Serializable
@SerialName("blockquote")
data class BlockquoteBlockDto(override val id: String, val text: String, val marks: List<MarkDto> = emptyList()) :
    BlockDto

@Serializable
@SerialName("list_item")
data class ListItemBlockDto(
    override val id: String,
    val level: Int,
    val ordered: Boolean,
    val text: String,
    val marks: List<MarkDto> = emptyList(),
) : BlockDto

@Serializable
@SerialName("divider")
data class DividerBlockDto(override val id: String) : BlockDto
