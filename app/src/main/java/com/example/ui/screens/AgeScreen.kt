package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.storage.AppDatabase
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeScreen(navController: NavController, database: AppDatabase) {
    var dateOfBirth by remember { mutableStateOf(LocalDate.of(2000, 1, 1)) }
    var asOfDate by remember { mutableStateOf(LocalDate.now()) }
    
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    
    var showDobPicker by remember { mutableStateOf(false) }
    var showAsOfPicker by remember { mutableStateOf(false) }
    
    val period = Period.between(dateOfBirth, asOfDate)
    val totalDays = ChronoUnit.DAYS.between(dateOfBirth, asOfDate)
    
    val isError = totalDays < 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Age Calculator") },
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
            if (isError) {
                Text("Date of Birth cannot be after 'As Of' date", color = MaterialTheme.colorScheme.error)
            }
            
            OutlinedButton(onClick = { showDobPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Date of Birth: ${dateOfBirth.format(formatter)}")
            }
            
            OutlinedButton(onClick = { showAsOfPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("As Of Date: ${asOfDate.format(formatter)}")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (!isError) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${period.years} Years, ${period.months} Months, ${period.days} Days",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Total Days: $totalDays",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
        
        if (showDobPicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = dateOfBirth.toEpochDay() * 24 * 60 * 60 * 1000
            )
            DatePickerDialog(
                onDismissRequest = { showDobPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            dateOfBirth = LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                        }
                        showDobPicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDobPicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
        
        if (showAsOfPicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = asOfDate.toEpochDay() * 24 * 60 * 60 * 1000
            )
            DatePickerDialog(
                onDismissRequest = { showAsOfPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            asOfDate = LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                        }
                        showAsOfPicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAsOfPicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
