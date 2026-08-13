package com.example.storage

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val CURRENCY_KEY = stringPreferencesKey("currency")
        val UNIT_SYSTEM_KEY = stringPreferencesKey("unit_system")
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "System default"
    }
    val currencyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CURRENCY_KEY] ?: "USD ($)"
    }
    val unitSystemFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[UNIT_SYSTEM_KEY] ?: "Metric"
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[THEME_KEY] = theme }
    }
    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { it[CURRENCY_KEY] = currency }
    }
    suspend fun setUnitSystem(system: String) {
        context.dataStore.edit { it[UNIT_SYSTEM_KEY] = system }
    }
}
