package io.mikoshift.natsudroid.data.repository

import io.mikoshift.natsudroid.core.domain.repository.DictionaryRepository
import io.mikoshift.natsudroid.core.model.DictionaryLookupResult
import io.mikoshift.natsudroid.core.model.DictionaryPage
import io.mikoshift.natsudroid.data.mapper.toDomain
import io.mikoshift.natsudroid.data.remote.DictionaryApi
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictionaryRepositoryImpl
@Inject
constructor(private val dictionaryApi: DictionaryApi) :
    DictionaryRepository {
    override suspend fun listDictionaries(page: Int, perPage: Int): Result<DictionaryPage> =
        runCatching { dictionaryApi.index(page = page, perPage = perPage) }.fold(
            onSuccess = { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body.toDomain())
                } else {
                    Result.failure(IOException(response.message()))
                }
            },
            onFailure = { throwable -> Result.failure(throwable) },
        )

    override suspend fun toggleDictionary(id: String): Result<Unit> = runCatching { dictionaryApi.toggle(id) }.fold(
        onSuccess = { response ->
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException(response.message()))
            }
        },
        onFailure = { throwable -> Result.failure(throwable) },
    )

    override suspend fun lookup(query: String): Result<List<DictionaryLookupResult>> = if (query.isBlank()) {
        Result.success(emptyList())
    } else {
        runCatching { dictionaryApi.lookup(query) }.fold(
            onSuccess = { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body.data.map { it.toDomain() })
                } else {
                    Result.failure(IOException(response.message()))
                }
            },
            onFailure = { throwable -> Result.failure(throwable) },
        )
    }
}
