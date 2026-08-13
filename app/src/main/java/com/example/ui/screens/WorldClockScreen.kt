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
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen(navController: NavController, database: AppDatabase) {
    val allZones = remember { ZoneId.getAvailableZoneIds().sorted() }
    var selectedZone by remember { mutableStateOf(ZoneId.systemDefault().id) }
    var expanded by remember { mutableStateOf(false) }
    
    // We'll just show a list of favorite zones for now
    val favoriteZones = remember { mutableStateListOf("UTC", "America/New_York", "Europe/London", "Asia/Tokyo") }
    
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm z")
    var currentTime = remember { ZonedDateTime.now(ZoneId.of(selectedZone)) }
    
    // In a real app we'd have a ticker to update time, for now it's static on load/change
    LaunchedEffect(selectedZone) {
        currentTime = ZonedDateTime.now(ZoneId.of(selectedZone))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("World Clock") },
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
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedZone,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Base Timezone") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    allZones.forEach { zone ->
                        DropdownMenuItem(
                            text = { Text(zone) },
                            onClick = {
                                selectedZone = zone
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Text(
                text = "Current Time: ${currentTime.format(formatter)}",
                style = MaterialTheme.typography.titleMedium
            )
            
            Divider()
            
            Text("Other Timezones", style = MaterialTheme.typography.titleMedium)
            
            favoriteZones.forEach { zone ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val zoneTime = currentTime.withZoneSameInstant(ZoneId.of(zone))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(zone, style = MaterialTheme.typography.titleMedium)
                        Text(zoneTime.format(formatter), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
