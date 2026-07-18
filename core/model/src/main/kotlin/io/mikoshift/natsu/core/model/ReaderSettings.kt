package io.mikoshift.natsu.core.model

enum class ReaderTheme {
    LIGHT,
    DARK,
    SEPIA,
}

enum class FuriganaMode {
    OFF,
    ALWAYS,
}

data class ReaderSettings(
    val fontSizeSp: Double,
    val lineSpacingMultiplier: Double,
    val theme: ReaderTheme,
    val furiganaMode: FuriganaMode,
    val updatedAtMs: Long,
)
