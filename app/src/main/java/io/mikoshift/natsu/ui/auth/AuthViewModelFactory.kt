package io.mikoshift.natsu.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.mikoshift.natsu.data.repository.AuthRepository

/**
 * Constructs [RegisterViewModel] or [LoginViewModel] with the shared [AuthRepository], for use
 * with `viewModel(factory = AuthViewModelFactory(appContainer.authRepository))` in Compose.
 */
class AuthViewModelFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(RegisterViewModel::class.java) ->
            RegisterViewModel(authRepository) as T
        modelClass.isAssignableFrom(LoginViewModel::class.java) ->
            LoginViewModel(authRepository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
