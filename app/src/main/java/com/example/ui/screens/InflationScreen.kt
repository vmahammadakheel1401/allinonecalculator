package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.storage.AppDatabase
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InflationScreen(navController: NavHostController, database: AppDatabase) {
    var amount by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var years by remember { mutableStateOf("") }
    
    var futureValue by remember { mutableStateOf<Double?>(null) }
    var inflationIncrease by remember { mutableStateOf<Double?>(null) }
    var validationError by remember { mutableStateOf("") }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun calculate() {
        val a = amount.toDoubleOrNull() ?: 0.0
        val r = (rate.toDoubleOrNull() ?: 0.0) / 100
        val y = years.toDoubleOrNull() ?: 0.0
        
        if (a <= 0 || (rate.toDoubleOrNull() ?: -1.0) < 0 || y <= 0) {
            validationError = "Please enter valid positive values"
            futureValue = null
            return
        }
        validationError = ""
        futureValue = a * (1 + r).pow(y)
        inflationIncrease = (futureValue ?: 0.0) - a
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Inflation Calculator") }, navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount today") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text("Inflation Rate (%)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = years, onValueChange = { years = it }, label = { Text("Number of Years") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { calculate() }, modifier = Modifier.fillMaxWidth()) { Text("Calculate") }
            
            if (validationError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(validationError, color = MaterialTheme.colorScheme.error)
            }
            
            futureValue?.let { fv ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("${currencyFormatter.format(amount.toDoubleOrNull() ?: 0.0)} today will require", style = MaterialTheme.typography.bodyLarge)
                Text(currencyFormatter.format(fv), style = MaterialTheme.typography.headlineMedium)
                Text("in ${years} years", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Inflation increase: ${currencyFormatter.format(inflationIncrease ?: 0.0)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
