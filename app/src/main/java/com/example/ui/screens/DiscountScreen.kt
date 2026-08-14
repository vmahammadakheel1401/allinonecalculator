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
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscountScreen(navController: NavController, database: AppDatabase, settingsManager: SettingsManager) {
    var originalPrice by remember { mutableStateOf("") }
    var discountPercent by remember { mutableStateOf("") }
    var additionalDiscount by remember { mutableStateOf("") }

    var result by remember { mutableStateOf<FinanceCalculators.DiscountResult?>(null) }
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
        val op = originalPrice.toDoubleOrNull()
        val dp = discountPercent.toDoubleOrNull()
        val ad = additionalDiscount.toDoubleOrNull()

        if (op == null || dp == null) {
            errorMessage = "Please enter valid numbers for price and discount"
            result = null
            return
        }
        if (op < 0 || dp < 0 || (ad != null && ad < 0)) {
            errorMessage = "Values cannot be negative"
            result = null
            return
        }
        
        if (dp > 100 || (ad != null && ad > 100)) {
             errorMessage = "Discount cannot exceed 100%"
             result = null
             return
        }

        errorMessage = null
        val res = FinanceCalculators.calculateDiscount(
            FinanceCalculators.DiscountInput(op, dp, ad)
        )
        result = res

        coroutineScope.launch {
            database.historyDao().insert(
                HistoryEntry(
                    toolName = "Discount Calculator",
                    inputSummary = "Price: ${currencyFormat.format(op)}, Disc: $dp%",
                    result = "Final: ${currencyFormat.format(res.finalPrice)}"
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discount Calculator") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
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
                value = originalPrice,
                onValueChange = { originalPrice = it },
                label = { Text("Original Price ($currencySymbol)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = discountPercent,
                onValueChange = { discountPercent = it },
                label = { Text("Discount Percentage (%)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = additionalDiscount,
                onValueChange = { additionalDiscount = it },
                label = { Text("Additional Discount (Optional %)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        originalPrice = ""
                        discountPercent = ""
                        additionalDiscount = ""
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Final Price",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currencySymbol ${currencyFormat.format(result!!.finalPrice)}",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                "Total Savings", 
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                            Text(
                                "$currencySymbol ${currencyFormat.format(result!!.amountSaved)}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
