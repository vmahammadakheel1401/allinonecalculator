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
fun SIPScreen(
    navController: NavHostController,
    database: AppDatabase,
    settingsManager: SettingsManager
) {
    var monthlyInvestment by remember { mutableStateOf("") }
    var annualReturn by remember { mutableStateOf("12") }
    var years by remember { mutableStateOf("10") }
    var totalInvested by remember { mutableStateOf<Double?>(null) }
    var estimatedReturns by remember { mutableStateOf<Double?>(null) }
    var finalValue by remember { mutableStateOf<Double?>(null) }
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
        val p = monthlyInvestment.toDoubleOrNull()
        val rAnnual = annualReturn.toDoubleOrNull()
        val y = years.toDoubleOrNull()

        if (p == null || rAnnual == null || y == null || p <= 0 || rAnnual <= 0 || y <= 0) {
            validationError = "Please enter valid positive values for all fields."
            totalInvested = null
            estimatedReturns = null
            finalValue = null
            return
        }

        validationError = ""
        val i = (rAnnual / 100) / 12
        val n = y * 12

        val invested = p * n
        val fv = p * ((1 + i).pow(n) - 1) * (1 + i) / i
        val returns = fv - invested

        totalInvested = invested
        estimatedReturns = returns
        finalValue = fv
        focusManager.clearFocus()

        coroutineScope.launch {
            database.historyDao().insert(
                HistoryEntry(
                    toolName = "SIP Calculator",
                    inputSummary = "Monthly: $currencySymbol${formatNumber(p)}, Return: ${formatNumber(rAnnual, 1)}%, Tenure: ${formatNumber(y, 0)} yr",
                    result = "Maturity: $currencySymbol${formatNumber(fv)}, Profit: $currencySymbol${formatNumber(returns)}"
                )
            )
        }
    }

    fun reset() {
        monthlyInvestment = ""
        annualReturn = "12"
        years = "10"
        totalInvested = null
        estimatedReturns = null
        finalValue = null
        validationError = ""
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SIP Calculator") },
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
                value = monthlyInvestment,
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
                    monthlyInvestment = filtered
                },
                visualTransformation = remember(isIndianLocale) { NumberCommaVisualTransformation(isIndianLocale) },
                label = { Text("Monthly Investment ($currencySymbol)") },
                prefix = { Text("$currencySymbol ") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sip_monthly_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text(if (isIndianLocale) "e.g. 5,000" else "e.g. 500") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = annualReturn,
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
                    annualReturn = filtered
                },
                label = { Text("Expected Annual Return (%)") },
                suffix = { Text("%") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sip_return_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("12") },
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
                label = { Text("Time Period (Years)") },
                suffix = { Text("yr") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sip_years_input"),
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

            if (finalValue != null && totalInvested != null && estimatedReturns != null) {
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sip_result_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Expected Maturity Amount",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$currencySymbol ${formatNumber(finalValue!!)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
                            text = "Investment Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Invested Amount", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol ${formatNumber(totalInvested!!)}", fontWeight = FontWeight.Medium)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimated Gain / Returns", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("+ $currencySymbol ${formatNumber(estimatedReturns!!)}", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Maturity Value", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol ${formatNumber(finalValue!!)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

