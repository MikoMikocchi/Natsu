package io.mikoshift.natsu.ui.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.mikoshift.natsu.core.model.DictionaryLookupResult
import io.mikoshift.natsu.feature.reader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryLookupSheet(
    query: String,
    isLoading: Boolean,
    results: List<DictionaryLookupResult>,
    errorMessage: String?,
    suggestEnableDictionary: Boolean,
    onDismiss: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.dictionary_lookup_title, query),
                style = MaterialTheme.typography.titleLarge,
            )

            when {
                isLoading -> CircularProgressIndicator()
                errorMessage != null -> Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                )
                results.isEmpty() ->
                    Text(
                        text = stringResource(
                            if (suggestEnableDictionary) {
                                R.string.dictionary_lookup_no_dictionaries
                            } else {
                                R.string.dictionary_lookup_empty
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                else -> results.forEach { result ->
                    DictionaryLookupResultItem(result)
                }
            }
        }
    }
}

@Composable
private fun DictionaryLookupResultItem(result: DictionaryLookupResult) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = buildString {
                append(result.word)
                if (result.reading.isNotBlank()) {
                    append(" (")
                    append(result.reading)
                    append(")")
                }
            },
            style = MaterialTheme.typography.titleMedium,
        )
        result.senses.forEach { sense ->
            val definition = sense.definitions.joinToString("; ")
            val pos = sense.partsOfSpeech.joinToString(", ")
            Text(
                text = buildString {
                    if (pos.isNotBlank()) append("[$pos] ")
                    append(definition)
                    if (sense.dictionaryTitle.isNotBlank()) {
                        append(" — ")
                        append(sense.dictionaryTitle)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
