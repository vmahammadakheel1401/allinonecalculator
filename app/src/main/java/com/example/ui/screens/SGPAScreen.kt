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

data class Subject(val name: String, val credits: Double, val gradePoints: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SGPAScreen(navController: NavHostController, database: AppDatabase) {
    var subjects by remember { mutableStateOf(listOf(Subject("", 0.0, 0.0))) }
    var sgpa by remember { mutableStateOf(0.0) }

    fun calculateSGPA() {
        var totalPoints = 0.0
        var totalCredits = 0.0
        for (subject in subjects) {
            totalPoints += subject.credits * subject.gradePoints
            totalCredits += subject.credits
        }
        sgpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0
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
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(subjects) { index, subject ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = subject.name,
                            onValueChange = { subjects = subjects.toMutableList().apply { this[index] = subject.copy(name = it) } },
                            label = { Text("Subject") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = if (subject.credits > 0) subject.credits.toString() else "",
                            onValueChange = { subjects = subjects.toMutableList().apply { this[index] = subject.copy(credits = it.toDoubleOrNull() ?: 0.0) } },
                            label = { Text("Credits") },
                            modifier = Modifier.width(80.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = if (subject.gradePoints > 0) subject.gradePoints.toString() else "",
                            onValueChange = { subjects = subjects.toMutableList().apply { this[index] = subject.copy(gradePoints = it.toDoubleOrNull() ?: 0.0) } },
                            label = { Text("Points") },
                            modifier = Modifier.width(80.dp)
                        )
                        IconButton(onClick = { subjects = subjects.toMutableList().apply { removeAt(index) } }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Button(onClick = { subjects = subjects + Subject("", 0.0, 0.0) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add Subject")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { calculateSGPA() }, modifier = Modifier.fillMaxWidth()) {
                Text("Calculate SGPA")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("SGPA: ${"%.2f".format(sgpa)}", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
