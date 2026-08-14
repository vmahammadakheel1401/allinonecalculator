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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelCostScreen(navController: NavHostController, database: AppDatabase) {
    var distance by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var totalCost by remember { mutableStateOf(0.0) }

    fun calculate() {
        val d = distance.toDoubleOrNull() ?: 0.0
        val m = mileage.toDoubleOrNull() ?: 0.0
        val p = price.toDoubleOrNull() ?: 0.0
        if (m > 0) {
            totalCost = (d / m) * p
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Fuel Cost Calculator") }, navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            OutlinedTextField(value = distance, onValueChange = { distance = it }, label = { Text("Distance (km)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = mileage, onValueChange = { mileage = it }, label = { Text("Mileage (km/l)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Fuel Price (per litre)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { calculate() }, modifier = Modifier.fillMaxWidth()) { Text("Calculate") }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Total Cost: ${"%.2f".format(totalCost)}", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
