package io.mikoshift.natsudroid.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Wraps [EncryptedSharedPreferences] until auth storage is migrated to DataStore + Tink.
 * security-crypto 1.1.0 deprecated these APIs without a drop-in replacement.
 */
@Suppress("DEPRECATION")
internal object EncryptedPrefsFactory {
    fun create(
        context: Context,
        fileName: String,
    ): SharedPreferences {
        val masterKey =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

        return EncryptedSharedPreferences.create(
            context,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
