package io.mikoshift.natsu.data.remote

/**
 * Central network configuration for the app's Retrofit clients.
 */
object NetworkConfig {

    // 10.0.2.2 is the Android emulator's alias for the host machine's localhost, so this
    // points at a backend running locally on the developer's machine (e.g. `rails s` on
    // port 3000). This is only valid for local dev against the emulator; a real app would
    // need this configurable per build variant (dev/staging/prod), but that's out of scope
    // for this subtask.
    const val BASE_URL = "http://10.0.2.2:3000/v1/"
}
