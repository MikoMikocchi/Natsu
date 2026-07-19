package io.mikoshift.natsu.core.model.content

data class DocumentPackage(val manifest: PackageManifest, val sections: Map<String, List<Block>>) {
    val sectionOrder: List<String> = manifest.sections.map { it.id }

    fun blocksInReadingOrder(): List<ReaderBlock> = buildList {
        for (sectionId in sectionOrder) {
            val blocks = sections[sectionId].orEmpty()
            blocks.forEachIndexed { index, block ->
                add(
                    ReaderBlock(
                        sectionId = sectionId,
                        blockIndex = index,
                        block = block,
                    ),
                )
            }
        }
    }
}

data class ReaderBlock(val sectionId: String, val blockIndex: Int, val block: Block)
