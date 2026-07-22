package io.mikoshift.natsudroid.data.repository

import io.mikoshift.natsudroid.data.remote.DictionaryApi
import io.mikoshift.natsudroid.data.remote.dto.DictionaryIndexResponse
import io.mikoshift.natsudroid.data.remote.dto.DictionaryLookupResponse
import io.mikoshift.natsudroid.data.remote.dto.DictionaryLookupResultResponse
import io.mikoshift.natsudroid.data.remote.dto.DictionaryResponse
import io.mikoshift.natsudroid.data.remote.dto.DictionarySenseResponse
import io.mikoshift.natsudroid.data.remote.dto.MatchKindDto
import io.mikoshift.natsudroid.data.remote.dto.PaginationResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class DictionaryRepositoryImplTest {
    private lateinit var dictionaryApi: DictionaryApi
    private lateinit var repository: DictionaryRepositoryImpl

    @Before
    fun setUp() {
        dictionaryApi = mockk()
        repository = DictionaryRepositoryImpl(dictionaryApi = dictionaryApi)
    }

    @Test
    fun lookup_success_returnsResults() = runTest {
        coEvery { dictionaryApi.lookup("本") } returns
            Response.success(
                DictionaryLookupResponse(
                    data =
                    listOf(
                        DictionaryLookupResultResponse(
                            word = "本",
                            reading = "ほん",
                            matchKind = MatchKindDto.DIRECT,
                            senses =
                            listOf(
                                DictionarySenseResponse(
                                    definitions = listOf("book"),
                                    partsOfSpeech = listOf("noun"),
                                    dictionaryTitle = "JMdict",
                                ),
                            ),
                        ),
                    ),
                    serverTimeMs = 100L,
                ),
            )

        val result = repository.lookup("本")

        assertTrue(result.isSuccess)
        assertEquals("本", result.getOrNull()?.first()?.word)
    }

    @Test
    fun listDictionaries_success_returnsPage() = runTest {
        coEvery { dictionaryApi.index(page = 1, perPage = 50) } returns
            Response.success(
                DictionaryIndexResponse(
                    dictionaries =
                    listOf(
                        DictionaryResponse(
                            id = "dict-1",
                            catalogId = "jmdict",
                            title = "JMdict",
                            revision = "1",
                            termCount = 100,
                            enabled = true,
                        ),
                    ),
                    pagination =
                    PaginationResponse(
                        page = 1,
                        perPage = 50,
                        totalCount = 1,
                        totalPages = 1,
                    ),
                    serverTimeMs = 100L,
                ),
            )

        val result = repository.listDictionaries()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.dictionaries?.size)
    }
}
