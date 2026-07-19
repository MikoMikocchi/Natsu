package io.mikoshift.natsu.data.repository

import io.mikoshift.natsu.core.domain.repository.ReaderSettingRepository
import io.mikoshift.natsu.core.model.FuriganaMode
import io.mikoshift.natsu.core.model.ReaderSettings
import io.mikoshift.natsu.core.model.ReaderTheme
import io.mikoshift.natsu.data.local.ReaderSettingStore
import io.mikoshift.natsu.data.mapper.toDomain
import io.mikoshift.natsu.data.mapper.toDto
import io.mikoshift.natsu.data.remote.ReaderSettingApi
import io.mikoshift.natsu.data.remote.dto.ReaderSettingUpdateRequest
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderSettingRepositoryImpl
@Inject
constructor(
    private val readerSettingApi: ReaderSettingApi,
    private val readerSettingStore: ReaderSettingStore,
) : ReaderSettingRepository {
    override val settings: Flow<ReaderSettings?> = readerSettingStore.settingsFlow

    override suspend fun refresh(): Result<ReaderSettings> = runCatching {
        readerSettingApi.show()
    }.fold(
        onSuccess = { response ->
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val settings = body.settings.toDomain()
                readerSettingStore.save(settings)
                Result.success(settings)
            } else {
                Result.failure(IOException(response.message()))
            }
        },
        onFailure = { throwable -> Result.failure(throwable) },
    )

    override suspend fun update(
        fontSizeSp: Double?,
        lineSpacingMultiplier: Double?,
        theme: ReaderTheme?,
        furiganaMode: FuriganaMode?,
    ): Result<ReaderSettings> {
        val current = readerSettingStore.getSettings()
        val updatedAtMs = System.currentTimeMillis()
        val request =
            ReaderSettingUpdateRequest(
                fontSizeSp = fontSizeSp,
                lineSpacingMultiplier = lineSpacingMultiplier,
                theme = theme?.toDto(),
                furiganaMode = furiganaMode?.toDto(),
                updatedAtMs = updatedAtMs,
            )

        return runCatching {
            readerSettingApi.update(request)
        }.fold(
            onSuccess = { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    val settings = body.settings.toDomain()
                    readerSettingStore.save(settings)
                    Result.success(settings)
                } else {
                    current?.let { readerSettingStore.save(it) }
                    Result.failure(IOException(response.message()))
                }
            },
            onFailure = { throwable -> Result.failure(throwable) },
        )
    }
}
