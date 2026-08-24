package com.example.storage

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val CURRENCY_KEY = stringPreferencesKey("currency")
        val UNIT_SYSTEM_KEY = stringPreferencesKey("unit_system")
        val PRECISION_KEY = intPreferencesKey("precision")
    }

    private fun getDefaultCurrency(): String {
        val country = Locale.getDefault().country
        return when (country) {
            "IN" -> "INR (₹)"
            "GB" -> "GBP (£)"
            "JP" -> "JPY (¥)"
            "DE", "FR", "IT", "ES", "NL", "BE", "IE", "GR", "PT", "FI", "AT" -> "EUR (€)"
            else -> "USD ($)"
        }
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "System default"
    }
    val currencyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CURRENCY_KEY] ?: getDefaultCurrency()
    }
    val unitSystemFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[UNIT_SYSTEM_KEY] ?: "Metric"
    }
    val precisionFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PRECISION_KEY] ?: 2
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
    suspend fun setPrecision(precision: Int) {
        context.dataStore.edit { it[PRECISION_KEY] = precision }
    }
}
