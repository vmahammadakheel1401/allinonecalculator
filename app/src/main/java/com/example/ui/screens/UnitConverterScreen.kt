package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(navController: NavController, database: AppDatabase, settingsManager: SettingsManager? = null) {
    var selectedCategory by remember { mutableStateOf(UnitCategory.Length) }
    
    val unitsForCategory = UnitConverter.getUnitsForCategory(selectedCategory)
    
    var fromUnit by remember(selectedCategory) { mutableStateOf(unitsForCategory.first()) }
    var toUnit by remember(selectedCategory) { mutableStateOf(unitsForCategory.drop(1).firstOrNull() ?: unitsForCategory.first()) }
    
    var fromValue by remember { mutableStateOf("1") }
    var toValue by remember { mutableStateOf("") }

    val decimalFormat = remember { DecimalFormat("#.########") }
    val coroutineScope = rememberCoroutineScope()

    val currency by settingsManager?.currencyFlow?.collectAsState(initial = "USD ($)") ?: remember { mutableStateOf("USD ($)") }
    val isIndianLocale = currency.contains("INR")

    fun performConversion() {
        val value = fromValue.toDoubleOrNull()
        if (value != null) {
            val result = UnitConverter.convert(value, fromUnit, toUnit)
            toValue = decimalFormat.format(result)
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
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(UnitCategory.entries.toTypedArray()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.name) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UnitCard(
                        label = "FROM",
                        value = fromValue,
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
                            fromValue = filtered
                        },
                        unit = fromUnit,
                        units = unitsForCategory,
                        onUnitChange = { fromUnit = it },
                        isIndian = isIndianLocale
                    )
                    UnitCard(
                        label = "TO",
                        value = toValue,
                        onValueChange = {}, // Read-only via UI
                        unit = toUnit,
                        units = unitsForCategory,
                        onUnitChange = { toUnit = it },
                        readOnly = true,
                        isIndian = isIndianLocale
                    )
                }

                // Swap Button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-4).dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            val tempUnit = fromUnit
                            fromUnit = toUnit
                            toUnit = tempUnit
                            
                            // Save to history on manual swap as a discrete event
                            val v = fromValue.toDoubleOrNull()
                            if (v != null) {
                                coroutineScope.launch {
                                    database.historyDao().insert(
                                        HistoryEntry(
                                            toolName = "Unit Converter",
                                            inputSummary = "$fromValue ${fromUnit.name} to ${toUnit.name}",
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
                        contentDescription = "Swap",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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
    unit: UnitItem,
    units: List<UnitItem>,
    onUnitChange: (UnitItem) -> Unit,
    readOnly: Boolean = false,
    isIndian: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
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
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    visualTransformation = remember(isIndian) { NumberCommaVisualTransformation(isIndian) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    readOnly = readOnly,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { expanded = true }
                    ) {
                        Text(
                            text = unit.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Select Unit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        units.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u.name) },
                                onClick = {
                                    onUnitChange(u)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        }
    }
}
