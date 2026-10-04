package com.phoneguard.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.phoneguard.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** [PreferencesRepository] backed by Jetpack DataStore. */
@Singleton
class DataStorePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    private val safeData: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    override val onboardingCompleted: Flow<Boolean> = safeData.map { it[ONBOARDING_DONE] ?: false }

    override suspend fun setOnboardingCompleted() {
        dataStore.edit { it[ONBOARDING_DONE] = true }
    }

    override suspend fun knownRiskKeys(): Set<String> = safeData.first()[KNOWN_RISKS].orEmpty()

    override suspend fun setKnownRiskKeys(keys: Set<String>) {
        dataStore.edit { it[KNOWN_RISKS] = keys }
    }

    private companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KNOWN_RISKS = stringSetPreferencesKey("known_risk_keys")
    }
}
