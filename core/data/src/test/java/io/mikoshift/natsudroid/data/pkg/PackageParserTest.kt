package io.mikoshift.natsudroid.data.pkg

import io.mikoshift.natsudroid.core.model.content.HeadingBlock
import io.mikoshift.natsudroid.core.model.content.ImageBlock
import io.mikoshift.natsudroid.core.model.content.ParagraphBlock
import io.mikoshift.natsudroid.data.remote.NetworkFactory
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class PackageParserTest {
    private lateinit var parser: PackageParser

    @Before
    fun setUp() {
        val networkFactory =
            NetworkFactory(
                baseUrl = "https://example.com/v1/",
                rootBaseUrl = "https://example.com/",
                isDebugBuild = false,
            )
        parser =
            PackageParser(
                networkFactory = networkFactory,
                packageFileStore = mockk(relaxed = true),
            )
    }

    @Test
    fun parseZip_acceptsSectionBlocksWithoutTypeDiscriminator() {
        val zipFile = File.createTempFile("package-blocks-no-type", ".zip")
        zipFile.writeBytes(PackageTestFixtures.sampleZipBytesWithUntypedBlocks())

        val documentPackage = parser.parseZip(zipFile)

        val blocks = documentPackage.sections["section-0"].orEmpty()
        assertTrue(blocks[0] is HeadingBlock)
        assertEquals("Оглавление", (blocks[0] as HeadingBlock).text)
    }

    @Test
    fun parseZip_acceptsTocNodesWithNullSectionId() {
        val zipFile = File.createTempFile("package-toc-null", ".zip")
        zipFile.writeBytes(PackageTestFixtures.sampleZipBytesWithNullTocSectionId())

        val documentPackage = parser.parseZip(zipFile)

        assertEquals(1, documentPackage.manifest.toc.size)
        assertEquals("1. Введение", documentPackage.manifest.toc.first().title)
        assertEquals(null, documentPackage.manifest.toc.first().sectionId)
    }

    @Test
    fun parseZip_readsManifestSectionsAndBlocks() {
        val zipFile = File.createTempFile("package", ".zip")
        zipFile.writeBytes(PackageTestFixtures.sampleZipBytes())

        val documentPackage = parser.parseZip(zipFile)

        assertEquals(2, documentPackage.manifest.schemaVersion)
        assertEquals("My Book", documentPackage.manifest.title)
        assertEquals(1, documentPackage.manifest.sections.size)
        val blocks = documentPackage.sections["section-0"].orEmpty()
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is ParagraphBlock)
        assertTrue(blocks[1] is HeadingBlock)
        assertTrue(blocks[2] is ImageBlock)
    }
}
