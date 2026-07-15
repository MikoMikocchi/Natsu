package io.mikoshift.natsu.core.common.logging

interface AppLogger {
    fun d(tag: String, message: String)

    fun e(tag: String, message: String, throwable: Throwable? = null)
}
