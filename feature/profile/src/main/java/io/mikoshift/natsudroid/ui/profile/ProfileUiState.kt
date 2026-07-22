package io.mikoshift.natsudroid.ui.profile

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val displayDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

private fun Instant.toDisplayDate(): String = displayDateFormatter.format(this)

data class UserUiModel(val displayName: String, val email: String, val memberSince: String?)

data class SessionUiModel(val id: String, val deviceName: String, val createdAt: String, val isCurrent: Boolean)

fun io.mikoshift.natsudroid.core.model.User.toUiModel(): UserUiModel = UserUiModel(
    displayName = name,
    email = email,
    memberSince = createdAt?.toDisplayDate(),
)

fun io.mikoshift.natsudroid.core.model.DeviceSession.toUiModel(): SessionUiModel = SessionUiModel(
    id = id,
    deviceName = name,
    createdAt = createdAt.toDisplayDate(),
    isCurrent = current,
)

data class DictionaryUiModel(
    val id: String,
    val title: String,
    val termCount: Int,
    val enabled: Boolean,
    val isToggling: Boolean,
)

fun io.mikoshift.natsudroid.core.model.Dictionary.toUiModel(isToggling: Boolean = false): DictionaryUiModel =
    DictionaryUiModel(
        id = id,
        title = title,
        termCount = termCount,
        enabled = enabled,
        isToggling = isToggling,
    )

data class ProfileUiState(
    val user: UserUiModel? = null,
    val sessions: List<SessionUiModel> = emptyList(),
    val dictionaries: List<DictionaryUiModel> = emptyList(),
    val isLoadingDictionaries: Boolean = false,
    val togglingDictionaryId: String? = null,
    val deletePassword: String = "",
    val deletePasswordError: String? = null,
    val isLoadingUser: Boolean = false,
    val isLoadingSessions: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val isLoggingOut: Boolean = false,
    val revokingSessionId: String? = null,
    val generalError: String? = null,
    val showDeleteDialog: Boolean = false,
)

sealed interface ProfileEffect {
    data class ShowMessage(val text: String) : ProfileEffect
}
