package io.mikoshift.natsu.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.mikoshift.natsu.core.domain.repository.AuthRepository
import io.mikoshift.natsu.core.domain.usecase.DeleteAccountUseCase
import io.mikoshift.natsu.core.domain.usecase.LogoutUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveUserProfileUseCase
import io.mikoshift.natsu.core.domain.usecase.RevokeSessionUseCase
import io.mikoshift.natsu.core.model.AuthError
import io.mikoshift.natsu.feature.profile.R
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeUserProfile: ObserveUserProfileUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val revokeSession: RevokeSessionUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

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
            logoutUseCase()
            _uiState.update { it.copy(isLoggingOut = false) }
        }
    }

    fun deleteAccount() {
        val password = _uiState.value.deletePassword
        if (password.isBlank()) {
            _uiState.update {
                it.copy(deletePasswordError = context.getString(R.string.error_password_required))
            }
            return
        }

        _uiState.update { it.copy(isDeletingAccount = true, deletePasswordError = null, generalError = null) }
        viewModelScope.launch {
            deleteAccount(password).fold(
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
            revokeSession(sessionId = sessionId, isCurrentSession = isCurrent).fold(
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
            observeUserProfile().fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(isLoadingUser = false, user = user.toUiModel()) }
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
                    _uiState.update {
                        it.copy(
                            isLoadingSessions = false,
                            sessions = sessions.map { session -> session.toUiModel() },
                        )
                    }
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
                _uiState.update { it.copy(isDeletingAccount = false, generalError = null) }
                viewModelScope.launch {
                    _effects.send(ProfileEffect.ShowMessage(context.getString(R.string.error_network)))
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        generalError = (error as? AuthError.Unknown)?.errorMessage
                            ?: context.getString(R.string.error_generic),
                    )
                }
            }
        }
    }
}
