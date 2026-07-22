package io.mikoshift.natsudroid.data.pkg.mapper

import io.mikoshift.natsudroid.core.model.SourceFormat
import io.mikoshift.natsudroid.core.model.content.Block
import io.mikoshift.natsudroid.core.model.content.BlockquoteBlock
import io.mikoshift.natsudroid.core.model.content.DividerBlock
import io.mikoshift.natsudroid.core.model.content.DocumentPackage
import io.mikoshift.natsudroid.core.model.content.HeadingBlock
import io.mikoshift.natsudroid.core.model.content.ImageBlock
import io.mikoshift.natsudroid.core.model.content.ListItemBlock
import io.mikoshift.natsudroid.core.model.content.ManifestSection
import io.mikoshift.natsudroid.core.model.content.Mark
import io.mikoshift.natsudroid.core.model.content.MarkType
import io.mikoshift.natsudroid.core.model.content.PackageManifest
import io.mikoshift.natsudroid.core.model.content.ParagraphBlock
import io.mikoshift.natsudroid.core.model.content.TocNode
import io.mikoshift.natsudroid.data.pkg.dto.BlockDto
import io.mikoshift.natsudroid.data.pkg.dto.BlockquoteBlockDto
import io.mikoshift.natsudroid.data.pkg.dto.DividerBlockDto
import io.mikoshift.natsudroid.data.pkg.dto.HeadingBlockDto
import io.mikoshift.natsudroid.data.pkg.dto.ImageBlockDto
import io.mikoshift.natsudroid.data.pkg.dto.ListItemBlockDto
import io.mikoshift.natsudroid.data.pkg.dto.ManifestSectionDto
import io.mikoshift.natsudroid.data.pkg.dto.MarkDto
import io.mikoshift.natsudroid.data.pkg.dto.MarkTypeDto
import io.mikoshift.natsudroid.data.pkg.dto.PackageManifestDto
import io.mikoshift.natsudroid.data.pkg.dto.ParagraphBlockDto
import io.mikoshift.natsudroid.data.pkg.dto.TocNodeDto
import io.mikoshift.natsudroid.data.remote.dto.SourceFormat as SourceFormatDto

fun PackageManifestDto.toDomain(sections: Map<String, List<Block>>): DocumentPackage = DocumentPackage(
    manifest =
    PackageManifest(
        schemaVersion = schemaVersion,
        title = title,
        authors = authors,
        language = language,
        coverAssetId = coverAssetId,
        sourceFormat = sourceFormat.toDomain(),
        toc = toc.map { it.toDomain() },
        sections = this.sections.map { it.toDomain() },
    ),
    sections = sections,
)

private fun ManifestSectionDto.toDomain(): ManifestSection = ManifestSection(
    id = id,
    title = title,
    path = path,
    wordCount = wordCount,
    checksum = checksum,
)

private fun TocNodeDto.toDomain(): TocNode = TocNode(
    title = title,
    sectionId = sectionId,
    children = children.map { it.toDomain() },
)

fun BlockDto.toDomain(): Block = when (this) {
    is ParagraphBlockDto ->
        ParagraphBlock(
            id = id,
            text = text,
            marks = marks.map { it.toDomain() },
        )

    is HeadingBlockDto ->
        HeadingBlock(
            id = id,
            level = level,
            text = text,
            marks = marks.map { it.toDomain() },
        )

    is ImageBlockDto ->
        ImageBlock(
            id = id,
            assetId = assetId,
            alt = alt,
        )

    is BlockquoteBlockDto ->
        BlockquoteBlock(
            id = id,
            text = text,
            marks = marks.map { it.toDomain() },
        )

    is ListItemBlockDto ->
        ListItemBlock(
            id = id,
            level = level,
            ordered = ordered,
            text = text,
            marks = marks.map { it.toDomain() },
        )

    is DividerBlockDto -> DividerBlock(id = id)
}

private fun MarkDto.toDomain(): Mark = Mark(
    type =
    when (type) {
        MarkTypeDto.BOLD -> MarkType.BOLD
        MarkTypeDto.ITALIC -> MarkType.ITALIC
    },
    start = start,
    end = end,
)

private fun SourceFormatDto.toDomain(): SourceFormat = SourceFormat.valueOf(name)
