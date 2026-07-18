package io.mikoshift.natsu.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MatchKindDto {
    @SerialName("DIRECT") DIRECT,
    @SerialName("DEINFLECTION") DEINFLECTION,
}

@Serializable
data class DictionaryResponse(
    val id: String,
    @SerialName("catalog_id") val catalogId: String,
    val title: String,
    val revision: String,
    @SerialName("term_count") val termCount: Int,
    val enabled: Boolean,
)

@Serializable
data class PaginationResponse(
    val page: Int,
    @SerialName("per_page") val perPage: Int,
    @SerialName("total_count") val totalCount: Int,
    @SerialName("total_pages") val totalPages: Int,
)

@Serializable
data class DictionaryIndexResponse(
    val dictionaries: List<DictionaryResponse>,
    val pagination: PaginationResponse,
    @SerialName("server_time_ms") val serverTimeMs: Long,
)

@Serializable
data class DictionarySenseResponse(
    val definitions: List<String>,
    @SerialName("parts_of_speech") val partsOfSpeech: List<String>,
    @SerialName("dictionary_title") val dictionaryTitle: String,
)

@Serializable
data class DictionaryLookupResultResponse(
    val word: String,
    val reading: String,
    @SerialName("match_kind") val matchKind: MatchKindDto,
    @SerialName("rule_name") val ruleName: String? = null,
    @SerialName("rule_description") val ruleDescription: String? = null,
    val senses: List<DictionarySenseResponse>,
)

@Serializable
data class DictionaryLookupResponse(
    val data: List<DictionaryLookupResultResponse>,
    @SerialName("server_time_ms") val serverTimeMs: Long,
)
