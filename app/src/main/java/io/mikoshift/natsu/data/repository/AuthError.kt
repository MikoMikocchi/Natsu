package io.mikoshift.natsu.data.repository

/**
 * Domain-level error type for [AuthRepository] operations.
 *
 * Extends [RuntimeException] so it can be used directly as the failure value of a
 * `kotlin.Result<T>` (via `Result.failure(authError)`); callers can recover it with
 * `result.exceptionOrNull() as? AuthError` without needing a separate wrapper exception.
 */
sealed class AuthError : RuntimeException() {
    data class ValidationError(val fieldErrors: Map<String, List<String>>) : AuthError()
    data object Unauthorized : AuthError()
    data object NetworkFailure : AuthError()
    data class Unknown(val errorMessage: String?) : AuthError()
}
