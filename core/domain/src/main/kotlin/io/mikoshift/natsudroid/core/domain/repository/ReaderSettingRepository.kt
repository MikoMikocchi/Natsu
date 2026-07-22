package io.mikoshift.natsudroid.core.domain.repository

import io.mikoshift.natsudroid.core.model.FuriganaMode
import io.mikoshift.natsudroid.core.model.ReaderSettings
import io.mikoshift.natsudroid.core.model.ReaderTheme
import kotlinx.coroutines.flow.Flow

interface ReaderSettingRepository {
    val settings: Flow<ReaderSettings?>

    suspend fun refresh(): Result<ReaderSettings>

    suspend fun update(
        fontSizeSp: Double? = null,
        lineSpacingMultiplier: Double? = null,
        theme: ReaderTheme? = null,
        furiganaMode: FuriganaMode? = null,
    ): Result<ReaderSettings>
}
