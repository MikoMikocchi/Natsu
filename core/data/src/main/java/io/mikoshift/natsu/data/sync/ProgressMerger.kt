package io.mikoshift.natsu.data.sync

import io.mikoshift.natsu.data.local.db.ReadingProgressEntity
import io.mikoshift.natsu.data.mapper.toEntity
import io.mikoshift.natsu.data.remote.dto.ReadingProgressResponse

object ProgressMerger {

    fun merge(
        server: ReadingProgressResponse,
        local: ReadingProgressEntity?,
        hasPendingOutbox: Boolean,
    ): ReadingProgressEntity {
        if (local == null) {
            return server.toEntity()
        }

        if (server.updatedAtMs > local.updatedAtMs) {
            return server.toEntity()
        }

        if (hasPendingOutbox) {
            return local
        }

        return server.toEntity()
    }
}
