package io.mikoshift.natsudroid.di

/**
 * Android emulator maps [EMULATOR_HOST] to the development machine's loopback;
 * [LOCALHOST_PATTERN] inside the emulator points at the emulator itself.
 */
internal object DevApiUrl {
    private val LOCALHOST_PATTERN = Regex("""127\.0\.0\.1|localhost""")
    private const val EMULATOR_HOST = "10.0.2.2"

    fun resolve(url: String, isDebugBuild: Boolean): String {
        if (!isDebugBuild) return url
        return url.replaceFirst(LOCALHOST_PATTERN, EMULATOR_HOST)
    }
}
