package io.mikoshift.natsu.core.model

data class DocumentCache(
    val documentId: String,
    val localPackagePath: String? = null,
    val cachedPackageSha256: String? = null,
)
