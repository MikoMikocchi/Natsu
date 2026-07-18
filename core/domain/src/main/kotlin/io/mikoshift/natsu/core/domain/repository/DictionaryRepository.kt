package io.mikoshift.natsu.core.domain.repository

import io.mikoshift.natsu.core.model.DictionaryLookupResult
import io.mikoshift.natsu.core.model.DictionaryPage

interface DictionaryRepository {
    suspend fun listDictionaries(page: Int = 1, perPage: Int = 50): Result<DictionaryPage>

    suspend fun toggleDictionary(id: String): Result<Unit>

    suspend fun lookup(query: String): Result<List<DictionaryLookupResult>>
}
