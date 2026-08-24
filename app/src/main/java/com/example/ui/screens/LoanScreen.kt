package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.calculators.FinanceCalculators
import com.example.models.HistoryEntry
import com.example.storage.AppDatabase
import com.example.storage.SettingsManager
import com.example.utilities.NumberCommaVisualTransformation
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanScreen(navController: NavController, database: AppDatabase, settingsManager: SettingsManager) {
    var principal by remember { mutableStateOf("50000") }
    var interestRate by remember { mutableStateOf("5.5") }
    var termInYears by remember { mutableStateOf("5") }

    var result by remember { mutableStateOf<FinanceCalculators.LoanResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val defaultCurrency by settingsManager.currencyFlow.collectAsState(initial = "USD ($)")
    val currencySymbol = remember(defaultCurrency) {
        when {
            defaultCurrency.contains("INR") -> "₹"
            defaultCurrency.contains("EUR") -> "€"
            defaultCurrency.contains("GBP") -> "£"
            defaultCurrency.contains("JPY") -> "¥"
            else -> "$"
        }
    }
    
    val currencyFormat = remember(defaultCurrency) { java.text.DecimalFormat("#,###.##") }
    val coroutineScope = rememberCoroutineScope()

    fun calculate() {
        val p = principal.toDoubleOrNull()
        val r = interestRate.toDoubleOrNull()
        val t = termInYears.toDoubleOrNull()

        if (p == null || r == null || t == null) {
            errorMessage = "Please enter valid numbers"
            result = null
            return
        }
        if (p < 0 || r < 0 || t < 0) {
            errorMessage = "Values cannot be negative"
            result = null
            return
        }

        errorMessage = null
        val res = FinanceCalculators.calculateLoanEmi(FinanceCalculators.LoanInput(p, r, t))
        result = res

        coroutineScope.launch {
            database.historyDao().insert(
                HistoryEntry(
                    toolName = "EMI Calculator",
                    inputSummary = "Amt: ${currencyFormat.format(p)}, Rate: $r%, Term: $t yr",
                    result = "EMI: ${currencyFormat.format(res.monthlyEmi)}"
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EMI Calculator") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            OutlinedTextField(
                value = principal,
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
                    principal = filtered
                },
                visualTransformation = remember(defaultCurrency) { NumberCommaVisualTransformation(defaultCurrency.contains("INR")) },
                label = { Text("Loan Amount ($currencySymbol)") },
                prefix = { Text("$currencySymbol ") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = interestRate,
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
                    interestRate = filtered
                },
                label = { Text("Interest Rate (%)") },
                suffix = { Text("%") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = termInYears,
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
                    termInYears = filtered
                },
                label = { Text("Loan Tenure (Years)") },
                suffix = { Text("yr") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        principal = ""
                        interestRate = ""
                        termInYears = ""
                        result = null
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = { calculate() },
                    modifier = Modifier.weight(2f)
                ) {
                    Text("Calculate")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (result != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Monthly EMI",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$currencySymbol ${currencyFormat.format(result!!.monthlyEmi)}",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Interest", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                "$currencySymbol ${currencyFormat.format(result!!.totalInterest)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Payment", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                "$currencySymbol ${currencyFormat.format(result!!.totalPayment)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Google Play Financial Services disclosure
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Financial Info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Disclaimer: Calculations are estimates for informational and personal financial planning purposes only. Actual interest rates, taxes, and loan terms may vary by lending institution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
