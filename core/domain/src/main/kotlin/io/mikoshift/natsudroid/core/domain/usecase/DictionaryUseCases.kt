package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.DictionaryRepository
import io.mikoshift.natsudroid.core.model.DictionaryLookupResult
import io.mikoshift.natsudroid.core.model.DictionaryPage
import javax.inject.Inject

class ListDictionariesUseCase
@Inject
constructor(private val dictionaryRepository: DictionaryRepository) {
    suspend operator fun invoke(page: Int = 1, perPage: Int = 50): Result<DictionaryPage> =
        dictionaryRepository.listDictionaries(page = page, perPage = perPage)
}

class ToggleDictionaryUseCase
@Inject
constructor(private val dictionaryRepository: DictionaryRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = dictionaryRepository.toggleDictionary(id)
}

class LookupWordUseCase
@Inject
constructor(private val dictionaryRepository: DictionaryRepository) {
    suspend operator fun invoke(query: String): Result<List<DictionaryLookupResult>> =
        dictionaryRepository.lookup(query.trim())
}
