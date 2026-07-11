package io.mikoshift.natsu.di

import android.content.Context

/**
 * Simple manual dependency container for the app (no Hilt/Dagger).
 *
 * This is a stub: later subtasks will add lazily-initialized properties here for things
 * like the Retrofit/OkHttp network client, the TokenStore (EncryptedSharedPreferences),
 * and the AuthRepository.
 */
class AppContainer(private val context: Context) {

    // TODO: network client (OkHttpClient / Retrofit), TokenStore, AuthRepository, etc.
    // will be added here as lazy-initialized properties by later subtasks.
}
