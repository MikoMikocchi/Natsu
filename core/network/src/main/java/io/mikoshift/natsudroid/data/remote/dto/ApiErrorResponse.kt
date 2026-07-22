package io.mikoshift.natsudroid.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Backend validation error envelope, e.g. `{"errors": {"email": ["is invalid"]}}`.
 * Returned on 400/422 responses; parsed from the error body rather than a 2xx response.
 */
@Serializable
data class ApiErrorResponse(val errors: Map<String, List<String>>)
