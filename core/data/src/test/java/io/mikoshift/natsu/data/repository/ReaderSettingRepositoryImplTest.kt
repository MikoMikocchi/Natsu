package io.mikoshift.natsu.data.repository

import io.mikoshift.natsu.data.local.ReaderSettingStore
import io.mikoshift.natsu.data.remote.ReaderSettingApi
import io.mikoshift.natsu.data.remote.dto.FuriganaModeDto
import io.mikoshift.natsu.data.remote.dto.ReaderSettingResponse
import io.mikoshift.natsu.data.remote.dto.ReaderSettingShowResponse
import io.mikoshift.natsu.data.remote.dto.ReaderThemeDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ReaderSettingRepositoryImplTest {
    private lateinit var readerSettingApi: ReaderSettingApi
    private lateinit var readerSettingStore: ReaderSettingStore
    private lateinit var repository: ReaderSettingRepositoryImpl

    @Before
    fun setUp() {
        readerSettingApi = mockk()
        readerSettingStore = mockk(relaxed = true)
        repository =
            ReaderSettingRepositoryImpl(
                readerSettingApi = readerSettingApi,
                readerSettingStore = readerSettingStore,
            )
    }

    @Test
    fun refresh_success_savesSettings() = runTest {
        coEvery { readerSettingApi.show() } returns Response.success(sampleResponse())

        val result = repository.refresh()

        assertTrue(result.isSuccess)
        assertEquals(16.0, result.getOrNull()?.fontSizeSp)
    }

    private fun sampleResponse() = ReaderSettingShowResponse(
        settings =
        ReaderSettingResponse(
            fontSizeSp = 16.0,
            lineSpacingMultiplier = 1.8,
            theme = ReaderThemeDto.LIGHT,
            furiganaMode = FuriganaModeDto.OFF,
            updatedAtMs = 100L,
        ),
        serverTimeMs = 100L,
    )
}
