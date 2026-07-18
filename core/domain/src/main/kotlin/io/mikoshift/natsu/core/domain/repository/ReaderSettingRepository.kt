package io.mikoshift.natsu.core.domain.repository

import io.mikoshift.natsu.core.model.FuriganaMode
import io.mikoshift.natsu.core.model.ReaderSettings
import io.mikoshift.natsu.core.model.ReaderTheme
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
