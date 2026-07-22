package io.mikoshift.natsudroid.ui.reader.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.mikoshift.natsudroid.core.model.content.TocNode
import io.mikoshift.natsudroid.feature.reader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TocSheet(toc: List<TocNode>, onDismiss: () -> Unit, onSectionSelected: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.reader_table_of_contents),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            LazyColumn {
                items(flattenToc(toc)) { node ->
                    val itemModifier = Modifier.padding(start = (node.depth * 16).dp)
                    ListItem(
                        headlineContent = { Text(node.title) },
                        modifier =
                        if (node.sectionId != null) {
                            itemModifier.clickable {
                                onSectionSelected(node.sectionId)
                                onDismiss()
                            }
                        } else {
                            itemModifier
                        },
                    )
                }
            }
        }
    }
}

private data class FlatTocNode(val title: String, val sectionId: String?, val depth: Int)

private fun flattenToc(nodes: List<TocNode>, depth: Int = 0): List<FlatTocNode> = buildList {
    nodes.forEach { node ->
        if (node.title != null || node.sectionId != null) {
            add(FlatTocNode(title = node.title.orEmpty(), sectionId = node.sectionId, depth = depth))
        }
        addAll(flattenToc(node.children, depth + 1))
    }
}
