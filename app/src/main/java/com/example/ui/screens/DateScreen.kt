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
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateScreen(navController: NavController, database: AppDatabase) {
    var selectedTab by remember { mutableStateOf(0) }
    
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Date Calculator") },
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
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Difference") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Add/Subtract") }
                )
            }
            
            Box(modifier = Modifier.padding(16.dp)) {
                if (selectedTab == 0) {
                    DateDifferenceTab(formatter)
                } else {
                    DateAddSubtractTab(formatter)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDifferenceTab(formatter: DateTimeFormatter) {
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(7)) }
    
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    
    val totalDays = ChronoUnit.DAYS.between(startDate, endDate)
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text("From: ${startDate.format(formatter)}")
        }
        
        OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text("To: ${endDate.format(formatter)}")
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$totalDays Days",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    if (showStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        startDate = LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                    }
                    showStartPicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }
    
    if (showEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate.toEpochDay() * 24 * 60 * 60 * 1000)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        endDate = LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                    }
                    showEndPicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateAddSubtractTab(formatter: DateTimeFormatter) {
    var baseDate by remember { mutableStateOf(LocalDate.now()) }
    var daysStr by remember { mutableStateOf("7") }
    var isAdd by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val days = daysStr.toLongOrNull() ?: 0L
    val resultDate = if (isAdd) baseDate.plusDays(days) else baseDate.minusDays(days)
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Date: ${baseDate.format(formatter)}")
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilterChip(
                selected = isAdd,
                onClick = { isAdd = true },
                label = { Text("Add") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = !isAdd,
                onClick = { isAdd = false },
                label = { Text("Subtract") },
                modifier = Modifier.weight(1f)
            )
        }
        
        OutlinedTextField(
            value = daysStr,
            onValueChange = { daysStr = it },
            label = { Text("Days") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Result Date",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resultDate.format(formatter),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
    
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = baseDate.toEpochDay() * 24 * 60 * 60 * 1000)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        baseDate = LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}
