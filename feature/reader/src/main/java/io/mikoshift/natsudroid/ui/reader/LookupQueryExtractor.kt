package io.mikoshift.natsudroid.ui.reader

private val CjkChar = Regex("""[\p{Script=Han}\p{Script=Hiragana}\p{Script=Katakana}]""")
private val WordChar = Regex("""[\p{L}\p{N}'-]""")

fun extractWordAtOffset(text: String, offset: Int): IntRange? {
    if (text.isEmpty()) return null

    val index = offset.coerceIn(0, text.lastIndex)
    val resolvedIndex = resolveWordIndex(text, index) ?: return null

    return if (isCjk(text, resolvedIndex)) {
        expandCjkRange(text, resolvedIndex)
    } else {
        expandWordRange(text, resolvedIndex)
    }
}

private fun resolveWordIndex(text: String, index: Int): Int? {
    if (isWordLike(text, index)) return index

    val left = (index downTo 0).firstOrNull { isWordLike(text, it) }
    val right = (index until text.length).firstOrNull { isWordLike(text, it) }
    return when {
        left == null && right == null -> null
        left == null -> right
        right == null -> left
        index - left <= right - index -> left
        else -> right
    }
}

private fun expandCjkRange(text: String, index: Int): IntRange {
    var start = index
    var end = index
    while (start > 0 && isCjk(text, start - 1)) start--
    while (end < text.lastIndex && isCjk(text, end + 1)) end++
    return start..end
}

private fun expandWordRange(text: String, index: Int): IntRange {
    var start = index
    var end = index
    while (start > 0 && isWordChar(text, start - 1)) start--
    while (end < text.lastIndex && isWordChar(text, end + 1)) end++
    return start..end
}

private fun isWordLike(text: String, index: Int): Boolean = isCjk(text, index) || isWordChar(text, index)

private fun isCjk(text: String, index: Int): Boolean = CjkChar.matches(text[index].toString())

private fun isWordChar(text: String, index: Int): Boolean = WordChar.matches(text[index].toString())

fun extractLookupQuery(text: String): String? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null

    val cjkMatch =
        Regex("""[\p{Script=Han}\p{Script=Hiragana}\p{Script=Katakana}]+""")
            .find(trimmed)
            ?.value
    if (!cjkMatch.isNullOrBlank()) return cjkMatch

    return trimmed
        .split(Regex("""\s+"""))
        .firstOrNull()
        ?.trim('.', ';', ':', '!', '?', '"', '\'', '(', ')', '[', ']')
        ?.takeIf { it.isNotBlank() }
}
