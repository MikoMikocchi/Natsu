package io.mikoshift.natsu.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Note: the backend serializes JSON with snake_case property naming
// (spring.jackson.property-naming-strategy: SNAKE_CASE), so multi-word
// fields need an explicit @SerialName to match the wire format even
// though the Kotlin property names stay camelCase.

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)

@Serializable
data class DeleteAccountRequest(val password: String)
