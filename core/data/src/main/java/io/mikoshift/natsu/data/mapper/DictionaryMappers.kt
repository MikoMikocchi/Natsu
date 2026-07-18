package io.mikoshift.natsu.data.mapper

import io.mikoshift.natsu.core.model.Dictionary
import io.mikoshift.natsu.core.model.DictionaryLookupResult
import io.mikoshift.natsu.core.model.DictionaryPage
import io.mikoshift.natsu.core.model.DictionaryPagination
import io.mikoshift.natsu.core.model.DictionarySense
import io.mikoshift.natsu.core.model.MatchKind
import io.mikoshift.natsu.data.remote.dto.DictionaryIndexResponse
import io.mikoshift.natsu.data.remote.dto.DictionaryLookupResultResponse
import io.mikoshift.natsu.data.remote.dto.DictionaryResponse
import io.mikoshift.natsu.data.remote.dto.DictionarySenseResponse
import io.mikoshift.natsu.data.remote.dto.MatchKindDto
import io.mikoshift.natsu.data.remote.dto.PaginationResponse

fun DictionaryIndexResponse.toDomain(): DictionaryPage = DictionaryPage(
    dictionaries = dictionaries.map { it.toDomain() },
    pagination = pagination.toDomain(),
)

fun DictionaryResponse.toDomain(): Dictionary = Dictionary(
    id = id,
    catalogId = catalogId,
    title = title,
    revision = revision,
    termCount = termCount,
    enabled = enabled,
)

fun PaginationResponse.toDomain(): DictionaryPagination = DictionaryPagination(
    page = page,
    perPage = perPage,
    totalCount = totalCount,
    totalPages = totalPages,
)

fun DictionaryLookupResultResponse.toDomain(): DictionaryLookupResult = DictionaryLookupResult(
    word = word,
    reading = reading,
    matchKind = matchKind.toDomain(),
    ruleName = ruleName,
    ruleDescription = ruleDescription,
    senses = senses.map { it.toDomain() },
)

fun DictionarySenseResponse.toDomain(): DictionarySense = DictionarySense(
    definitions = definitions,
    partsOfSpeech = partsOfSpeech,
    dictionaryTitle = dictionaryTitle,
)

fun MatchKindDto.toDomain(): MatchKind = MatchKind.valueOf(name)
