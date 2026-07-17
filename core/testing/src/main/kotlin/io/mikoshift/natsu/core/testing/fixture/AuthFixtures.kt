package io.mikoshift.natsu.core.testing.fixture

import io.mikoshift.natsu.core.model.AuthSession
import io.mikoshift.natsu.core.model.DeviceSession
import io.mikoshift.natsu.core.model.User

object AuthFixtures {
    fun session(
        accessToken: String = "access-token",
        refreshToken: String = "refresh-token",
        userId: Long = 1L,
        userName: String = "Test User",
        userEmail: String = "test@example.com",
    ) = AuthSession(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId,
        userName = userName,
        userEmail = userEmail,
    )

    fun user(
        id: Long = 1L,
        name: String = "Test User",
        email: String = "test@example.com",
        createdAt: String = "2026-01-01",
    ) = User(
        id = id,
        name = name,
        email = email,
        createdAt = createdAt,
    )

    fun deviceSession(
        id: Long = 1L,
        name: String = "Pixel 8",
        createdAt: String = "2026-01-01",
        current: Boolean = true,
    ) = DeviceSession(
        id = id,
        name = name,
        createdAt = createdAt,
        current = current,
    )
}
