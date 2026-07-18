package io.mikoshift.natsu.ui.reader

fun extractLookupQuery(text: String): String? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null

    val cjkMatch = Regex("""[\p{Script=Han}\p{Script=Hiragana}\p{Script=Katakana}]+""")
        .find(trimmed)
        ?.value
    if (!cjkMatch.isNullOrBlank()) return cjkMatch

    return trimmed.split(Regex("""\s+""")).firstOrNull()
        ?.trim('.', ';', ':', '!', '?', '"', '\'', '(', ')', '[', ']')
        ?.takeIf { it.isNotBlank() }
}
