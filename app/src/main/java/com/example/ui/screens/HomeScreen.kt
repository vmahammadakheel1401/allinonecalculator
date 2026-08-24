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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.navigation.Screen

data class ToolItem(
    val title: String,
    val icon: ImageVector,
    val category: String,
    val route: String,
    val color: Color,
    val aliases: List<String> = emptyList()
)

val allTools = listOf(
    ToolItem("Basic Calculator", Icons.Filled.Calculate, "Calculator", Screen.Calculator.route, Color(0xFF5C6BC0), listOf("basic", "math")),
    ToolItem("Currency Converter", Icons.Filled.CurrencyExchange, "Converter", Screen.Currency.route, Color(0xFF26A69A), listOf("money", "exchange", "currency")),
    ToolItem("Unit Converter", Icons.Filled.SquareFoot, "Converter", Screen.UnitConverter.route, Color(0xFFFFA726), listOf("unit", "conversion")),
    ToolItem("Discount Calculator", Icons.Filled.LocalOffer, "Finance", Screen.Discount.route, Color(0xFFEC407A), listOf("discount", "offer", "percent")),
    ToolItem("Loan / EMI Calculator", Icons.Filled.AccountBalance, "Finance", Screen.Loan.route, Color(0xFFAB47BC), listOf("loan", "emi", "finance")),
    ToolItem("Date Calculator", Icons.Filled.DateRange, "Date & Time", Screen.DateCalc.route, Color(0xFF29B6F6), listOf("date", "time")),
    ToolItem("Age Calculator", Icons.Filled.Cake, "Date & Time", Screen.Age.route, Color(0xFFEF5350), listOf("age", "birthday")),
    ToolItem("SGPA / CGPA Calculator", Icons.Filled.School, "Calculator", Screen.SGPA.route, Color(0xFF5C6BC0), listOf("sgpa", "cgpa", "college", "grade")),
    ToolItem("Fuel Cost Calculator", Icons.Filled.LocalGasStation, "Calculator", Screen.Fuel.route, Color(0xFF26A69A), listOf("fuel", "gas", "cost")),
    ToolItem("GST Calculator", Icons.Filled.Receipt, "Finance", Screen.GST.route, Color(0xFFFFA726), listOf("gst", "tax")),
    ToolItem("SIP Calculator", Icons.Filled.TrendingUp, "Finance", Screen.SIP.route, Color(0xFFEC407A), listOf("sip", "investment")),
    ToolItem("Salary Calculator", Icons.Filled.AttachMoney, "Finance", Screen.Salary.route, Color(0xFFAB47BC), listOf("salary", "take-home")),
    ToolItem("Inflation Calculator", Icons.Filled.TrendingDown, "Finance", Screen.Inflation.route, Color(0xFF29B6F6), listOf("inflation", "future")),
    ToolItem("Unit Price Calculator", Icons.Filled.ShoppingCart, "Calculator", Screen.UnitPrice.route, Color(0xFFEF5350), listOf("unit price", "shopping", "compare")),
    ToolItem("Freelance Rate Calculator", Icons.Filled.Work, "Finance", Screen.Freelance.route, Color(0xFF66BB6A), listOf("freelance", "hourly")),
    ToolItem("BMI Calculator", Icons.Filled.FitnessCenter, "Health", Screen.BMI.route, Color(0xFF4CAF50), listOf("bmi", "health", "weight")),
    ToolItem("Ovulation Calculator", Icons.Filled.PregnantWoman, "Health", Screen.Ovulation.route, Color(0xFFF06292), listOf("ovulation", "period", "menstrual", "health"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Calculator", "Converter", "Finance", "Date & Time", "Health")

    val filteredTools = allTools.filter {
        (selectedCategory == "All" || it.category == selectedCategory) &&
        (it.title.contains(searchQuery, ignoreCase = true) || it.aliases.any { alias -> alias.contains(searchQuery, ignoreCase = true) })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("All-in-One Calculator")
                        Text("Everyday calculations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
                .padding(8.dp),
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
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
