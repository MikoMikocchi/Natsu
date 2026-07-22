package io.mikoshift.natsudroid.ui.reader.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.mikoshift.natsudroid.core.model.content.BlockquoteBlock
import io.mikoshift.natsudroid.core.model.content.DividerBlock
import io.mikoshift.natsudroid.core.model.content.HeadingBlock
import io.mikoshift.natsudroid.core.model.content.ImageBlock
import io.mikoshift.natsudroid.core.model.content.ListItemBlock
import io.mikoshift.natsudroid.core.model.content.ParagraphBlock
import io.mikoshift.natsudroid.ui.reader.ReaderBlockItem
import io.mikoshift.natsudroid.ui.reader.SelectedWord

@Composable
fun BlockContent(
    item: ReaderBlockItem,
    modifier: Modifier = Modifier,
    bodyStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    selectedWord: SelectedWord? = null,
    onWordTap: ((String, String, Int) -> Unit)? = null,
) {
    val isSelected = selectedWord?.blockId == item.id
    val wordTapHandler = onWordTap?.let { handler ->
        { charOffset: Int ->
            when (val block = item.block) {
                is ParagraphBlock -> handler(item.id, block.text, charOffset)
                is HeadingBlock -> handler(item.id, block.text, charOffset)
                is BlockquoteBlock -> handler(item.id, block.text, charOffset)
                is ListItemBlock -> handler(item.id, block.text, charOffset)
                else -> Unit
            }
        }
    }

    when (val block = item.block) {
        is ParagraphBlock -> MarkedText(
            text = block.text,
            marks = block.marks,
            modifier = modifier.padding(vertical = 4.dp),
            style = bodyStyle,
            selectedRange = if (isSelected) selectedWord.range else null,
            onWordTap = wordTapHandler,
        )
        is HeadingBlock -> MarkedText(
            text = block.text,
            marks = block.marks,
            modifier = modifier.padding(vertical = 8.dp),
            style = headingStyle(block.level, bodyStyle),
            selectedRange = if (isSelected) selectedWord.range else null,
            onWordTap = wordTapHandler,
        )
        is BlockquoteBlock -> MarkedText(
            text = block.text,
            marks = block.marks,
            modifier = modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
            style = bodyStyle.copy(
                color = bodyStyle.color.copy(alpha = 0.75f),
            ),
            selectedRange = if (isSelected) selectedWord.range else null,
            onWordTap = wordTapHandler,
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
                style = bodyStyle,
                selectedRange = if (isSelected) selectedWord.range else null,
                onWordTap = wordTapHandler,
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
