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
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SIPScreen(navController: NavHostController, database: AppDatabase) {
    var monthlyInvestment by remember { mutableStateOf("") }
    var annualReturn by remember { mutableStateOf("") }
    var years by remember { mutableStateOf("") }
    var totalInvested by remember { mutableStateOf(0.0) }
    var estimatedReturns by remember { mutableStateOf(0.0) }
    var finalValue by remember { mutableStateOf(0.0) }

    fun calculate() {
        val p = monthlyInvestment.toDoubleOrNull() ?: 0.0
        val r = (annualReturn.toDoubleOrNull() ?: 0.0) / 12 / 100
        val n = (years.toDoubleOrNull() ?: 0.0) * 12
        if (r > 0 && n > 0) {
            totalInvested = p * n
            finalValue = p * ((1 + r).pow(n) - 1) * (1 + r) / r
            estimatedReturns = finalValue - totalInvested
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("SIP Calculator") }, navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            OutlinedTextField(value = monthlyInvestment, onValueChange = { monthlyInvestment = it }, label = { Text("Monthly Investment") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = annualReturn, onValueChange = { annualReturn = it }, label = { Text("Expected Annual Return (%)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = years, onValueChange = { years = it }, label = { Text("Duration (Years)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { calculate() }, modifier = Modifier.fillMaxWidth()) { Text("Calculate") }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Invested: ${"%.2f".format(totalInvested)}")
            Text("Returns: ${"%.2f".format(estimatedReturns)}")
            Text("Final Value: ${"%.2f".format(finalValue)}", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
