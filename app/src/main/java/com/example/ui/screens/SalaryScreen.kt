package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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

enum class SalaryInputType(val label: String) {
    ANNUAL("Annual"),
    MONTHLY("Monthly"),
    HOURLY("Hourly")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryScreen(
    navController: NavHostController,
    database: AppDatabase,
    settingsManager: SettingsManager
) {
    var inputType by remember { mutableStateOf(SalaryInputType.ANNUAL) }
    var rawAmountInput by remember { mutableStateOf("") }

    // Work schedule settings
    var hoursPerWeekInput by remember { mutableStateOf("40") }
    var daysPerWeekInput by remember { mutableStateOf("5") }

    // Deductions / Take-Home Settings
    var showDeductions by remember { mutableStateOf(false) }
    var taxRateInput by remember { mutableStateOf("15") }
    var retirementRateInput by remember { mutableStateOf("5") }
    var otherMonthlyDeductionsInput by remember { mutableStateOf("0") }

    var validationError by remember { mutableStateOf("") }
    var hasCalculated by remember { mutableStateOf(false) }

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

    val amountValue = rawAmountInput.toDoubleOrNull() ?: 0.0
    val hoursPerWeek = (hoursPerWeekInput.toDoubleOrNull() ?: 40.0).coerceIn(1.0, 168.0)
    val daysPerWeek = (daysPerWeekInput.toDoubleOrNull() ?: 5.0).coerceIn(1.0, 7.0)

    val taxRate = (taxRateInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val retirementRate = (retirementRateInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val otherMonthlyDeduction = otherMonthlyDeductionsInput.toDoubleOrNull() ?: 0.0

    // Compute Annual Base Gross
    val annualGross = when (inputType) {
        SalaryInputType.ANNUAL -> amountValue
        SalaryInputType.MONTHLY -> amountValue * 12.0
        SalaryInputType.HOURLY -> amountValue * hoursPerWeek * 52.0
    }

    val isValid = amountValue > 0.0

    // Payout Gross Figures
    val monthlyGross = annualGross / 12.0
    val semiMonthlyGross = annualGross / 24.0
    val biWeeklyGross = annualGross / 26.0
    val weeklyGross = annualGross / 52.0
    val totalWorkDaysPerYear = daysPerWeek * 52.0
    val dailyGross = if (totalWorkDaysPerYear > 0) annualGross / totalWorkDaysPerYear else 0.0
    val totalWorkHoursPerYear = hoursPerWeek * 52.0
    val hourlyGross = if (totalWorkHoursPerYear > 0) annualGross / totalWorkHoursPerYear else 0.0

    // Deductions & Net Take-Home Calculations
    val annualTax = annualGross * (taxRate / 100.0)
    val annualRetirement = annualGross * (retirementRate / 100.0)
    val annualOtherDeductions = otherMonthlyDeduction * 12.0
    val totalAnnualDeductions = annualTax + annualRetirement + annualOtherDeductions
    val annualNet = (annualGross - totalAnnualDeductions).coerceAtLeast(0.0)

    val monthlyNet = annualNet / 12.0
    val biWeeklyNet = annualNet / 26.0
    val weeklyNet = annualNet / 52.0
    val hourlyNet = if (totalWorkHoursPerYear > 0) annualNet / totalWorkHoursPerYear else 0.0

    fun saveHistory() {
        if (!isValid) return
        coroutineScope.launch {
            database.historyDao().insert(
                HistoryEntry(
                    toolName = "Salary Calculator",
                    inputSummary = "${inputType.label}: $currencySymbol${formatNumber(amountValue)}",
                    result = "Gross: $currencySymbol${formatNumber(monthlyGross)}/mo | In-Hand: $currencySymbol${formatNumber(monthlyNet)}/mo"
                )
            )
        }
    }

    fun calculate() {
        if (!isValid) {
            validationError = "Please enter a valid salary amount greater than zero."
            hasCalculated = false
            return
        }
        validationError = ""
        hasCalculated = true
        focusManager.clearFocus()
        saveHistory()
    }

    fun reset() {
        rawAmountInput = ""
        inputType = SalaryInputType.ANNUAL
        hoursPerWeekInput = "40"
        daysPerWeekInput = "5"
        taxRateInput = "15"
        retirementRateInput = "5"
        otherMonthlyDeductionsInput = "0"
        showDeductions = false
        validationError = ""
        hasCalculated = false
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Salary & Take-Home Pay") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Mode Selector (Annual, Monthly, Hourly)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Pay Basis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SalaryInputType.values().forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = inputType == type,
                                onClick = {
                                    inputType = type
                                    if (validationError.isNotEmpty()) validationError = ""
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = SalaryInputType.values().size)
                            ) {
                                Text(type.label)
                            }
                        }
                    }

                    // Main Salary Input Field
                    OutlinedTextField(
                        value = rawAmountInput,
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
                            rawAmountInput = filtered
                            if (validationError.isNotEmpty()) validationError = ""
                        },
                        visualTransformation = remember(isIndianLocale) { NumberCommaVisualTransformation(isIndianLocale) },
                        label = {
                            Text(
                                when (inputType) {
                                    SalaryInputType.ANNUAL -> "Annual Salary / CTC"
                                    SalaryInputType.MONTHLY -> "Monthly Gross Salary"
                                    SalaryInputType.HOURLY -> "Hourly Wage / Rate"
                                }
                            )
                        },
                        placeholder = {
                            Text(
                                when (inputType) {
                                    SalaryInputType.ANNUAL -> if (isIndianLocale) "e.g. 1,200,000" else "e.g. 90,000"
                                    SalaryInputType.MONTHLY -> if (isIndianLocale) "e.g. 100,000" else "e.g. 7,500"
                                    SalaryInputType.HOURLY -> "e.g. 45"
                                }
                            )
                        },
                        trailingIcon = {
                            if (rawAmountInput.isNotEmpty()) {
                                IconButton(onClick = { rawAmountInput = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear input", modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        prefix = { Text("$currencySymbol ") },
                        suffix = {
                            Text(
                                when (inputType) {
                                    SalaryInputType.ANNUAL -> "/ yr"
                                    SalaryInputType.MONTHLY -> "/ mo"
                                    SalaryInputType.HOURLY -> "/ hr"
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ctc_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    // Quick Presets
                    Text(
                        text = "Quick Presets:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val presets = when (inputType) {
                            SalaryInputType.ANNUAL -> {
                                if (isIndianLocale) listOf("500000" to "₹5L", "800000" to "₹8L", "1200000" to "₹12L", "1800000" to "₹18L", "2500000" to "₹25L", "4000000" to "₹40L")
                                else listOf("45000" to "$45k", "60000" to "$60k", "80000" to "$80k", "100000" to "$100k", "125000" to "$125k", "150000" to "$150k")
                            }
                            SalaryInputType.MONTHLY -> {
                                if (isIndianLocale) listOf("40000" to "₹40k", "65000" to "₹65k", "100000" to "₹1L", "150000" to "₹1.5L", "200000" to "₹2L")
                                else listOf("3500" to "$3.5k", "5000" to "$5k", "7500" to "$7.5k", "10000" to "$10k", "12500" to "$12.5k")
                            }
                            SalaryInputType.HOURLY -> {
                                listOf("20" to "$20/hr", "30" to "$30/hr", "45" to "$45/hr", "60" to "$60/hr", "80" to "$80/hr", "100" to "$100/hr")
                            }
                        }

                        presets.forEach { (value, label) ->
                            item {
                                FilterChip(
                                    selected = rawAmountInput == value,
                                    onClick = {
                                        rawAmountInput = value
                                        if (validationError.isNotEmpty()) validationError = ""
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // Expandable: Deductions & Take-Home Settings
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeductions = !showDeductions },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Take-Home & Deductions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            if (showDeductions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = if (showDeductions) "Customize tax, retirement/PF, and working schedule" else "Estimated Taxes ($taxRate%), PF/401k ($retirementRate%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AnimatedVisibility(visible = showDeductions) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = taxRateInput,
                                    onValueChange = { input ->
                                        taxRateInput = input.filter { it.isDigit() || it == '.' }.take(5)
                                    },
                                    label = { Text("Income Tax %") },
                                    suffix = { Text("%") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = retirementRateInput,
                                    onValueChange = { input ->
                                        retirementRateInput = input.filter { it.isDigit() || it == '.' }.take(5)
                                    },
                                    label = { Text("PF / 401(k) %") },
                                    suffix = { Text("%") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = otherMonthlyDeductionsInput,
                                    onValueChange = { input ->
                                        otherMonthlyDeductionsInput = input.filter { it.isDigit() || it == '.' }.take(8)
                                    },
                                    label = { Text("Other Deductions") },
                                    prefix = { Text("$currencySymbol ") },
                                    suffix = { Text("/ mo") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )

                                OutlinedTextField(
                                    value = hoursPerWeekInput,
                                    onValueChange = { input ->
                                        hoursPerWeekInput = input.filter { it.isDigit() || it == '.' }.take(4)
                                    },
                                    label = { Text("Hours / Wk") },
                                    suffix = { Text("hrs") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }
                        }
                    }
                }
            }

            if (validationError.isNotEmpty()) {
                Text(
                    text = validationError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { calculate() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calculate_button")
                ) {
                    Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
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

            if (isValid) {
                // Primary Result Cards: In-Hand vs Gross
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Net In-Hand Monthly Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("salary_result_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Take-Home (In-Hand)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currencySymbol ${formatNumber(monthlyNet)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "per month",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Gross Monthly Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Gross Salary",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currencySymbol ${formatNumber(monthlyGross)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "per month",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Summary Comparison Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Annual Net Pay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol ${formatNumber(annualNet)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Divider(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Annual Deductions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol ${formatNumber(totalAnnualDeductions)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Divider(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hourly (In-Hand)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol ${formatNumber(hourlyNet)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Detailed Multi-Frequency Payout Grid Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Payout Frequency Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "In-Hand / Gross",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        PayoutRow(
                            period = "Annual (1 year)",
                            netAmount = "$currencySymbol ${formatNumber(annualNet)}",
                            grossAmount = "$currencySymbol ${formatNumber(annualGross)}"
                        )

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        PayoutRow(
                            period = "Monthly (12 paychecks/yr)",
                            netAmount = "$currencySymbol ${formatNumber(monthlyNet)}",
                            grossAmount = "$currencySymbol ${formatNumber(monthlyGross)}"
                        )

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        PayoutRow(
                            period = "Semi-Monthly (24 paychecks/yr)",
                            netAmount = "$currencySymbol ${formatNumber(annualNet / 24.0)}",
                            grossAmount = "$currencySymbol ${formatNumber(semiMonthlyGross)}"
                        )

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        PayoutRow(
                            period = "Bi-Weekly (26 paychecks/yr)",
                            netAmount = "$currencySymbol ${formatNumber(biWeeklyNet)}",
                            grossAmount = "$currencySymbol ${formatNumber(biWeeklyGross)}"
                        )

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        PayoutRow(
                            period = "Weekly (52 paychecks/yr)",
                            netAmount = "$currencySymbol ${formatNumber(weeklyNet)}",
                            grossAmount = "$currencySymbol ${formatNumber(weeklyGross)}"
                        )

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        PayoutRow(
                            period = "Daily (${daysPerWeek.toInt()} days/wk)",
                            netAmount = "$currencySymbol ${formatNumber(if (totalWorkDaysPerYear > 0) annualNet / totalWorkDaysPerYear else 0.0)}",
                            grossAmount = "$currencySymbol ${formatNumber(dailyGross)}"
                        )

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        PayoutRow(
                            period = "Hourly (${hoursPerWeek.toInt()} hrs/wk)",
                            netAmount = "$currencySymbol ${formatNumber(hourlyNet)}",
                            grossAmount = "$currencySymbol ${formatNumber(hourlyGross)}"
                        )
                    }
                }

                // Deductions Itemized Breakdown Card
                if (totalAnnualDeductions > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Monthly Deductions Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Estimated Income Tax ($taxRate%)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("- $currencySymbol ${formatNumber(annualTax / 12.0)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Retirement / PF Contribution ($retirementRate%)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("- $currencySymbol ${formatNumber(annualRetirement / 12.0)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                            }

                            if (otherMonthlyDeduction > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Other Deductions / Benefits", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("- $currencySymbol ${formatNumber(otherMonthlyDeduction)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Monthly Deductions", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("- $currencySymbol ${formatNumber(totalAnnualDeductions / 12.0)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            } else {
                // Helpful guidance card when empty
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Enter Your Salary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Type your annual, monthly, or hourly rate or select a quick preset above to view your take-home pay and full payout breakdown.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PayoutRow(
    period: String,
    netAmount: String,
    grossAmount: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1.1f)) {
            Text(
                text = period,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Column(
            modifier = Modifier.weight(0.9f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = netAmount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Gross: $grossAmount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
