package io.mikoshift.natsu.core.model

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
    val userName: String,
    val userEmail: String,
)
