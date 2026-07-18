package io.mikoshift.natsu.ui.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.mikoshift.natsu.core.model.FuriganaMode
import io.mikoshift.natsu.core.model.ReaderSettings
import io.mikoshift.natsu.core.model.ReaderTheme
import io.mikoshift.natsu.feature.reader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onDismiss: () -> Unit,
    onFontSizeChange: (Double) -> Unit,
    onLineSpacingChange: (Double) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onFuriganaModeChange: (FuriganaMode) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.reader_settings_title),
                style = MaterialTheme.typography.titleLarge,
            )

            Text(text = stringResource(R.string.reader_font_size))
            Slider(
                value = settings.fontSizeSp.toFloat(),
                onValueChange = { onFontSizeChange(it.toDouble()) },
                valueRange = 10f..40f,
            )

            Text(text = stringResource(R.string.reader_line_spacing))
            Slider(
                value = settings.lineSpacingMultiplier.toFloat(),
                onValueChange = { onLineSpacingChange(it.toDouble()) },
                valueRange = 1f..3f,
            )

            Text(text = stringResource(R.string.reader_theme))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReaderTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = settings.theme == theme,
                        onClick = { onThemeChange(theme) },
                        label = { Text(theme.label()) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.reader_furigana))
                Switch(
                    checked = settings.furiganaMode == FuriganaMode.ALWAYS,
                    onCheckedChange = { enabled ->
                        onFuriganaModeChange(if (enabled) FuriganaMode.ALWAYS else FuriganaMode.OFF)
                    },
                )
            }
        }
    }
}

@Composable
private fun ReaderTheme.label(): String = when (this) {
    ReaderTheme.LIGHT -> stringResource(R.string.reader_theme_light)
    ReaderTheme.DARK -> stringResource(R.string.reader_theme_dark)
    ReaderTheme.SEPIA -> stringResource(R.string.reader_theme_sepia)
}
