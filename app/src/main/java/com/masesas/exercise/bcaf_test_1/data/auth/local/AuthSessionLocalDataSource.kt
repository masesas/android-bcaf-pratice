package com.masesas.exercise.bcaf_test_1.data.auth.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthSession
import com.masesas.exercise.bcaf_test_1.domain.auth.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val AUTH_PREFERENCES_NAME = "auth_session"

private val Context.authPreferences: DataStore<Preferences> by preferencesDataStore(
    name = AUTH_PREFERENCES_NAME,
)

class AuthSessionLocalDataSource(context: Context) {

    private val dataStore = context.applicationContext.authPreferences

    fun observe(): Flow<AuthSession?> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map(::toSession)

   suspend fun save(session: AuthSession) {
        dataStore.edit { preferences ->
            preferences[Keys.USER_ID] = session.user.id
            preferences[Keys.USER_NAME] = session.user.name
            preferences[Keys.USER_EMAIL] = session.user.email
            preferences[Keys.USER_TIPE] = session.user.tipe.orEmpty()
            preferences[Keys.USER_ROLES] = session.user.roles.toSet()
            preferences[Keys.ACCESS_TOKEN] = session.accessToken
            preferences[Keys.EXPIRES_AT] = session.expiresAtMillis
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private fun toSession(preferences: Preferences): AuthSession? {
        val id = preferences[Keys.USER_ID] ?: return null
        val name = preferences[Keys.USER_NAME] ?: return null
        val email = preferences[Keys.USER_EMAIL] ?: return null
        val token = preferences[Keys.ACCESS_TOKEN] ?: return null
        val expiresAt = preferences[Keys.EXPIRES_AT] ?: return null

        return AuthSession(
            user = AuthUser(
                id = id,
                name = name,
                email = email,
                tipe = preferences[Keys.USER_TIPE]?.takeIf { it.isNotBlank() },
                roles = preferences[Keys.USER_ROLES].orEmpty().toList(),
            ),
            accessToken = token,
            expiresAtMillis = expiresAt,
        )
    }

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_TIPE = stringPreferencesKey("user_tipe")
        val USER_ROLES = stringSetPreferencesKey("user_roles")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val EXPIRES_AT = longPreferencesKey("expires_at")
    }
}
