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
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InflationScreen(
    navController: NavHostController,
    database: AppDatabase,
    settingsManager: SettingsManager
) {
    var amount by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("6.0") }
    var years by remember { mutableStateOf("10") }
    
    var futureValue by remember { mutableStateOf<Double?>(null) }
    var inflationIncrease by remember { mutableStateOf<Double?>(null) }
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
        val a = amount.toDoubleOrNull()
        val r = rate.toDoubleOrNull()
        val y = years.toDoubleOrNull()
        
        if (a == null || r == null || y == null || a <= 0 || r < 0 || y <= 0) {
            validationError = "Please enter valid positive values for all fields."
            futureValue = null
            inflationIncrease = null
            return
        }
        validationError = ""
        val rateFraction = r / 100
        val fv = a * (1 + rateFraction).pow(y)
        val increase = fv - a

        futureValue = fv
        inflationIncrease = increase
        focusManager.clearFocus()

        coroutineScope.launch {
            database.historyDao().insert(
                HistoryEntry(
                    toolName = "Inflation Calculator",
                    inputSummary = "Present: $currencySymbol${formatNumber(a)}, Rate: ${formatNumber(r, 1)}%, Years: ${formatNumber(y, 0)}",
                    result = "Future: $currencySymbol${formatNumber(fv)}, Increase: $currencySymbol${formatNumber(increase)}"
                )
            )
        }
    }

    fun reset() {
        amount = ""
        rate = "6.0"
        years = "10"
        futureValue = null
        inflationIncrease = null
        validationError = ""
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inflation Calculator") },
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
                value = amount,
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
                    amount = filtered
                },
                visualTransformation = remember(isIndianLocale) { NumberCommaVisualTransformation(isIndianLocale) },
                label = { Text("Current Amount ($currencySymbol)") },
                prefix = { Text("$currencySymbol ") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inflation_amount_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text(if (isIndianLocale) "e.g. 100,000" else "e.g. 10,000") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = rate,
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
                    rate = filtered
                },
                label = { Text("Expected Inflation Rate (% per year)") },
                suffix = { Text("%") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inflation_rate_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("6.0") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = years,
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
                    years = filtered
                },
                label = { Text("Time Horizon (Years)") },
                suffix = { Text("yr") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inflation_years_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("10") },
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

            if (futureValue != null && inflationIncrease != null) {
                val presentAmt = amount.toDoubleOrNull() ?: 0.0
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inflation_result_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Future Equivalent Cost",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$currencySymbol ${formatNumber(futureValue!!)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "in $years years with $rate% annual inflation",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Purchasing Power Summary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current Value", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol ${formatNumber(presentAmt)}", fontWeight = FontWeight.Medium)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Inflation Increase (Erosion)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("+ $currencySymbol ${formatNumber(inflationIncrease!!)}", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Future Amount Needed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol ${formatNumber(futureValue!!)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

