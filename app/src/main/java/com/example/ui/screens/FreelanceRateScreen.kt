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
fun FreelanceRateScreen(navController: NavHostController, database: AppDatabase) {
    var income by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf(0.0) }

    fun calculate() {
        val i = income.toDoubleOrNull() ?: 0.0
        val h = hours.toDoubleOrNull() ?: 1.0
        rate = i / h
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Freelance Rate Calculator") }, navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            OutlinedTextField(value = income, onValueChange = { income = it }, label = { Text("Desired Monthly Income") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Monthly Working Hours") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { calculate() }, modifier = Modifier.fillMaxWidth()) { Text("Calculate") }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Hourly Rate: ${"%.2f".format(rate)}", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
