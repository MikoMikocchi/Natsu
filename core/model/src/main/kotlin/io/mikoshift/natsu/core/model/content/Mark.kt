package io.mikoshift.natsu.core.model.content

enum class MarkType {
    BOLD,
    ITALIC,
}

data class Mark(
    val type: MarkType,
    val start: Int,
    val end: Int,
)
