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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.storage.AppDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitPriceScreen(navController: NavHostController, database: AppDatabase) {
    var price1 by remember { mutableStateOf("") }
    var qty1 by remember { mutableStateOf("") }
    var unit1 by remember { mutableStateOf("g") }
    var price2 by remember { mutableStateOf("") }
    var qty2 by remember { mutableStateOf("") }
    var unit2 by remember { mutableStateOf("g") }
    var result by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf("") }

    val units = listOf("g", "kg", "ml", "L", "pcs")

    fun getFactor(unit: String): Double = when (unit) {
        "kg" -> 1000.0
        "L" -> 1000.0
        else -> 1.0
    }

    fun compare() {
        val p1 = price1.toDoubleOrNull() ?: 0.0
        val q1 = qty1.toDoubleOrNull() ?: 0.0
        val p2 = price2.toDoubleOrNull() ?: 0.0
        val q2 = qty2.toDoubleOrNull() ?: 0.0

        if (p1 <= 0 || q1 <= 0 || p2 <= 0 || q2 <= 0) {
            validationError = "Please enter valid positive values"
            result = ""
            return
        }
        
        // Basic category check (Weight/Volume/Pcs)
        val isWeightVol = (unit1 in listOf("g", "kg", "ml", "L")) && (unit2 in listOf("g", "kg", "ml", "L"))
        val isPcs = (unit1 == "pcs") && (unit2 == "pcs")

        if (!isWeightVol && !isPcs) {
            validationError = "Units are not comparable"
            result = ""
            return
        }

        validationError = ""

        val normalizedPrice1 = p1 / (q1 * getFactor(unit1))
        val normalizedPrice2 = p2 / (q2 * getFactor(unit2))
        
        val betterUnit = if (unit1 == unit2) unit1 else "base unit"

        result = when {
            normalizedPrice1 < normalizedPrice2 -> 
                "Product 1 is better value (₹%.2f/%s vs ₹%.2f/%s)\nSave ₹%.2f/%s with Product 1".format(
                    normalizedPrice1, betterUnit, normalizedPrice2, betterUnit, (normalizedPrice2 - normalizedPrice1), betterUnit)
            normalizedPrice2 < normalizedPrice1 -> 
                "Product 2 is better value (₹%.2f/%s vs ₹%.2f/%s)\nSave ₹%.2f/%s with Product 2".format(
                    normalizedPrice2, betterUnit, normalizedPrice1, betterUnit, (normalizedPrice1 - normalizedPrice2), betterUnit)
            else -> "Both are equal value (₹%.2f/%s)".format(normalizedPrice1, betterUnit)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Unit Price Calculator") }, navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Product 1", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = price1, onValueChange = { price1 = it }, label = { Text("Price") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(value = qty1, onValueChange = { qty1 = it }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            
            // Dropdown implementation for unit selection
            var expanded1 by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { expanded1 = true }, modifier = Modifier.fillMaxWidth()) { Text("Unit: $unit1") }
                DropdownMenu(expanded = expanded1, onDismissRequest = { expanded1 = false }) {
                    units.forEach { unit -> DropdownMenuItem(text = { Text(unit) }, onClick = { unit1 = unit; expanded1 = false }) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Product 2", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = price2, onValueChange = { price2 = it }, label = { Text("Price") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(value = qty2, onValueChange = { qty2 = it }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            
            var expanded2 by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { expanded2 = true }, modifier = Modifier.fillMaxWidth()) { Text("Unit: $unit2") }
                DropdownMenu(expanded = expanded2, onDismissRequest = { expanded2 = false }) {
                    units.forEach { unit -> DropdownMenuItem(text = { Text(unit) }, onClick = { unit2 = unit; expanded2 = false }) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { compare() }, modifier = Modifier.fillMaxWidth()) { Text("Compare") }
            
            if (validationError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(validationError, color = MaterialTheme.colorScheme.error)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(result, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
