package com.example.calculators

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

data class ExchangeRateResponse(
    val result: String,
    val base_code: String,
    val rates: Map<String, Double>
)

interface ExchangeRateApi {
    @GET("v6/latest/USD")
    suspend fun getLatestRates(): ExchangeRateResponse
}

object CurrencyConverter {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://open.er-api.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(ExchangeRateApi::class.java)

    // Offline reference rates (Base: USD)
    private val fallbackRates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "JPY" to 150.0,
        "INR" to 83.0,
        "AUD" to 1.52,
        "CAD" to 1.35,
        "CHF" to 0.88,
        "CNY" to 7.19,
        "SGD" to 1.34,
        "MXN" to 17.05,
        "BRL" to 5.0,
        "ZAR" to 18.9,
        "NZD" to 1.65,
        "HKD" to 7.82,
        "KRW" to 1340.0,
        "TRY" to 31.0,
        "AED" to 3.67,
        "SAR" to 3.75,
        "THB" to 35.8
    )

    val rates = mutableStateMapOf<String, Double>().apply {
        putAll(fallbackRates)
    }

    fun getCurrencyName(code: String): String {
        return when (code) {
            "USD" -> "United States - Dollar"
            "EUR" -> "European Union - Euro"
            "GBP" -> "United Kingdom - Pound"
            "JPY" -> "Japan - Yen"
            "INR" -> "India - Rupee"
            "AUD" -> "Australia - Dollar"
            "CAD" -> "Canada - Dollar"
            "CHF" -> "Switzerland - Franc"
            "CNY" -> "China - Yuan"
            "SGD" -> "Singapore - Dollar"
            "MXN" -> "Mexico - Peso"
            "BRL" -> "Brazil - Real"
            "ZAR" -> "South Africa - Rand"
            "NZD" -> "New Zealand - Dollar"
            "HKD" -> "Hong Kong - Dollar"
            "KRW" -> "South Korea - Won"
            "TRY" -> "Turkey - Lira"
            "AED" -> "UAE - Dirham"
            "SAR" -> "Saudi Arabia - Riyal"
            "THB" -> "Thailand - Baht"
            else -> code
        }
    }

    val isLive = mutableStateOf(false)

    suspend fun fetchLiveRates() {
        try {
            withContext(Dispatchers.IO) {
                val response = api.getLatestRates()
                if (response.result == "success" || response.rates.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        rates.putAll(response.rates)
                        isLive.value = true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Keep fallback rates if network fails
        }
    }

    fun convert(value: Double, fromCurrency: String, toCurrency: String): Double {
        val fromRate = rates[fromCurrency] ?: 1.0
        val toRate = rates[toCurrency] ?: 1.0
        
        val valueInUsd = value / fromRate
        return valueInUsd * toRate
    }
}
