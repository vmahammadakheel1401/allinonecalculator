package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.storage.AppDatabase
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BMIScreen(navController: NavController, database: AppDatabase) {
    var heightInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var isMetric by remember { mutableStateOf(true) }

    val height = heightInput.toDoubleOrNull() ?: 0.0
    val weight = weightInput.toDoubleOrNull() ?: 0.0

    var bmi by remember { mutableStateOf(0.0) }
    
    if (height > 0 && weight > 0) {
        bmi = if (isMetric) {
            val heightInMeters = height / 100
            weight / (heightInMeters * heightInMeters)
        } else {
            703 * weight / (height * height)
        }
    } else {
        bmi = 0.0
    }

    val (category, color) = when {
        bmi == 0.0 -> Pair("", Color.Transparent)
        bmi < 18.5 -> Pair("Underweight", Color(0xFF4FC3F7)) // Light Blue
        bmi < 25.0 -> Pair("Normal", Color(0xFF81C784)) // Green
        bmi < 30.0 -> Pair("Overweight", Color(0xFFFFB74D)) // Orange
        else -> Pair("Obese", Color(0xFFE57373)) // Red
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = isMetric,
                    onClick = { isMetric = true },
                    label = { Text("Metric (kg, cm)") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = !isMetric,
                    onClick = { isMetric = false },
                    label = { Text("Imperial (lbs, in)") }
                )
            }

            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it },
                label = { Text(if (isMetric) "Weight (kg)" else "Weight (lbs)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = heightInput,
                onValueChange = { heightInput = it },
                label = { Text(if (isMetric) "Height (cm)" else "Height (inches)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (bmi > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Your BMI",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.1f", bmi),
                            style = MaterialTheme.typography.displayLarge,
                            color = color
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = color.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleLarge,
                                color = color,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
