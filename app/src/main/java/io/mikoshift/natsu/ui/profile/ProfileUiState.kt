package io.mikoshift.natsu.ui.profile

import io.mikoshift.natsu.data.remote.dto.DeviceSessionResponse
import io.mikoshift.natsu.data.remote.dto.UserResponse

data class ProfileUiState(
    val user: UserResponse? = null,
    val sessions: List<DeviceSessionResponse> = emptyList(),
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
