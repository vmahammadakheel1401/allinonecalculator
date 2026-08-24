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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.models.HistoryEntry
import com.example.storage.AppDatabase
import com.example.storage.SettingsManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class NumberCommaVisualTransformation(private val isIndian: Boolean) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val parts = originalText.split(".")
        val intPart = parts[0]
        val fracPart = if (parts.size > 1) parts[1] else null
        val hasTrailingDot = originalText.endsWith(".")

        val formattedInt = StringBuilder()
        val intLen = intPart.length

        if (isIndian) {
            // Indian numbering: last 3 digits, then groups of 2
            var count = 0
            for (i in intLen - 1 downTo 0) {
                formattedInt.append(intPart[i])
                count++
                if (count == 3 && i > 0) {
                    formattedInt.append(',')
                } else if (count > 3 && (count - 3) % 2 == 0 && i > 0) {
                    formattedInt.append(',')
                }
            }
            formattedInt.reverse()
        } else {
            // International standard: groups of 3
            var count = 0
            for (i in intLen - 1 downTo 0) {
                formattedInt.append(intPart[i])
                count++
                if (count % 3 == 0 && i > 0) {
                    formattedInt.append(',')
                }
            }
            formattedInt.reverse()
        }

        val formattedString = buildString {
            append(formattedInt)
            if (hasTrailingDot) {
                append('.')
            }
            if (fracPart != null) {
                append('.')
                append(fracPart)
            }
        }

        // Build 1-to-1 offset mapping
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val clampedOffset = offset.coerceIn(0, originalText.length)
                
                var transformed = 0
                var originalSeen = 0

                for (char in formattedString) {
                    if (originalSeen == clampedOffset) break
                    if (char != ',') {
                        originalSeen++
                    }
                    transformed++
                }
                return transformed
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val clampedOffset = offset.coerceIn(0, formattedString.length)
                
                var originalCount = 0
                for (i in 0 until clampedOffset) {
                    if (formattedString[i] != ',') {
                        originalCount++
                    }
                }
                return originalCount.coerceIn(0, originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedString), offsetMapping)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreelanceRateScreen(
    navController: NavHostController,
    database: AppDatabase,
    settingsManager: SettingsManager
) {
    var income by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf<Double?>(null) }
    var calculatedIncome by remember { mutableStateOf(0.0) }
    var calculatedHours by remember { mutableStateOf(0.0) }
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
        val i = income.toDoubleOrNull()
        val h = hours.toDoubleOrNull()

        if (i == null || h == null || i <= 0 || h <= 0) {
            validationError = "Please enter valid positive numbers for income and working hours."
            rate = null
        } else if (h > 744) {
            validationError = "Monthly working hours cannot exceed 744 hours (31 days × 24 hours)."
            rate = null
        } else {
            validationError = ""
            val calculatedRate = i / h
            rate = calculatedRate
            calculatedIncome = i
            calculatedHours = h
            focusManager.clearFocus()

            coroutineScope.launch {
                database.historyDao().insert(
                    HistoryEntry(
                        toolName = "Freelance Rate",
                        inputSummary = "$currencySymbol${formatNumber(i)} / mo, ${formatNumber(h, 0)} hrs",
                        result = "$currencySymbol ${formatNumber(calculatedRate)} / hr"
                    )
                )
            }
        }
    }

    fun reset() {
        income = ""
        hours = ""
        rate = null
        validationError = ""
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Freelance Rate Calculator") },
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
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Calculate your ideal hourly billing rate based on your monthly revenue goals and billable working hours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = income,
                onValueChange = { input ->
                    // Allow only digits and a single decimal point
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
                    income = filtered
                },
                visualTransformation = remember(isIndianLocale) { NumberCommaVisualTransformation(isIndianLocale) },
                label = { Text("Desired Monthly Income ($currencySymbol)") },
                prefix = { Text("$currencySymbol ") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("monthly_income_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text(if (isIndianLocale) "e.g. 100,000" else "e.g. 5,000") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = hours,
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
                    val numericVal = filtered.toDoubleOrNull()
                    if (numericVal == null || numericVal <= 744.0) {
                        hours = filtered
                        if (validationError.contains("744")) {
                            validationError = ""
                        }
                    } else {
                        validationError = "Monthly hours cannot exceed 744 hours (max 31 days × 24 hrs)."
                    }
                },
                visualTransformation = remember(isIndianLocale) { NumberCommaVisualTransformation(isIndianLocale) },
                label = { Text("Monthly Billable Hours") },
                supportingText = { Text("Maximum 744 hours (31 days × 24 hrs)") },
                suffix = { Text("hrs") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("monthly_hours_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("e.g. 160") },
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
                OutlinedButton(
                    onClick = { reset() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reset_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset")
                }

                Button(
                    onClick = { calculate() },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("calculate_button")
                ) {
                    Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Calculate")
                }
            }

            rate?.let { r ->
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("result_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Recommended Hourly Rate",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$currencySymbol ${formatNumber(r)} / hr",
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
                            text = "Rate Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        RateRow(label = "Daily Rate (8 hours)", value = "$currencySymbol ${formatNumber(r * 8)}")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        RateRow(label = "Weekly Rate (40 hours)", value = "$currencySymbol ${formatNumber(r * 40)}")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        RateRow(label = "Monthly Target", value = "$currencySymbol ${formatNumber(calculatedIncome)}")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        RateRow(label = "Annual Target", value = "$currencySymbol ${formatNumber(calculatedIncome * 12)}")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        RateRow(label = "Working Hours", value = "${formatNumber(calculatedHours, 0)} hrs/month")
                    }
                }
            }
        }
    }
}

@Composable
private fun RateRow(label: String, value: String) {
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

