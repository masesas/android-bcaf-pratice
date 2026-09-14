package com.masesas.exercise.bcaf_test_1.data.notification.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.masesas.exercise.bcaf_test_1.BuildConfig
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

private const val DEVICE_REGISTRATION_PREFERENCES_NAME =
    "${BuildConfig.APPLICATION_ID}_device_registration"

private val Context.deviceRegistrationPreferences: DataStore<Preferences> by preferencesDataStore(
    name = DEVICE_REGISTRATION_PREFERENCES_NAME,
)

/** Menyimpan installation id yang sudah diterima backend, penanda apakah sinkronisasi perlu diulang. */
class DeviceRegistrationLocalDataSource(context: Context) {

    private val dataStore = context.applicationContext.deviceRegistrationPreferences

    suspend fun syncedInstallationId(): String? = dataStore.data
        .catch { throwable -> if (throwable is IOException) emit(emptyPreferences()) else throw throwable }
        .first()[Keys.INSTALLATION_ID]

    suspend fun save(installationId: String) {
        dataStore.edit { preferences -> preferences[Keys.INSTALLATION_ID] = installationId }
    }

    suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private object Keys {
        val INSTALLATION_ID = stringPreferencesKey("installation_id")
    }
}
