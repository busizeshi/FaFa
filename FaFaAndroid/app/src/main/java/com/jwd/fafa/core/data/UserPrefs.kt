package com.jwd.fafa.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 本地偏好：欢迎页完成标记、协议同意版本（与 bootstrap legalVersions 比对）。 */
@Singleton
class UserPrefs @Inject constructor(
    @com.jwd.fafa.core.di.UserPrefsDataStore private val dataStore: DataStore<Preferences>,
) {

    val welcomeCompletedFlow: Flow<Boolean> =
        dataStore.data.map { it[KEY_WELCOME_COMPLETED] == true }

    /** 已同意的协议版本，key 为 documentType。 */
    val consentedVersionsFlow: Flow<Map<String, String>> =
        dataStore.data.map { prefs -> prefs[KEY_CONSENT]?.let(::deserialize) ?: emptyMap() }

    suspend fun setWelcomeCompleted() {
        dataStore.edit { it[KEY_WELCOME_COMPLETED] = true }
    }

    suspend fun consentVersions(): Map<String, String> =
        dataStore.data.first()[KEY_CONSENT]?.let(::deserialize) ?: emptyMap()

    suspend fun saveConsent(versions: Map<String, String>) {
        dataStore.edit { it[KEY_CONSENT] = serialize(versions) }
    }

    suspend fun clearConsent() {
        dataStore.edit { it.remove(KEY_CONSENT) }
    }

    private fun serialize(versions: Map<String, String>): String =
        versions.entries.joinToString(",") { "${it.key}=${it.value}" }

    private fun deserialize(raw: String): Map<String, String> =
        raw.split(",").mapNotNull { entry ->
            entry.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
        }.toMap()

    private companion object {
        val KEY_WELCOME_COMPLETED = booleanPreferencesKey("welcome_completed")
        val KEY_CONSENT = stringPreferencesKey("consent_versions")
    }
}
