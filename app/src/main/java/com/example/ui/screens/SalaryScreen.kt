package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.storage.AppDatabase
import com.example.storage.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryScreen(navController: NavHostController, database: AppDatabase, settingsManager: SettingsManager) {
    var ctc by remember { mutableStateOf("") }
    var monthlySalary by remember { mutableStateOf(0.0) }
    
    val currency by settingsManager.currencyFlow.collectAsState(initial = "USD ($)")
    val currencySymbol = remember(currency) {
        val start = currency.indexOf("(")
        val end = currency.indexOf(")")
        if (start != -1 && end != -1) currency.substring(start + 1, end) else "$"
    }

    fun calculate() {
        monthlySalary = (ctc.toDoubleOrNull() ?: 0.0) / 12
    }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Salary Calculator") }, navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            OutlinedTextField(value = ctc, onValueChange = { ctc = it }, label = { Text("Annual CTC") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { calculate() }, modifier = Modifier.fillMaxWidth()) { Text("Calculate") }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Monthly Salary: $currencySymbol ${"%.2f".format(monthlySalary)}", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
