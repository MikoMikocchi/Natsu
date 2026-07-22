package io.mikoshift.natsudroid.ui.profile

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val currentPasswordError: String? = null,
    val passwordError: String? = null,
    val passwordConfirmationError: String? = null,
    val generalError: String? = null,
    val successMessage: String? = null,
    val isLoading: Boolean = false,
)
