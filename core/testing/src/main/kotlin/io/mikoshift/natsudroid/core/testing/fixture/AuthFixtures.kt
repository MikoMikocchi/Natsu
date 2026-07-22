package io.mikoshift.natsudroid.core.testing.fixture

import io.mikoshift.natsudroid.core.model.AuthSession
import io.mikoshift.natsudroid.core.model.DeviceSession
import io.mikoshift.natsudroid.core.model.User
import java.time.Instant

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
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ) = User(
        id = id,
        name = name,
        email = email,
        createdAt = createdAt,
    )

    fun deviceSession(
        id: String = "session-1",
        name: String = "Pixel 8",
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        current: Boolean = true,
    ) = DeviceSession(
        id = id,
        name = name,
        createdAt = createdAt,
        current = current,
    )
}
