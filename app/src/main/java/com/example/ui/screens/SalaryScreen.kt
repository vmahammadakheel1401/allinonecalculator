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
fun SalaryScreen(
    navController: NavHostController,
    database: AppDatabase,
    settingsManager: SettingsManager
) {
    var ctc by remember { mutableStateOf("") }
    var monthlyGross by remember { mutableStateOf<Double?>(null) }
    var weeklySalary by remember { mutableStateOf<Double?>(null) }
    var dailySalary by remember { mutableStateOf<Double?>(null) }
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
        val annual = ctc.toDoubleOrNull()
        if (annual == null || annual <= 0) {
            validationError = "Please enter a valid positive Annual CTC."
            monthlyGross = null
            weeklySalary = null
            dailySalary = null
            return
        }

        validationError = ""
        val monthly = annual / 12
        val weekly = annual / 52
        val daily = annual / 260 // approx 260 working days per year

        monthlyGross = monthly
        weeklySalary = weekly
        dailySalary = daily
        focusManager.clearFocus()

        coroutineScope.launch {
            database.historyDao().insert(
                HistoryEntry(
                    toolName = "Salary Calculator",
                    inputSummary = "Annual CTC: $currencySymbol${formatNumber(annual)}",
                    result = "Monthly: $currencySymbol${formatNumber(monthly)}, Weekly: $currencySymbol${formatNumber(weekly)}"
                )
            )
        }
    }

    fun reset() {
        ctc = ""
        monthlyGross = null
        weeklySalary = null
        dailySalary = null
        validationError = ""
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Salary Calculator") },
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
                value = ctc,
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
                    ctc = filtered
                },
                visualTransformation = remember(isIndianLocale) { NumberCommaVisualTransformation(isIndianLocale) },
                label = { Text("Annual Cost to Company (CTC)") },
                prefix = { Text("$currencySymbol ") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ctc_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text(if (isIndianLocale) "e.g. 1,200,000" else "e.g. 90,000") },
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

            if (monthlyGross != null) {
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("salary_result_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Estimated Monthly Gross Salary",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$currencySymbol ${formatNumber(monthlyGross!!)}",
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
                            text = "Payout Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Annual CTC", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol ${formatNumber(ctc.toDoubleOrNull() ?: 0.0)}", fontWeight = FontWeight.Medium)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Monthly Pay", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol ${formatNumber(monthlyGross!!)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Weekly Pay", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol ${formatNumber(weeklySalary!!)}", fontWeight = FontWeight.Medium)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Daily Pay (approx)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol ${formatNumber(dailySalary!!)}", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

