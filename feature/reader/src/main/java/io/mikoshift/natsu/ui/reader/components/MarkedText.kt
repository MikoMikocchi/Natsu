package io.mikoshift.natsu.ui.reader.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import io.mikoshift.natsu.core.model.content.Mark
import io.mikoshift.natsu.core.model.content.MarkType

@Composable
fun MarkedText(
    text: String,
    marks: List<Mark>,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    selectedRange: IntRange? = null,
    onWordTap: ((Int) -> Unit)? = null,
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val selectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)

    Text(
        text = buildMarkedAnnotatedString(
            text = text,
            marks = marks,
            selectedRange = selectedRange,
            selectionColor = selectionColor,
        ),
        modifier = modifier.then(
            if (onWordTap != null) {
                Modifier.pointerInput(text, onWordTap) {
                    detectTapGestures { position ->
                        val layout = textLayoutResult ?: return@detectTapGestures
                        onWordTap(layout.getOffsetForPosition(position))
                    }
                }
            } else {
                Modifier
            },
        ),
        onTextLayout = { textLayoutResult = it },
        style = style,
    )
}

internal fun buildMarkedAnnotatedString(
    text: String,
    marks: List<Mark>,
    selectedRange: IntRange? = null,
    selectionColor: Color? = null,
): AnnotatedString = buildAnnotatedString {
    append(text)
    marks.forEach { mark ->
        val start = mark.start.coerceIn(0, text.length)
        val end = mark.end.coerceIn(start, text.length)
        if (start >= end) return@forEach
        when (mark.type) {
            MarkType.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
            MarkType.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
        }
    }
    if (selectedRange != null && selectionColor != null) {
        val start = selectedRange.first.coerceIn(0, text.length)
        val end = (selectedRange.last + 1).coerceIn(start, text.length)
        if (start < end) {
            addStyle(SpanStyle(background = selectionColor), start, end)
        }
    }
}
