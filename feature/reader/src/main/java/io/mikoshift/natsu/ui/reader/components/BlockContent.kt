package io.mikoshift.natsu.ui.reader.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
    bodyStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    onLongPressText: ((String) -> Unit)? = null,
) {
    val longPressModifier = if (onLongPressText != null) {
        modifier.combinedClickable(
            onClick = {},
            onLongClick = {
                when (val block = item.block) {
                    is ParagraphBlock -> onLongPressText(block.text)
                    is HeadingBlock -> onLongPressText(block.text)
                    is BlockquoteBlock -> onLongPressText(block.text)
                    is ListItemBlock -> onLongPressText(block.text)
                    else -> Unit
                }
            },
        )
    } else {
        modifier
    }

    when (val block = item.block) {
        is ParagraphBlock -> MarkedText(
            text = block.text,
            marks = block.marks,
            modifier = longPressModifier.padding(vertical = 4.dp),
            style = bodyStyle,
        )
        is HeadingBlock -> MarkedText(
            text = block.text,
            marks = block.marks,
            modifier = longPressModifier.padding(vertical = 8.dp),
            style = headingStyle(block.level, bodyStyle),
        )
        is BlockquoteBlock -> MarkedText(
            text = block.text,
            marks = block.marks,
            modifier = longPressModifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
            style = bodyStyle.copy(
                color = bodyStyle.color.copy(alpha = 0.75f),
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
                modifier = longPressModifier.padding(start = (block.level * 16).dp, top = 2.dp, bottom = 2.dp),
                style = bodyStyle,
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
private fun headingStyle(
    level: Int,
    bodyStyle: androidx.compose.ui.text.TextStyle,
): androidx.compose.ui.text.TextStyle = when (level) {
    1 -> bodyStyle.copy(fontSize = bodyStyle.fontSize * 1.6f)
    2 -> bodyStyle.copy(fontSize = bodyStyle.fontSize * 1.4f)
    3 -> bodyStyle.copy(fontSize = bodyStyle.fontSize * 1.2f)
    else -> bodyStyle.copy(fontSize = bodyStyle.fontSize * 1.1f)
}
