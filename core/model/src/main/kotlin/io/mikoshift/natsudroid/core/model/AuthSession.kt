package io.mikoshift.natsudroid.core.model

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
    val userName: String,
    val userEmail: String,
)
