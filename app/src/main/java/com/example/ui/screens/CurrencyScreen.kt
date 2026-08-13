package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.calculators.CurrencyConverter
import com.example.models.HistoryEntry
import com.example.storage.AppDatabase
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyScreen(navController: NavController, database: AppDatabase) {
    val currencies by remember { 
        derivedStateOf { 
            val list = CurrencyConverter.rates.keys.toList().sorted()
            listOf("USD") + list.filter { it != "USD" }
        } 
    }
    
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("INR") }
    
    var fromValue by remember { mutableStateOf("1") }
    var toValue by remember { mutableStateOf("") }
    
    val decimalFormat = remember { DecimalFormat("#.######") }
    val coroutineScope = rememberCoroutineScope()
    
    fun performConversion() {
        val value = fromValue.toDoubleOrNull()
        if (value != null) {
            val result = CurrencyConverter.convert(value, fromCurrency, toCurrency)
            toValue = decimalFormat.format(result)
        } else {
            toValue = ""
        }
    }

    LaunchedEffect(Unit) {
        CurrencyConverter.fetchLiveRates()
    }

    LaunchedEffect(fromValue, fromCurrency, toCurrency, CurrencyConverter.isLive.value) {
        performConversion()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Currency Converter") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
            Text(
                if (CurrencyConverter.isLive.value) "Using live market rates." else "Offline reference rates. May not reflect current market rates.",
                style = MaterialTheme.typography.bodySmall,
                color = if (CurrencyConverter.isLive.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CurrencyCard(
                        label = "FROM",
                        value = fromValue,
                        onValueChange = { fromValue = it },
                        currency = fromCurrency,
                        currencies = currencies,
                        onCurrencyChange = { fromCurrency = it }
                    )
                    
                    CurrencyCard(
                        label = "TO",
                        value = toValue,
                        onValueChange = {}, 
                        currency = toCurrency,
                        currencies = currencies,
                        onCurrencyChange = { toCurrency = it },
                        readOnly = true
                    )
                }
                
                // Swap Button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-4).dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable {
                            val tempCurrency = fromCurrency
                            fromCurrency = toCurrency
                            toCurrency = tempCurrency
                            
                            val v = fromValue.toDoubleOrNull()
                            if (v != null) {
                                coroutineScope.launch {
                                    database.historyDao().insert(
                                        HistoryEntry(
                                            toolName = "Currency",
                                            inputSummary = "$fromValue $fromCurrency to $toCurrency",
                                            result = CurrencyConverter.convert(v, fromCurrency, toCurrency).let { decimalFormat.format(it) }
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
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun CurrencyCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    currency: String,
    currencies: List<String>,
    onCurrencyChange: (String) -> Unit,
    readOnly: Boolean = false
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                            text = currency,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Select Currency",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        currencies.forEach { c ->
                            val fullName = CurrencyConverter.getCurrencyName(c)
                            DropdownMenuItem(
                                text = { Text("$c - $fullName") },
                                onClick = {
                                    onCurrencyChange(c)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
        }
    }
}
