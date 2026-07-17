package io.mikoshift.natsu.core.model.content

import io.mikoshift.natsu.core.model.SourceFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class PlainTextIndexTest {

    @Test
    fun extractPlainText_matchesBackendOrder() {
        val documentPackage = DocumentPackage(
            manifest = PackageManifest(
                schemaVersion = 2,
                title = "Book",
                authors = emptyList(),
                language = null,
                coverAssetId = null,
                sourceFormat = SourceFormat.PLAIN_TEXT,
                toc = emptyList(),
                sections = listOf(
                    ManifestSection(
                        id = "section-0",
                        title = null,
                        path = "sections/section-0.json",
                        wordCount = 0,
                        checksum = "",
                    ),
                ),
            ),
            sections = mapOf(
                "section-0" to listOf(
                    HeadingBlock(id = "b0", level = 1, text = "Title"),
                    ParagraphBlock(id = "b1", text = "Body text."),
                ),
            ),
        )

        val index = PlainTextIndex.fromDocumentPackage(documentPackage)

        assertEquals("Title\nBody text.", index.plainText)
    }

    @Test
    fun locateGlobalOffset_mapsToBlockPosition() {
        val documentPackage = samplePackage()
        val index = PlainTextIndex.fromDocumentPackage(documentPackage)

        val position = index.locateGlobalOffset("Hello".length + 1)!!

        assertEquals("section-0", position.sectionId)
        assertEquals(1, position.blockIndex)
        assertEquals(0, position.blockCharOffset)
    }

    @Test
    fun locateFromSearch_skipsTitlePrefix() {
        val documentPackage = samplePackage()
        val index = PlainTextIndex.fromDocumentPackage(documentPackage)
        val title = "My Book"

        val position = index.locateFromSearch(title, title.length + 1 + "Hello".length + 1)!!

        assertEquals(1, position.blockIndex)
    }

    private fun samplePackage(): DocumentPackage = DocumentPackage(
        manifest = PackageManifest(
            schemaVersion = 2,
            title = "My Book",
            authors = emptyList(),
            language = null,
            coverAssetId = null,
            sourceFormat = SourceFormat.EPUB,
            toc = emptyList(),
            sections = listOf(
                ManifestSection(
                    id = "section-0",
                    title = "Chapter",
                    path = "sections/section-0.json",
                    wordCount = 0,
                    checksum = "",
                ),
            ),
        ),
        sections = mapOf(
            "section-0" to listOf(
                ParagraphBlock(id = "b0", text = "Hello"),
                ParagraphBlock(id = "b1", text = "World"),
            ),
        ),
    )
}
