package io.mikoshift.natsu.ui.reader.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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
) {
    Text(
        text = buildMarkedAnnotatedString(text, marks),
        modifier = modifier,
        style = style,
    )
}

internal fun buildMarkedAnnotatedString(text: String, marks: List<Mark>): AnnotatedString =
    buildAnnotatedString {
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
    }
