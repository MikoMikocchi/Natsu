package io.mikoshift.natsu.ui.profile

import io.mikoshift.natsu.core.model.DeviceSession
import io.mikoshift.natsu.core.model.User

data class ProfileUiState(
    val user: User? = null,
    val sessions: List<DeviceSession> = emptyList(),
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
