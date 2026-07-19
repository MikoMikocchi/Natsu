package io.mikoshift.natsu.core.model

data class Dictionary(
    val id: String,
    val catalogId: String,
    val title: String,
    val revision: String,
    val termCount: Int,
    val enabled: Boolean,
)

data class DictionaryPagination(val page: Int, val perPage: Int, val totalCount: Int, val totalPages: Int)

data class DictionaryPage(val dictionaries: List<Dictionary>, val pagination: DictionaryPagination)

enum class MatchKind {
    DIRECT,
    DEINFLECTION,
}

data class DictionarySense(val definitions: List<String>, val partsOfSpeech: List<String>, val dictionaryTitle: String)

data class DictionaryLookupResult(
    val word: String,
    val reading: String,
    val matchKind: MatchKind,
    val ruleName: String?,
    val ruleDescription: String?,
    val senses: List<DictionarySense>,
)
