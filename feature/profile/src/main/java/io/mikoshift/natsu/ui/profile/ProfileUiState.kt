package io.mikoshift.natsu.ui.profile

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val displayDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

private fun Instant.toDisplayDate(): String = displayDateFormatter.format(this)

data class UserUiModel(
    val displayName: String,
    val email: String,
    val memberSince: String,
)

data class SessionUiModel(
    val id: Long,
    val deviceName: String,
    val createdAt: String,
    val isCurrent: Boolean,
)

fun io.mikoshift.natsu.core.model.User.toUiModel(): UserUiModel = UserUiModel(
    displayName = name,
    email = email,
    memberSince = createdAt.toDisplayDate(),
)

fun io.mikoshift.natsu.core.model.DeviceSession.toUiModel(): SessionUiModel = SessionUiModel(
    id = id,
    deviceName = name,
    createdAt = createdAt.toDisplayDate(),
    isCurrent = current,
)

data class ProfileUiState(
    val user: UserUiModel? = null,
    val sessions: List<SessionUiModel> = emptyList(),
    val deletePassword: String = "",
    val deletePasswordError: String? = null,
    val isLoadingUser: Boolean = false,
    val isLoadingSessions: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val isLoggingOut: Boolean = false,
    val revokingSessionId: Long? = null,
    val generalError: String? = null,
    val showDeleteDialog: Boolean = false,
)

sealed interface ProfileEffect {
    data class ShowMessage(val text: String) : ProfileEffect
}
