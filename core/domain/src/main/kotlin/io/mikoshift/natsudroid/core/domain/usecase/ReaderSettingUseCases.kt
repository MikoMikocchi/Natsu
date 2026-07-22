package io.mikoshift.natsudroid.core.domain.usecase

import io.mikoshift.natsudroid.core.domain.repository.ReaderSettingRepository
import io.mikoshift.natsudroid.core.model.FuriganaMode
import io.mikoshift.natsudroid.core.model.ReaderSettings
import io.mikoshift.natsudroid.core.model.ReaderTheme
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveReaderSettingsUseCase
@Inject
constructor(private val readerSettingRepository: ReaderSettingRepository) {
    operator fun invoke(): Flow<ReaderSettings?> = readerSettingRepository.settings

    suspend fun refresh(): Result<ReaderSettings> = readerSettingRepository.refresh()
}

class UpdateReaderSettingsUseCase
@Inject
constructor(private val readerSettingRepository: ReaderSettingRepository) {
    suspend operator fun invoke(
        fontSizeSp: Double? = null,
        lineSpacingMultiplier: Double? = null,
        theme: ReaderTheme? = null,
        furiganaMode: FuriganaMode? = null,
    ): Result<ReaderSettings> = readerSettingRepository.update(
        fontSizeSp = fontSizeSp,
        lineSpacingMultiplier = lineSpacingMultiplier,
        theme = theme,
        furiganaMode = furiganaMode,
    )
}
