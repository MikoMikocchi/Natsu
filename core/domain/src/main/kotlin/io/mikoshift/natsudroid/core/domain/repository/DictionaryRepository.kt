package io.mikoshift.natsudroid.core.domain.repository

import io.mikoshift.natsudroid.core.model.DictionaryLookupResult
import io.mikoshift.natsudroid.core.model.DictionaryPage

interface DictionaryRepository {
    suspend fun listDictionaries(page: Int = 1, perPage: Int = 50): Result<DictionaryPage>

    suspend fun toggleDictionary(id: String): Result<Unit>

    suspend fun lookup(query: String): Result<List<DictionaryLookupResult>>
}
