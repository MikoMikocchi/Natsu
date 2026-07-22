package io.mikoshift.natsudroid.data.mapper

import io.mikoshift.natsudroid.core.model.FuriganaMode
import io.mikoshift.natsudroid.core.model.ReaderSettings
import io.mikoshift.natsudroid.core.model.ReaderTheme
import io.mikoshift.natsudroid.data.remote.dto.FuriganaModeDto
import io.mikoshift.natsudroid.data.remote.dto.ReaderSettingResponse
import io.mikoshift.natsudroid.data.remote.dto.ReaderThemeDto

fun ReaderSettingResponse.toDomain(): ReaderSettings = ReaderSettings(
    fontSizeSp = fontSizeSp,
    lineSpacingMultiplier = lineSpacingMultiplier,
    theme = theme.toDomain(),
    furiganaMode = furiganaMode.toDomain(),
    updatedAtMs = updatedAtMs,
)

fun ReaderThemeDto.toDomain(): ReaderTheme = ReaderTheme.valueOf(name)

fun FuriganaModeDto.toDomain(): FuriganaMode = FuriganaMode.valueOf(name)

fun ReaderTheme.toDto(): ReaderThemeDto = ReaderThemeDto.valueOf(name)

fun FuriganaMode.toDto(): FuriganaModeDto = FuriganaModeDto.valueOf(name)
