package io.mikoshift.natsu.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ReaderThemeDto {
    @SerialName("LIGHT") LIGHT,
    @SerialName("DARK") DARK,
    @SerialName("SEPIA") SEPIA,
}

@Serializable
enum class FuriganaModeDto {
    @SerialName("OFF") OFF,
    @SerialName("ALWAYS") ALWAYS,
}

@Serializable
data class ReaderSettingResponse(
    @SerialName("font_size_sp") val fontSizeSp: Double,
    @SerialName("line_spacing_multiplier") val lineSpacingMultiplier: Double,
    val theme: ReaderThemeDto,
    @SerialName("furigana_mode") val furiganaMode: FuriganaModeDto,
    @SerialName("updated_at_ms") val updatedAtMs: Long,
)

@Serializable
data class ReaderSettingShowResponse(
    val settings: ReaderSettingResponse,
    @SerialName("server_time_ms") val serverTimeMs: Long,
)

@Serializable
data class ReaderSettingUpdateRequest(
    @SerialName("font_size_sp") val fontSizeSp: Double? = null,
    @SerialName("line_spacing_multiplier") val lineSpacingMultiplier: Double? = null,
    val theme: ReaderThemeDto? = null,
    @SerialName("furigana_mode") val furiganaMode: FuriganaModeDto? = null,
    @SerialName("updated_at_ms") val updatedAtMs: Long,
)
