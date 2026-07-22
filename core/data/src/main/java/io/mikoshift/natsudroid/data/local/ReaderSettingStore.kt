package io.mikoshift.natsudroid.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.mikoshift.natsudroid.core.model.FuriganaMode
import io.mikoshift.natsudroid.core.model.ReaderSettings
import io.mikoshift.natsudroid.core.model.ReaderTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderSettingStore
@Inject
constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settingsFlow = MutableStateFlow(readSettings())
    val settingsFlow: StateFlow<ReaderSettings?> = _settingsFlow.asStateFlow()

    fun getSettings(): ReaderSettings? = _settingsFlow.value

    fun save(settings: ReaderSettings) {
        prefs
            .edit()
            .putFloat(KEY_FONT_SIZE_SP, settings.fontSizeSp.toFloat())
            .putFloat(KEY_LINE_SPACING, settings.lineSpacingMultiplier.toFloat())
            .putString(KEY_THEME, settings.theme.name)
            .putString(KEY_FURIGANA_MODE, settings.furiganaMode.name)
            .putLong(KEY_UPDATED_AT_MS, settings.updatedAtMs)
            .apply()
        _settingsFlow.value = settings
    }

    fun clear() {
        prefs.edit().clear().apply()
        _settingsFlow.value = null
    }

    private fun readSettings(): ReaderSettings? {
        if (!prefs.contains(KEY_UPDATED_AT_MS)) return null
        return ReaderSettings(
            fontSizeSp = prefs.getFloat(KEY_FONT_SIZE_SP, DEFAULT_FONT_SIZE_SP).toDouble(),
            lineSpacingMultiplier = prefs.getFloat(KEY_LINE_SPACING, DEFAULT_LINE_SPACING).toDouble(),
            theme = ReaderTheme.valueOf(prefs.getString(KEY_THEME, ReaderTheme.LIGHT.name)!!),
            furiganaMode = FuriganaMode.valueOf(prefs.getString(KEY_FURIGANA_MODE, FuriganaMode.OFF.name)!!),
            updatedAtMs = prefs.getLong(KEY_UPDATED_AT_MS, 0L),
        )
    }

    private companion object {
        const val PREFS_NAME = "natsudroid_reader_settings"
        const val KEY_FONT_SIZE_SP = "font_size_sp"
        const val KEY_LINE_SPACING = "line_spacing_multiplier"
        const val KEY_THEME = "theme"
        const val KEY_FURIGANA_MODE = "furigana_mode"
        const val KEY_UPDATED_AT_MS = "updated_at_ms"
        const val DEFAULT_FONT_SIZE_SP = 16f
        const val DEFAULT_LINE_SPACING = 1.8f
    }
}
