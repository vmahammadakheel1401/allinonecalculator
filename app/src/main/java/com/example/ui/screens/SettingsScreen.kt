package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    
    val coroutineScope = rememberCoroutineScope()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

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
                headlineContent = { Text("Clear History") },
                supportingContent = { Text("Remove all calculation records") },
                modifier = Modifier.clickable { showClearHistoryDialog = true }
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
    }
}
