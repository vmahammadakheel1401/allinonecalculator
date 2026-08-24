package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.storage.AppDatabase
import com.example.storage.SettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, settingsManager: SettingsManager, database: AppDatabase) {
    val theme by settingsManager.themeFlow.collectAsState(initial = "System default")
    val currency by settingsManager.currencyFlow.collectAsState(initial = "USD ($)")
    val unitSystem by settingsManager.unitSystemFlow.collectAsState(initial = "Metric")
    val precision by settingsManager.precisionFlow.collectAsState(initial = 2)
    
    val coroutineScope = rememberCoroutineScope()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }
    var showPrecisionDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var userRating by remember { mutableStateOf(5) }
    var ratingSubmitted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text(theme) },
                modifier = Modifier.clickable { showThemeDialog = true }
            )
            Divider()
            ListItem(
                headlineContent = { Text("Default Currency") },
                supportingContent = { Text(currency) },
                modifier = Modifier.clickable { showCurrencyDialog = true }
            )
            Divider()
            ListItem(
                headlineContent = { Text("Default Unit System") },
                supportingContent = { Text(unitSystem) },
                modifier = Modifier.clickable { showUnitDialog = true }
            )
            Divider()
            ListItem(
                headlineContent = { Text("Decimal Precision") },
                supportingContent = { Text(precision.toString()) },
                modifier = Modifier.clickable { showPrecisionDialog = true }
            )
            Divider()
            ListItem(
                headlineContent = { Text("Clear History") },
                supportingContent = { Text("Remove all calculation records") },
                modifier = Modifier.clickable { showClearHistoryDialog = true }
            )
            Divider()
            ListItem(
                headlineContent = { Text("Privacy Policy") },
                modifier = Modifier.clickable { showPrivacyDialog = true }
            )
            Divider()
            ListItem(
                headlineContent = { Text("Rate the App") },
                supportingContent = { Text("Leave your rating and feedback") },
                modifier = Modifier.clickable { 
                    ratingSubmitted = false
                    showRateDialog = true 
                }
            )
            Divider()
            ListItem(
                headlineContent = { Text("About") },
                supportingContent = { Text("All-in-One Calculator v1.0") },
                modifier = Modifier.clickable { showAboutDialog = true }
            )
        }
        
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Choose Theme") },
                text = {
                    Column {
                        listOf("Light", "Dark", "System default").forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch { settingsManager.setTheme(option) }
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                RadioButton(
                                    selected = theme == option,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
        
        if (showCurrencyDialog) {
            AlertDialog(
                onDismissRequest = { showCurrencyDialog = false },
                title = { Text("Default Currency") },
                text = {
                    Column {
                        listOf("USD (\$)", "EUR (€)", "GBP (£)", "JPY (¥)", "INR (₹)").forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch { settingsManager.setCurrency(option) }
                                        showCurrencyDialog = false
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                RadioButton(
                                    selected = currency == option,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
        
        if (showUnitDialog) {
            AlertDialog(
                onDismissRequest = { showUnitDialog = false },
                title = { Text("Default Unit System") },
                text = {
                    Column {
                        listOf("Metric", "Imperial").forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch { settingsManager.setUnitSystem(option) }
                                        showUnitDialog = false
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                RadioButton(
                                    selected = unitSystem == option,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
        
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("About") },
                text = { 
                    Text("All-in-One Calculator\nVersion 1.0\n\nA comprehensive multi-tool calculator app featuring a variety of utilities for daily use, designed with Material 3.") 
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
        
        if (showPrecisionDialog) {
            AlertDialog(
                onDismissRequest = { showPrecisionDialog = false },
                title = { Text("Decimal Precision") },
                text = {
                    Column {
                        (0..5).forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch { settingsManager.setPrecision(option) }
                                        showPrecisionDialog = false
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                RadioButton(
                                    selected = precision == option,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option.toString())
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
        
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Privacy Policy & Data Safety") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "1. Zero Personal Data Collection",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "This application does not collect, store, or transmit any personal identifiable information, device identifiers, or user analytics.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "2. On-Device Storage",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "All calculation history, custom units, and user preferences are stored strictly on your local device using secure encrypted Android SQLite/Room database storage.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "3. Network Usage",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Network access is used solely to fetch updated indicative exchange rates for the currency converter via public APIs. No user data is sent with these requests.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text("I Understand")
                    }
                }
            )
        }
        
        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("Clear History") },
                text = { Text("Are you sure you want to delete all history? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            database.historyDao().clearAll()
                        }
                        showClearHistoryDialog = false
                    }) {
                        Text("Clear All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showRateDialog) {
            AlertDialog(
                onDismissRequest = { showRateDialog = false },
                title = { Text("Rate the App") },
                text = {
                    if (ratingSubmitted) {
                        Text("Thank you for your rating and feedback! ⭐")
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("How would you rate your experience?")
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                (1..5).forEach { star ->
                                    IconButton(onClick = { userRating = star }) {
                                        Icon(
                                            imageVector = if (star <= userRating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                            contentDescription = "$star Star",
                                            tint = if (star <= userRating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (ratingSubmitted) {
                        TextButton(onClick = { showRateDialog = false }) {
                            Text("Close")
                        }
                    } else {
                        TextButton(onClick = { ratingSubmitted = true }) {
                            Text("Submit")
                        }
                    }
                },
                dismissButton = {
                    if (!ratingSubmitted) {
                        TextButton(onClick = { showRateDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}
