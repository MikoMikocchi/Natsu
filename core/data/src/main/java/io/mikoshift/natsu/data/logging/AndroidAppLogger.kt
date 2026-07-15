package io.mikoshift.natsu.data.logging

import android.util.Log
import io.mikoshift.natsu.core.common.logging.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAppLogger @Inject constructor() : AppLogger {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}
