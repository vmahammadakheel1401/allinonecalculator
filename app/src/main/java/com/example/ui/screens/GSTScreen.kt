package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
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
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSTScreen(
    navController: NavHostController,
    database: AppDatabase,
    settingsManager: SettingsManager
) {
    var amount by remember { mutableStateOf("") }
    var gstRate by remember { mutableStateOf("18") }
    var gstAmount by remember { mutableStateOf<Double?>(null) }
    var totalAmount by remember { mutableStateOf<Double?>(null) }
    var netAmount by remember { mutableStateOf<Double?>(null) }
    var lastModeIsInclusive by remember { mutableStateOf(false) }
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

    val commonGstRates = listOf("5", "12", "18", "28")

    fun calculate(isInclusive: Boolean) {
        val a = amount.toDoubleOrNull()
        val r = gstRate.toDoubleOrNull()

        if (a == null || r == null || a < 0 || r < 0) {
            validationError = "Please enter valid positive numbers for amount and GST rate."
            gstAmount = null
            totalAmount = null
            netAmount = null
            return
        }

        validationError = ""
        lastModeIsInclusive = isInclusive

        val calculatedGst: Double
        val calculatedTotal: Double
        val calculatedNet: Double

        if (isInclusive) {
            calculatedTotal = a
            calculatedGst = a - (a / (1 + r / 100))
            calculatedNet = a - calculatedGst
        } else {
            calculatedNet = a
            calculatedGst = a * (r / 100)
            calculatedTotal = a + calculatedGst
        }

        gstAmount = calculatedGst
        totalAmount = calculatedTotal
        netAmount = calculatedNet
        focusManager.clearFocus()

        coroutineScope.launch {
            val modeLabel = if (isInclusive) "Inclusive" else "Exclusive"
            database.historyDao().insert(
                HistoryEntry(
                    toolName = "GST Calculator ($modeLabel)",
                    inputSummary = "Amt: $currencySymbol${formatNumber(a)}, GST: ${formatNumber(r, 1)}%",
                    result = "GST: $currencySymbol${formatNumber(calculatedGst)}, Total: $currencySymbol${formatNumber(calculatedTotal)}"
                )
            )
        }
    }

    fun reset() {
        amount = ""
        gstRate = "18"
        gstAmount = null
        totalAmount = null
        netAmount = null
        validationError = ""
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GST Calculator") },
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
                            if (char.isDigit()) {
                                append(char)
                            } else if (char == '.' && !hasDecimal) {
                                append(char)
                                hasDecimal = true
                            }
                        }
                    }
                    amount = filtered
                },
                visualTransformation = remember(isIndianLocale) { NumberCommaVisualTransformation(isIndianLocale) },
                label = { Text("Base Amount ($currencySymbol)") },
                prefix = { Text("$currencySymbol ") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gst_amount_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text(if (isIndianLocale) "e.g. 10,000" else "e.g. 1,000") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = gstRate,
                onValueChange = { input ->
                    val filtered = buildString {
                        var hasDecimal = false
                        for (char in input) {
                            if (char.isDigit()) {
                                append(char)
                            } else if (char == '.' && !hasDecimal) {
                                append(char)
                                hasDecimal = true
                            }
                        }
                    }
                    gstRate = filtered
                },
                label = { Text("GST Rate (%)") },
                suffix = { Text("%") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gst_rate_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("18") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                commonGstRates.forEach { ratePreset ->
                    FilterChip(
                        selected = gstRate == ratePreset,
                        onClick = { gstRate = ratePreset },
                        label = { Text("$ratePreset%") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

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
                    onClick = { calculate(false) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_gst_button")
                ) {
                    Text("+ Add GST")
                }

                Button(
                    onClick = { calculate(true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("remove_gst_button")
                ) {
                    Text("- Remove GST")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { reset() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_button")
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset")
            }

            if (totalAmount != null && gstAmount != null && netAmount != null) {
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gst_result_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (lastModeIsInclusive) "Total Amount (GST Included)" else "Total Amount (with GST Added)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$currencySymbol ${formatNumber(totalAmount!!)}",
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
                            text = "Calculation Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        GstRow(
                            label = "Net / Base Amount",
                            value = "$currencySymbol ${formatNumber(netAmount!!)}"
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        GstRow(
                            label = "GST Amount (${gstRate}%)",
                            value = "+ $currencySymbol ${formatNumber(gstAmount!!)}"
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        GstRow(
                            label = "CGST (${(gstRate.toDoubleOrNull() ?: 0.0) / 2}%)",
                            value = "$currencySymbol ${formatNumber(gstAmount!! / 2)}"
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        GstRow(
                            label = "SGST (${(gstRate.toDoubleOrNull() ?: 0.0) / 2}%)",
                            value = "$currencySymbol ${formatNumber(gstAmount!! / 2)}"
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        GstRow(
                            label = "Total Final Price",
                            value = "$currencySymbol ${formatNumber(totalAmount!!)}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GstRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

