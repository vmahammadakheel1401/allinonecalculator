package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.models.HistoryEntry
import com.example.storage.AppDatabase
import com.example.storage.SettingsManager
import com.example.utilities.NumberCommaVisualTransformation
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelCostScreen(
    navController: NavHostController,
    database: AppDatabase,
    settingsManager: SettingsManager
) {
    var distance by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var totalCost by remember { mutableStateOf<Double?>(null) }
    var fuelNeeded by remember { mutableStateOf<Double?>(null) }
    var validationError by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val currency by settingsManager.currencyFlow.collectAsState(initial = "USD ($)")
    val precision by settingsManager.precisionFlow.collectAsState(initial = 2)

    val isIndianLocale = remember(currency) {
        currency.contains("INR")
    }

    val currencySymbol = remember(currency) {
        when {
            currency.contains("INR") -> "₹"
            currency.contains("EUR") -> "€"
            currency.contains("GBP") -> "£"
            currency.contains("JPY") -> "¥"
            currency.contains("(") && currency.contains(")") -> {
                currency.substringAfter("(").substringBefore(")")
            }
            else -> "$"
        }
    }

    fun formatNumber(value: Double, decimals: Int = precision): String {
        val locale = if (isIndianLocale) Locale("en", "IN") else Locale.US
        val formatter = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }
        return formatter.format(value)
    }

    fun calculate() {
        val d = distance.toDoubleOrNull()
        val m = mileage.toDoubleOrNull()
        val p = price.toDoubleOrNull()

        if (d == null || m == null || p == null || d <= 0 || m <= 0 || p <= 0) {
            validationError = "Please enter valid positive values for all fields."
            totalCost = null
            fuelNeeded = null
            return
        }

        validationError = ""
        val needed = d / m
        val cost = needed * p
        fuelNeeded = needed
        totalCost = cost
        focusManager.clearFocus()

        coroutineScope.launch {
            database.historyDao().insert(
                HistoryEntry(
                    toolName = "Fuel Cost",
                    inputSummary = "Dist: ${formatNumber(d)} km, Mileage: ${formatNumber(m, 1)} km/l, Price: $currencySymbol${formatNumber(p)}",
                    result = "Cost: $currencySymbol${formatNumber(cost)}, Fuel: ${formatNumber(needed, 2)} L"
                )
            )
        }
    }

    fun reset() {
        distance = ""
        mileage = ""
        price = ""
        totalCost = null
        fuelNeeded = null
        validationError = ""
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fuel Cost Calculator") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = distance,
                onValueChange = { input ->
                    val filtered = buildString {
                        var hasDecimal = false
                        for (char in input) {
                            if (char.isDigit()) append(char)
                            else if (char == '.' && !hasDecimal) {
                                append(char)
                                hasDecimal = true
                            }
                        }
                    }
                    distance = filtered
                },
                visualTransformation = remember(isIndianLocale) { NumberCommaVisualTransformation(isIndianLocale) },
                label = { Text("Distance (km)") },
                suffix = { Text("km") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fuel_distance_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text(if (isIndianLocale) "e.g. 500" else "e.g. 300") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = mileage,
                onValueChange = { input ->
                    val filtered = buildString {
                        var hasDecimal = false
                        for (char in input) {
                            if (char.isDigit()) append(char)
                            else if (char == '.' && !hasDecimal) {
                                append(char)
                                hasDecimal = true
                            }
                        }
                    }
                    mileage = filtered
                },
                label = { Text("Mileage (km/l or mpg)") },
                suffix = { Text("km/l") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fuel_mileage_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("e.g. 18.5") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = price,
                onValueChange = { input ->
                    val filtered = buildString {
                        var hasDecimal = false
                        for (char in input) {
                            if (char.isDigit()) append(char)
                            else if (char == '.' && !hasDecimal) {
                                append(char)
                                hasDecimal = true
                            }
                        }
                    }
                    price = filtered
                },
                visualTransformation = remember(isIndianLocale) { NumberCommaVisualTransformation(isIndianLocale) },
                label = { Text("Fuel Price ($currencySymbol per unit)") },
                prefix = { Text("$currencySymbol ") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fuel_price_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text(if (isIndianLocale) "e.g. 104.50" else "e.g. 3.75") },
                singleLine = true
            )

            if (validationError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = validationError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { calculate() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calculate_button")
                ) {
                    Text("Calculate")
                }

                OutlinedButton(
                    onClick = { reset() },
                    modifier = Modifier.testTag("reset_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }

            if (totalCost != null && fuelNeeded != null) {
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fuel_result_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Estimated Fuel Cost",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$currencySymbol ${formatNumber(totalCost!!)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Fuel Required: ${formatNumber(fuelNeeded!!, 2)} litres",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

