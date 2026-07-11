package io.mikoshift.natsu.data.repository

sealed class DocumentError : RuntimeException() {
    data class ValidationError(val fieldErrors: Map<String, List<String>>) : DocumentError()
    data object Unauthorized : DocumentError()
    data object NetworkFailure : DocumentError()
    data class ImportFailed(val reason: String?) : DocumentError()
    data class Unknown(val errorMessage: String?) : DocumentError()
}
