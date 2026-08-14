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
    val rates: Map<String, Double>,
    val time_last_update_unix: Long?
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

    val rates = mutableStateMapOf<String, Double>()
    var ratesTimestamp = mutableStateOf<Long?>(null)

    data class CurrencyInfo(val code: String, val name: String, val region: String, val symbol: String?)

    private val currencyMetadata = mapOf(
        "USD" to CurrencyInfo("USD", "Dollar", "United States", "$"),
        "AED" to CurrencyInfo("AED", "Dirham", "United Arab Emirates", "د.إ"),
        "INR" to CurrencyInfo("INR", "Rupee", "India", "₹"),
        "EUR" to CurrencyInfo("EUR", "Euro", "Eurozone", "€"),
        "GBP" to CurrencyInfo("GBP", "Pound", "United Kingdom", "£"),
        "JPY" to CurrencyInfo("JPY", "Yen", "Japan", "¥"),
        "CNY" to CurrencyInfo("CNY", "Yuan", "China", "¥"),
        "AUD" to CurrencyInfo("AUD", "Dollar", "Australia", "A$"),
        "CAD" to CurrencyInfo("CAD", "Dollar", "Canada", "C$"),
        "SGD" to CurrencyInfo("SGD", "Dollar", "Singapore", "S$"),
        "SAR" to CurrencyInfo("SAR", "Riyal", "Saudi Arabia", "﷼"),
        "CHF" to CurrencyInfo("CHF", "Franc", "Switzerland", "CHF"),
        "KRW" to CurrencyInfo("KRW", "Won", "South Korea", "₩"),
        "THB" to CurrencyInfo("THB", "Baht", "Thailand", "฿"),
        "MYR" to CurrencyInfo("MYR", "Ringgit", "Malaysia", "RM"),
        "IDR" to CurrencyInfo("IDR", "Rupiah", "Indonesia", "Rp"),
        "PHP" to CurrencyInfo("PHP", "Peso", "Philippines", "₱"),
        "VND" to CurrencyInfo("VND", "Dong", "Vietnam", "₫"),
        "RUB" to CurrencyInfo("RUB", "Ruble", "Russia", "₽"),
        "AFN" to CurrencyInfo("AFN", "Afghani", "Afghanistan", "؋"),
        "ALL" to CurrencyInfo("ALL", "Lek", "Albania", "L"),
        "DZD" to CurrencyInfo("DZD", "Dinar", "Algeria", "دج"),
        "ARS" to CurrencyInfo("ARS", "Peso", "Argentina", "$"),
        "BRL" to CurrencyInfo("BRL", "Real", "Brazil", "R$"),
        "XAF" to CurrencyInfo("XAF", "CFA Franc", "Central Africa", "FCFA"),
        "XOF" to CurrencyInfo("XOF", "CFA Franc", "West Africa", "CFA"),
        "XCD" to CurrencyInfo("XCD", "East Caribbean Dollar", "East Caribbean", "EC$"),
        "MXN" to CurrencyInfo("MXN", "Peso", "Mexico", "MX$"),
        "ZAR" to CurrencyInfo("ZAR", "Rand", "South Africa", "R"),
        "TRY" to CurrencyInfo("TRY", "Lira", "Türkiye", "₺"),
        "NZD" to CurrencyInfo("NZD", "Dollar", "New Zealand", "$"),
        "HKD" to CurrencyInfo("HKD", "Dollar", "Hong Kong", "$")
    )

    private val priorityCodes = listOf("USD", "AED", "INR", "EUR", "GBP", "JPY", "CNY", "AUD", "CAD", "SGD")

    fun getSupportedCurrencies(): List<String> {
        val allCodes = rates.keys.toList()
        val sorted = allCodes.sortedWith(Comparator { a, b ->
            val aPriority = priorityCodes.indexOf(a)
            val bPriority = priorityCodes.indexOf(b)
            
            if (aPriority != -1 && bPriority != -1) return@Comparator aPriority.compareTo(bPriority)
            if (aPriority != -1) return@Comparator -1
            if (bPriority != -1) return@Comparator 1
            
            val infoA = getCurrencyDisplayInfo(a)
            val infoB = getCurrencyDisplayInfo(b)
            infoA.region.compareTo(infoB.region)
        })
        return sorted
    }

    fun getCurrencyDisplayInfo(code: String): CurrencyInfo {
        return currencyMetadata[code] ?: CurrencyInfo(code, "Currency", "Other", null)
    }

    val isLive = mutableStateOf(false)

    suspend fun fetchLiveRates(): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val response = api.getLatestRates()
                if (response.result == "success" && response.rates.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        rates.clear()
                        rates.putAll(response.rates)
                        ratesTimestamp.value = response.time_last_update_unix
                        isLive.value = true
                        true
                    }
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun convert(value: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return value
        val fromRate = rates[fromCurrency] ?: return 0.0
        val toRate = rates[toCurrency] ?: return 0.0
        
        val valueInUsd = value / fromRate
        return valueInUsd * toRate
    }
}
