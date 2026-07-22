package io.mikoshift.natsudroid.data.repository

import io.mikoshift.natsudroid.core.domain.repository.ReaderSettingRepository
import io.mikoshift.natsudroid.core.model.FuriganaMode
import io.mikoshift.natsudroid.core.model.ReaderSettings
import io.mikoshift.natsudroid.core.model.ReaderTheme
import io.mikoshift.natsudroid.data.local.ReaderSettingStore
import io.mikoshift.natsudroid.data.mapper.toDomain
import io.mikoshift.natsudroid.data.mapper.toDto
import io.mikoshift.natsudroid.data.remote.ReaderSettingApi
import io.mikoshift.natsudroid.data.remote.dto.ReaderSettingUpdateRequest
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
