package io.mikoshift.natsu.ui.reader.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.mikoshift.natsu.core.model.content.BlockquoteBlock
import io.mikoshift.natsu.core.model.content.DividerBlock
import io.mikoshift.natsu.core.model.content.HeadingBlock
import io.mikoshift.natsu.core.model.content.ImageBlock
import io.mikoshift.natsu.core.model.content.ListItemBlock
import io.mikoshift.natsu.core.model.content.ParagraphBlock
import io.mikoshift.natsu.ui.reader.ReaderBlockItem

@Composable
fun BlockContent(
    item: ReaderBlockItem,
    modifier: Modifier = Modifier,
) {
    when (val block = item.block) {
        is ParagraphBlock -> MarkedText(
            text = block.text,
            marks = block.marks,
            modifier = modifier.padding(vertical = 4.dp),
        )
        is HeadingBlock -> MarkedText(
            text = block.text,
            marks = block.marks,
            modifier = modifier.padding(vertical = 8.dp),
            style = headingStyle(block.level),
        )
        is BlockquoteBlock -> MarkedText(
            text = block.text,
            marks = block.marks,
            modifier = modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        is ListItemBlock -> {
            val prefix = if (block.ordered) "${block.level + 1}. " else "• "
            MarkedText(
                text = prefix + block.text,
                marks = block.marks.map { mark ->
                    mark.copy(
                        start = mark.start + prefix.length,
                        end = mark.end + prefix.length,
                    )
                },
                modifier = modifier.padding(start = (block.level * 16).dp, top = 2.dp, bottom = 2.dp),
            )
        }
        is ImageBlock -> PackageImage(
            assetPath = item.assetPath,
            alt = block.alt,
            modifier = modifier,
        )
        is DividerBlock -> HorizontalDivider(modifier = modifier.padding(vertical = 12.dp))
    }
}

@Composable
private fun headingStyle(level: Int): androidx.compose.ui.text.TextStyle = when (level) {
    1 -> MaterialTheme.typography.headlineMedium
    2 -> MaterialTheme.typography.headlineSmall
    3 -> MaterialTheme.typography.titleLarge
    else -> MaterialTheme.typography.titleMedium
}
