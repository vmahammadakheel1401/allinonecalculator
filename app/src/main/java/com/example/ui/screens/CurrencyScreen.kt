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
import androidx.compose.ui.res.stringResource
import com.example.R
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
import com.example.storage.SettingsManager
import com.example.utilities.NumberCommaVisualTransformation
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyScreen(navController: NavController, database: AppDatabase, settingsManager: SettingsManager) {
    val currencies by remember { 
        derivedStateOf { 
            CurrencyConverter.getSupportedCurrencies()
        } 
    }

    val localeCurrencyCode = remember {
        try {
            java.util.Currency.getInstance(java.util.Locale.getDefault()).currencyCode
        } catch (e: Exception) {
            "USD"
        }
    }
    
    val initialFromCurrency = if (currencies.contains(localeCurrencyCode)) localeCurrencyCode else "USD"
    
    var fromCurrency by remember { mutableStateOf(initialFromCurrency) }
    var toCurrency by remember { mutableStateOf("INR") }
    
    var fromValue by remember { mutableStateOf("1") }
    var toValue by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    fun performConversion() {
        if (isLoading || loadError) return
        val value = fromValue.toDoubleOrNull()
        if (value != null) {
            val result = CurrencyConverter.convert(value, fromCurrency, toCurrency)
            toValue = if (result == 0.0) "0" else {
                val absResult = Math.abs(result)
                when {
                    absResult < 0.0001 -> "%.6f".format(result)
                    absResult < 1 -> "%.4f".format(result)
                    absResult < 1000 -> "%.2f".format(result)
                    else -> "%.2f".format(result)
                }
            }
        } else {
            toValue = ""
        }
    }

    suspend fun loadRates() {
        isLoading = true
        loadError = false
        val success = CurrencyConverter.fetchLiveRates()
        isLoading = false
        loadError = !success
        if (success) performConversion()
    }

    LaunchedEffect(Unit) {
        loadRates()
    }

    LaunchedEffect(fromValue, fromCurrency, toCurrency, CurrencyConverter.rates.size) {
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (loadError) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Unable to load current exchange rates.", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { coroutineScope.launch { loadRates() } }) { Text("Retry") }
                }
            } else {
                val timestamp = CurrencyConverter.ratesTimestamp.value
                val dateStr = timestamp?.let { SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(it * 1000)) } ?: "Unknown"
                Text(
                    "Rates updated: $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.currency_indicative_notice),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CurrencyCard(
                        label = "FROM",
                        value = fromValue,
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
                            fromValue = filtered
                        },
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
                            performConversion()
                            
                            val v = fromValue.toDoubleOrNull()
                            if (v != null) {
                                coroutineScope.launch {
                                    database.historyDao().insert(
                                        HistoryEntry(
                                            toolName = "Currency",
                                            inputSummary = "$fromValue $fromCurrency to $toCurrency",
                                            result = toValue
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
                    visualTransformation = remember(currency) { NumberCommaVisualTransformation(currency == "INR") },
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
                            text = "${currency} (${CurrencyConverter.getCurrencyDisplayInfo(currency).symbol ?: ""})",
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
                            val info = CurrencyConverter.getCurrencyDisplayInfo(c)
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text("${info.region} — ${info.name} ${info.symbol?.let { "($it)" } ?: ""}")
                                        Text(info.code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
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
