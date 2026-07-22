package io.mikoshift.natsudroid.ui.reader

import androidx.compose.ui.graphics.Color
import io.mikoshift.natsudroid.core.model.ReaderTheme

data class ReaderThemeColors(val background: Color, val onBackground: Color)

fun ReaderTheme.toColors(): ReaderThemeColors = when (this) {
    ReaderTheme.LIGHT ->
        ReaderThemeColors(
            background = Color(0xFFFFFBFE),
            onBackground = Color(0xFF1C1B1F),
        )
    ReaderTheme.DARK ->
        ReaderThemeColors(
            background = Color(0xFF1C1B1F),
            onBackground = Color(0xFFE6E1E5),
        )
    ReaderTheme.SEPIA ->
        ReaderThemeColors(
            background = Color(0xFFF4ECD8),
            onBackground = Color(0xFF3E2723),
        )
}
