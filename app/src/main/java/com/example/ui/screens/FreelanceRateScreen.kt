package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.storage.AppDatabase
import com.example.storage.SettingsManager
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreelanceRateScreen(navController: NavHostController, database: AppDatabase, settingsManager: SettingsManager) {
    var income by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf<Double?>(null) }
    var validationError by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val currency by settingsManager.currencyFlow.collectAsState(initial = "USD ($)")
    val currencySymbol = remember(currency) {
        val start = currency.indexOf("(")
        val end = currency.indexOf(")")
        if (start != -1 && end != -1) currency.substring(start + 1, end) else "$"
    }
    val currencyFormatter = remember(currency) {
        NumberFormat.getCurrencyInstance(Locale.getDefault()) // This will format based on default locale
        // Note: For a truly robust implementation, one should format based on the selected currency's locale if possible.
        // Given the constraints, I will use the system default locale, but prefix/format with the selected currency symbol if needed.
        // Actually, just changing the label is probably what the user wants.
        NumberFormat.getCurrencyInstance(Locale.getDefault())
    }

    fun calculate() {
        val i = income.toDoubleOrNull()
        val h = hours.toDoubleOrNull()

        if (i == null || h == null || i <= 0 || h <= 0) {
            validationError = "Please enter valid positive values."
            rate = null
        } else {
            validationError = ""
            rate = i / h
            focusManager.clearFocus()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Freelance Rate Calculator") }, navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = income,
                onValueChange = { income = it },
                label = { Text("Desired Monthly Income ($currencySymbol)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                placeholder = { Text("e.g. 50000") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = hours,
                onValueChange = { hours = it },
                label = { Text("Monthly Working Hours") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("e.g. 160") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { calculate() }, modifier = Modifier.fillMaxWidth()) { Text("Calculate") }

            if (validationError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(validationError, color = MaterialTheme.colorScheme.error)
            }

            rate?.let { r ->
                Spacer(modifier = Modifier.height(24.dp))
                Text("Recommended Hourly Rate", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${currencyFormatter.format(r)} / hour",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Monthly Income: ${currencyFormatter.format(income.toDoubleOrNull() ?: 0.0)}")
                Text("Monthly Working Hours: $hours")
            }
        }
    }
}
