package io.mikoshift.natsu.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.mikoshift.natsu.data.repository.AuthError
import io.mikoshift.natsu.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadUser()
        loadSessions()
    }

    fun onDeletePasswordChange(value: String) {
        _uiState.update { it.copy(deletePassword = value, deletePasswordError = null) }
    }

    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true, deletePassword = "", deletePasswordError = null) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false, deletePassword = "", deletePasswordError = null) }
    }

    fun logout() {
        _uiState.update { it.copy(isLoggingOut = true, generalError = null) }
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoggingOut = false) }
        }
    }

    fun deleteAccount() {
        val password = _uiState.value.deletePassword
        if (password.isBlank()) {
            _uiState.update { it.copy(deletePasswordError = "Password is required") }
            return
        }

        _uiState.update { it.copy(isDeletingAccount = true, deletePasswordError = null, generalError = null) }
        viewModelScope.launch {
            authRepository.deleteAccount(password).fold(
                onSuccess = {
                    _uiState.update { it.copy(isDeletingAccount = false, showDeleteDialog = false) }
                },
                onFailure = { throwable ->
                    applyError(throwable as? AuthError, forDelete = true)
                },
            )
        }
    }

    fun revokeSession(sessionId: Long, isCurrent: Boolean) {
        _uiState.update { it.copy(revokingSessionId = sessionId, generalError = null) }
        viewModelScope.launch {
            authRepository.revokeSession(sessionId, isCurrent).fold(
                onSuccess = {
                    _uiState.update { it.copy(revokingSessionId = null) }
                    if (!isCurrent) {
                        loadSessions()
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(revokingSessionId = null) }
                    applyError(throwable as? AuthError)
                },
            )
        }
    }

    private fun loadUser() {
        _uiState.update { it.copy(isLoadingUser = true, generalError = null) }
        viewModelScope.launch {
            authRepository.getUser().fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(isLoadingUser = false, user = user) }
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isLoadingUser = false) }
                    applyError(throwable as? AuthError)
                },
            )
        }
    }

    private fun loadSessions() {
        _uiState.update { it.copy(isLoadingSessions = true) }
        viewModelScope.launch {
            authRepository.getSessions().fold(
                onSuccess = { sessions ->
                    _uiState.update { it.copy(isLoadingSessions = false, sessions = sessions) }
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isLoadingSessions = false) }
                    applyError(throwable as? AuthError)
                },
            )
        }
    }

    private fun applyError(error: AuthError?, forDelete: Boolean = false) {
        when (error) {
            is AuthError.ValidationError -> {
                val fieldErrors = error.fieldErrors
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        deletePasswordError = if (forDelete) {
                            fieldErrors["password"]?.joinToString(", ")
                        } else {
                            it.deletePasswordError
                        },
                        generalError = fieldErrors["base"]?.joinToString(", ")
                            ?: fieldErrors.filterKeys { key -> key !in setOf("password") }
                                .values.flatten().takeIf { messages -> messages.isNotEmpty() }
                                ?.joinToString(", "),
                    )
                }
            }
            is AuthError.NetworkFailure -> {
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        generalError = "Network error, please try again",
                    )
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        generalError = (error as? AuthError.Unknown)?.errorMessage
                            ?: "Something went wrong, please try again",
                    )
                }
            }
        }
    }
}
