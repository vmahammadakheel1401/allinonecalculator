package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.storage.AppDatabase
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BMIScreen(navController: NavController, database: AppDatabase) {
    var isMetric by remember { mutableStateOf(true) }
    
    var weightStr by remember { mutableStateOf("") }
    var heightCmStr by remember { mutableStateOf("") }
    var heightFtStr by remember { mutableStateOf("") }
    var heightInStr by remember { mutableStateOf("") }
    
    var bmiResult by remember { mutableStateOf<Double?>(null) }
    val format = remember { DecimalFormat("#.1") }

    fun calculateBmi() {
        val weight = weightStr.toDoubleOrNull() ?: return
        val heightInMeters = if (isMetric) {
            val cm = heightCmStr.toDoubleOrNull() ?: return
            cm / 100.0
        } else {
            val ft = heightFtStr.toDoubleOrNull() ?: 0.0
            val inc = heightInStr.toDoubleOrNull() ?: 0.0
            val totalInches = (ft * 12) + inc
            if (totalInches == 0.0) return
            totalInches * 0.0254
        }
        
        if (heightInMeters > 0 && weight > 0) {
            val weightInKg = if (isMetric) weight else weight * 0.453592
            bmiResult = weightInKg / (heightInMeters * heightInMeters)
        } else {
            bmiResult = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BMI Calculator") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilterChip(
                    selected = isMetric,
                    onClick = { 
                        isMetric = true 
                        bmiResult = null
                    },
                    label = { Text("Metric (kg, cm)") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isMetric,
                    onClick = { 
                        isMetric = false
                        bmiResult = null
                    },
                    label = { Text("Imperial (lb, ft-in)") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            OutlinedTextField(
                value = weightStr,
                onValueChange = { weightStr = it },
                label = { Text(if (isMetric) "Weight (kg)" else "Weight (lb)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            if (isMetric) {
                OutlinedTextField(
                    value = heightCmStr,
                    onValueChange = { heightCmStr = it },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = heightFtStr,
                        onValueChange = { heightFtStr = it },
                        label = { Text("Feet") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = heightInStr,
                        onValueChange = { heightInStr = it },
                        label = { Text("Inches") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                 OutlinedButton(
                    onClick = {
                        weightStr = ""
                        heightCmStr = ""
                        heightFtStr = ""
                        heightInStr = ""
                        bmiResult = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
                Button(onClick = { calculateBmi() }, modifier = Modifier.weight(2f)) {
                    Text("Calculate")
                }
            }
            
            if (bmiResult != null) {
                val category = when {
                    bmiResult!! < 18.5 -> "Underweight"
                    bmiResult!! < 25.0 -> "Normal"
                    bmiResult!! < 30.0 -> "Overweight"
                    else -> "Obese"
                }
                
                val color = when {
                    bmiResult!! < 18.5 -> MaterialTheme.colorScheme.tertiary
                    bmiResult!! < 25.0 -> MaterialTheme.colorScheme.primary
                    bmiResult!! < 30.0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.error
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Your BMI",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = format.format(bmiResult!!),
                            style = MaterialTheme.typography.displayLarge,
                            color = color
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleLarge,
                            color = color
                        )
                    }
                }
            }
        }
    }
}
