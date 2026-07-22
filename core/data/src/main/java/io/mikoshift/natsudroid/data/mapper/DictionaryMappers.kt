package io.mikoshift.natsudroid.data.mapper

import io.mikoshift.natsudroid.core.model.Dictionary
import io.mikoshift.natsudroid.core.model.DictionaryLookupResult
import io.mikoshift.natsudroid.core.model.DictionaryPage
import io.mikoshift.natsudroid.core.model.DictionaryPagination
import io.mikoshift.natsudroid.core.model.DictionarySense
import io.mikoshift.natsudroid.core.model.MatchKind
import io.mikoshift.natsudroid.data.remote.dto.DictionaryIndexResponse
import io.mikoshift.natsudroid.data.remote.dto.DictionaryLookupResultResponse
import io.mikoshift.natsudroid.data.remote.dto.DictionaryResponse
import io.mikoshift.natsudroid.data.remote.dto.DictionarySenseResponse
import io.mikoshift.natsudroid.data.remote.dto.MatchKindDto
import io.mikoshift.natsudroid.data.remote.dto.PaginationResponse

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
