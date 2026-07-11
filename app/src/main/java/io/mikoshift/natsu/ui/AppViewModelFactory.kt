package io.mikoshift.natsu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.mikoshift.natsu.data.repository.AuthRepository
import io.mikoshift.natsu.data.repository.DocumentRepository
import io.mikoshift.natsu.ui.auth.ForgotPasswordViewModel
import io.mikoshift.natsu.ui.auth.LoginViewModel
import io.mikoshift.natsu.ui.auth.RegisterViewModel
import io.mikoshift.natsu.ui.auth.ResetPasswordViewModel
import io.mikoshift.natsu.ui.library.LibraryViewModel
import io.mikoshift.natsu.ui.profile.ChangePasswordViewModel
import io.mikoshift.natsu.ui.profile.ProfileViewModel

class AppViewModelFactory(
    private val authRepository: AuthRepository,
    private val documentRepository: DocumentRepository,
    private val resetToken: String = "",
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(RegisterViewModel::class.java) ->
            RegisterViewModel(authRepository) as T
        modelClass.isAssignableFrom(LoginViewModel::class.java) ->
            LoginViewModel(authRepository) as T
        modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java) ->
            ForgotPasswordViewModel(authRepository) as T
        modelClass.isAssignableFrom(ResetPasswordViewModel::class.java) ->
            ResetPasswordViewModel(authRepository, resetToken) as T
        modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
            ProfileViewModel(authRepository) as T
        modelClass.isAssignableFrom(ChangePasswordViewModel::class.java) ->
            ChangePasswordViewModel(authRepository) as T
        modelClass.isAssignableFrom(LibraryViewModel::class.java) ->
            LibraryViewModel(documentRepository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
