package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.storage.AppDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController, database: AppDatabase) {
    val historyEntries by database.historyDao().getAllHistory().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

    fun navigateToTool(toolName: String) {
        val route = when {
            toolName.contains("Calculator", ignoreCase = true) -> Screen.Calculator.route
            toolName.contains("Currency", ignoreCase = true) -> Screen.Currency.route
            toolName.contains("Unit", ignoreCase = true) -> Screen.UnitConverter.route
            toolName.contains("Discount", ignoreCase = true) -> Screen.Discount.route
            toolName.contains("Loan", ignoreCase = true) -> Screen.Loan.route
            toolName.contains("Date", ignoreCase = true) -> Screen.DateCalc.route
            toolName.contains("Age", ignoreCase = true) -> Screen.Age.route
            toolName.contains("BMI", ignoreCase = true) -> Screen.BMI.route
            toolName.contains("World", ignoreCase = true) || toolName.contains("Clock", ignoreCase = true) -> Screen.WorldClock.route
            else -> null
        }
        if (route != null) {
            navController.navigate(route)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                actions = {
                    if (historyEntries.isNotEmpty()) {
                        IconButton(onClick = { showDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Clear All")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (historyEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No history yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyEntries, key = { it.id }) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navigateToTool(entry.toolName) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = entry.toolName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = entry.inputSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.result,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
        
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Clear History") },
                text = { Text("Are you sure you want to delete all history? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            database.historyDao().clearAll()
                        }
                        showDialog = false
                    }) {
                        Text("Clear All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
