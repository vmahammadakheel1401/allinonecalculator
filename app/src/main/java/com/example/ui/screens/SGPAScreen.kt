package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.storage.AppDatabase

data class SubjectInput(val name: String = "", val credits: String = "", val gradePoints: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SGPAScreen(navController: NavHostController, database: AppDatabase) {
    var subjects by remember { mutableStateOf(listOf(SubjectInput("", "", ""))) }
    var sgpa by remember { mutableStateOf<Double?>(null) }
    var totalCredits by remember { mutableStateOf<Double?>(null) }

    fun calculateSGPA() {
        var points = 0.0
        var credits = 0.0
        for (subject in subjects) {
            val c = subject.credits.toDoubleOrNull() ?: 0.0
            val p = subject.gradePoints.toDoubleOrNull() ?: 0.0
            points += c * p
            credits += c
        }
        totalCredits = credits
        sgpa = if (credits > 0) points / credits else 0.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SGPA / CGPA Calculator") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(subjects) { index, subject ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = subject.name,
                            onValueChange = { input ->
                                subjects = subjects.toMutableList().apply { this[index] = subject.copy(name = input) }
                            },
                            label = { Text("Subject ${index + 1}") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = subject.credits,
                            onValueChange = { input ->
                                val filtered = buildString {
                                    var hasDecimal = false
                                    for (char in input) {
                                        if (char.isDigit()) append(char)
                                        else if (char == '.' && !hasDecimal) {
                                            append(char)
                                            hasDecimal = true
                                        }
                                    }
                                }
                                subjects = subjects.toMutableList().apply { this[index] = subject.copy(credits = filtered) }
                            },
                            label = { Text("Credits") },
                            modifier = Modifier.weight(0.9f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = subject.gradePoints,
                            onValueChange = { input ->
                                val filtered = buildString {
                                    var hasDecimal = false
                                    for (char in input) {
                                        if (char.isDigit()) append(char)
                                        else if (char == '.' && !hasDecimal) {
                                            append(char)
                                            hasDecimal = true
                                        }
                                    }
                                }
                                subjects = subjects.toMutableList().apply { this[index] = subject.copy(gradePoints = filtered) }
                            },
                            label = { Text("Grade Pt") },
                            modifier = Modifier.weight(0.9f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true
                        )
                        if (subjects.size > 1) {
                            IconButton(onClick = { subjects = subjects.toMutableList().apply { removeAt(index) } }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { subjects = subjects + SubjectInput("", "", "") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Subject")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { calculateSGPA() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Calculate SGPA")
            }
            if (sgpa != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Calculated SGPA / CGPA", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "%.2f".format(sgpa),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Total Credits: ${"%.1f".format(totalCredits ?: 0.0)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
