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
fun GSTScreen(navController: NavHostController, database: AppDatabase) {
    var amount by remember { mutableStateOf("") }
    var gstRate by remember { mutableStateOf("18") }
    var gstAmount by remember { mutableStateOf(0.0) }
    var totalAmount by remember { mutableStateOf(0.0) }

    fun calculate(isInclusive: Boolean) {
        val a = amount.toDoubleOrNull() ?: 0.0
        val r = gstRate.toDoubleOrNull() ?: 0.0
        if (isInclusive) {
            totalAmount = a
            gstAmount = a - (a / (1 + r / 100))
        } else {
            gstAmount = a * (r / 100)
            totalAmount = a + gstAmount
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("GST Calculator") }, navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = gstRate, onValueChange = { gstRate = it }, label = { Text("GST Rate (%)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { calculate(false) }, modifier = Modifier.weight(1f)) { Text("Add GST") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { calculate(true) }, modifier = Modifier.weight(1f)) { Text("Remove GST") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("GST Amount: ${"%.2f".format(gstAmount)}", style = MaterialTheme.typography.titleLarge)
            Text("Total Amount: ${"%.2f".format(totalAmount)}", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
