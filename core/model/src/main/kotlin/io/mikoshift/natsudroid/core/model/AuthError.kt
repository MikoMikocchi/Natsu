package io.mikoshift.natsudroid.core.model

sealed class AuthError : RuntimeException() {
    data class ValidationError(val fieldErrors: Map<String, List<String>>) : AuthError()

    data object Unauthorized : AuthError()

    data object NetworkFailure : AuthError()

    data class Unknown(val errorMessage: String?) : AuthError()
}
