package io.mikoshift.natsu.ui.profile

data class UserUiModel(
    val displayName: String,
    val email: String,
    val memberSince: String,
)

data class SessionUiModel(
    val id: Long,
    val deviceName: String,
    val subtitle: String,
    val isCurrent: Boolean,
)

fun io.mikoshift.natsu.core.model.User.toUiModel(): UserUiModel = UserUiModel(
    displayName = name,
    email = email,
    memberSince = createdAt,
)

fun io.mikoshift.natsu.core.model.DeviceSession.toUiModel(): SessionUiModel = SessionUiModel(
    id = id,
    deviceName = name,
    subtitle = if (current) "Current · $createdAt" else createdAt,
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
