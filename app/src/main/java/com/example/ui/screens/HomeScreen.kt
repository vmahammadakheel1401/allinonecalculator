package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.navigation.Screen

data class ToolItem(
    val title: String,
    val icon: ImageVector,
    val category: String,
    val route: String,
    val color: Color
)

val allTools = listOf(
    ToolItem("Calculator", Icons.Filled.Calculate, "Calculator", Screen.Calculator.route, Color(0xFF5C6BC0)),
    ToolItem("Currency", Icons.Filled.CurrencyExchange, "Converter", Screen.Currency.route, Color(0xFF26A69A)),
    ToolItem("Unit", Icons.Filled.SquareFoot, "Converter", Screen.UnitConverter.route, Color(0xFFFFA726)),
    ToolItem("Discount", Icons.Filled.LocalOffer, "Finance", Screen.Discount.route, Color(0xFFEC407A)),
    ToolItem("Loan", Icons.Filled.AccountBalance, "Finance", Screen.Loan.route, Color(0xFFAB47BC)),
    ToolItem("Date", Icons.Filled.DateRange, "Date & Time", Screen.DateCalc.route, Color(0xFF29B6F6)),
    ToolItem("Age", Icons.Filled.Cake, "Date & Time", Screen.Age.route, Color(0xFFEF5350)),
    ToolItem("BMI", Icons.Filled.AccessibilityNew, "Health", Screen.BMI.route, Color(0xFF66BB6A)),
    ToolItem("World Clock", Icons.Filled.Public, "Converter", Screen.WorldClock.route, Color(0xFF8D6E63))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Calculator", "Converter", "Finance", "Date & Time", "Health")

    val filteredTools = allTools.filter {
        (selectedCategory == "All" || it.category == selectedCategory) &&
        it.title.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All-in-One Calculator") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("Search tools...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                shape = MaterialTheme.shapes.extraLarge,
                singleLine = true
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                items(filteredTools) { tool ->
                    ToolCard(tool = tool, onClick = { navController.navigate(tool.route) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolCard(tool: ToolItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = tool.color.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = tool.color,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
