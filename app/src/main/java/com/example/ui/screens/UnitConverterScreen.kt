package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.calculators.UnitCategory
import com.example.calculators.UnitConverter
import com.example.calculators.UnitItem
import com.example.models.HistoryEntry
import com.example.storage.AppDatabase
import com.example.storage.SettingsManager
import com.example.utilities.NumberCommaVisualTransformation
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(
    navController: NavController,
    database: AppDatabase,
    settingsManager: SettingsManager? = null
) {
    var selectedCategory by remember { mutableStateOf(UnitCategory.Length) }
    val unitsForCategory = remember(selectedCategory) { UnitConverter.getUnitsForCategory(selectedCategory) }

    var fromUnit by remember(selectedCategory) { mutableStateOf(unitsForCategory.first()) }
    var toUnit by remember(selectedCategory) { mutableStateOf(unitsForCategory.drop(1).firstOrNull() ?: unitsForCategory.first()) }

    var fromValue by remember { mutableStateOf("1") }
    var toValue by remember { mutableStateOf("") }

    val decimalFormat = remember {
        DecimalFormat("#,##0.########", DecimalFormatSymbols(Locale.US)).apply {
            isGroupingUsed = false
        }
    }
    val coroutineScope = rememberCoroutineScope()

    val currency by settingsManager?.currencyFlow?.collectAsState(initial = "USD ($)") ?: remember { mutableStateOf("USD ($)") }
    val isIndianLocale = currency.contains("INR")

    fun performConversion() {
        val cleanInput = fromValue.trim()
        if (cleanInput.isEmpty() || cleanInput == "-" || cleanInput == "." || cleanInput == "-.") {
            toValue = ""
            return
        }

        val value = cleanInput.toDoubleOrNull()
        if (value != null) {
            try {
                val result = UnitConverter.convert(value, fromUnit, toUnit)
                toValue = decimalFormat.format(result)
            } catch (_: Exception) {
                toValue = "Error"
            }
        } else {
            toValue = ""
        }
    }

    LaunchedEffect(fromValue, fromUnit, toUnit) {
        performConversion()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unit Converter") },
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
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category selector tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(UnitCategory.entries.toTypedArray()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = category
                            fromValue = if (category == UnitCategory.Temperature) "100" else "1"
                        },
                        label = { Text(category.name) },
                        leadingIcon = if (category == UnitCategory.Temperature) {
                            { Icon(Icons.Outlined.Thermostat, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Quick Presets for Temperature
            if (selectedCategory == UnitCategory.Temperature) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Common Temperature Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val tempPresets = when (fromUnit.id) {
                            "c" -> listOf(
                                "-273.15" to "Absolute Zero (-273.15°C)",
                                "-40" to "Equal Point (-40°C)",
                                "0" to "Freezing (0°C)",
                                "20" to "Room Temp (20°C)",
                                "37" to "Body Temp (37°C)",
                                "100" to "Boiling (100°C)"
                            )
                            "f" -> listOf(
                                "-459.67" to "Absolute Zero (-459.67°F)",
                                "-40" to "Equal Point (-40°F)",
                                "32" to "Freezing (32°F)",
                                "68" to "Room Temp (68°F)",
                                "98.6" to "Body Temp (98.6°F)",
                                "212" to "Boiling (212°F)"
                            )
                            "k" -> listOf(
                                "0" to "Absolute Zero (0 K)",
                                "233.15" to "Equal Point (233.15 K)",
                                "273.15" to "Freezing (273.15 K)",
                                "293.15" to "Room Temp (293.15 K)",
                                "310.15" to "Body Temp (310.15 K)",
                                "373.15" to "Boiling (373.15 K)"
                            )
                            else -> listOf(
                                "0" to "0°R",
                                "491.67" to "Freezing (491.67°R)",
                                "671.67" to "Boiling (671.67°R)"
                            )
                        }

                        items(tempPresets) { (presetVal, label) ->
                            FilterChip(
                                selected = fromValue == presetVal,
                                onClick = { fromValue = presetVal },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // Converter Input & Output Card Stack
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // FROM CARD
                    UnitCard(
                        label = "FROM",
                        value = fromValue,
                        onValueChange = { input ->
                            // Allow negative sign, digits, and a single decimal point
                            val filtered = buildString {
                                var hasDecimal = false
                                var hasMinus = false
                                for (char in input) {
                                    if (char == '-' && length == 0 && !hasMinus) {
                                        append(char)
                                        hasMinus = true
                                    } else if (char.isDigit()) {
                                        append(char)
                                    } else if (char == '.' && !hasDecimal) {
                                        append(char)
                                        hasDecimal = true
                                    }
                                }
                            }
                            fromValue = filtered
                        },
                        onToggleSign = {
                            fromValue = when {
                                fromValue.startsWith("-") -> fromValue.removePrefix("-")
                                fromValue.isNotEmpty() && fromValue != "0" -> "-$fromValue"
                                else -> "-"
                            }
                        },
                        onClear = { fromValue = "" },
                        unit = fromUnit,
                        units = unitsForCategory,
                        onUnitChange = { fromUnit = it },
                        isIndian = isIndianLocale,
                        isInput = true,
                        allowNegative = selectedCategory == UnitCategory.Temperature
                    )

                    // TO CARD
                    UnitCard(
                        label = "TO",
                        value = toValue,
                        onValueChange = {},
                        onToggleSign = {},
                        onClear = {},
                        unit = toUnit,
                        units = unitsForCategory,
                        onUnitChange = { toUnit = it },
                        readOnly = true,
                        isIndian = isIndianLocale,
                        isInput = false,
                        allowNegative = selectedCategory == UnitCategory.Temperature
                    )
                }

                // Center Swap Button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .clickable {
                            val tempUnit = fromUnit
                            fromUnit = toUnit
                            toUnit = tempUnit

                            if (toValue.isNotEmpty() && toValue != "Error") {
                                fromValue = toValue
                            }

                            // Save to history on manual swap
                            val v = fromValue.toDoubleOrNull()
                            if (v != null) {
                                coroutineScope.launch {
                                    database.historyDao().insert(
                                        HistoryEntry(
                                            toolName = "Unit Converter",
                                            inputSummary = "$fromValue ${fromUnit.name} (${fromUnit.symbol}) to ${toUnit.name} (${toUnit.symbol})",
                                            result = UnitConverter.convert(v, fromUnit, toUnit).let { decimalFormat.format(it) }
                                        )
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapVert,
                        contentDescription = "Swap Units",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Formula and Details Box
            if (selectedCategory == UnitCategory.Temperature) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Formula",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = UnitConverter.getTemperatureFormula(fromUnit, toUnit),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onToggleSign: () -> Unit,
    onClear: () -> Unit,
    unit: UnitItem,
    units: List<UnitItem>,
    onUnitChange: (UnitItem) -> Unit,
    readOnly: Boolean = false,
    isIndian: Boolean = false,
    isInput: Boolean = false,
    allowNegative: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Unit Selector Dropdown trigger
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .clickable { expanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${unit.name} (${unit.symbol})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Select Unit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        units.forEach { u ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(u.name, fontWeight = if (u.id == unit.id) FontWeight.Bold else FontWeight.Normal)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(u.symbol, color = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    onUnitChange(u)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontSize = if (value.length > 10) 24.sp else 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    visualTransformation = remember(isIndian) { NumberCommaVisualTransformation(isIndian) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    readOnly = readOnly,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty() && isInput) {
                            Text(
                                text = "0",
                                style = TextStyle(
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            )
                        }
                        innerTextField()
                    }
                )

                if (isInput) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (allowNegative) {
                            IconButton(
                                onClick = onToggleSign,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "±",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (value.isNotEmpty()) {
                            IconButton(
                                onClick = onClear,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
