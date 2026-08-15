package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.storage.AppDatabase
import com.example.models.HistoryEntry
import com.example.utilities.ExpressionEvaluator
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(navController: NavController, database: AppDatabase) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var instantResult by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isScientific by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val evaluator = remember { ExpressionEvaluator() }
    
    // Formatter for thousands separator
    val numberFormatter = remember { DecimalFormat("#,###.########") }

    LaunchedEffect(expression) {
        if (expression.isNotEmpty()) {
            try {
                // Try to evaluate to show instant result
                val eval = evaluator.evaluate(expression)
                // Only show if it's a valid number and doesn't equal the last finalized result
                val formatted = numberFormatter.format(eval)
                instantResult = if (formatted != result) formatted else ""
            } catch (e: Exception) {
                instantResult = ""
            }
        } else {
            instantResult = ""
        }
    }

    fun formatNumber(input: String): String {
        return input.replace(Regex("\\d+(?:\\.\\d+)?")) { match ->
            val number = match.value.toDoubleOrNull() ?: return@replace match.value
            numberFormatter.format(number)
        }
    }

    fun onAction(action: String) {
        when (action) {
            "C" -> {
                expression = ""
                result = ""
                isError = false
            }
            "⌫" -> {
                if (expression.isNotEmpty()) {
                    expression = expression.dropLast(1)
                }
            }
            "=" -> {
                if (expression.isNotEmpty()) {
                    try {
                        val evalResult = evaluator.evaluate(expression)
                        result = numberFormatter.format(evalResult)
                        instantResult = ""
                        isError = false
                        
                        coroutineScope.launch {
                            database.historyDao().insert(
                                HistoryEntry(
                                    toolName = "Calculator",
                                    inputSummary = expression,
                                    result = result
                                )
                            )
                        }
                    } catch (e: Exception) {
                        result = "Error"
                        isError = true
                    }
                }
            }
            else -> {
                if (isError) {
                    expression = ""
                    result = ""
                    isError = false
                }
                when (action) {
                    "sin", "cos", "tan", "log", "ln", "√" -> expression += "$action("
                    "x²" -> expression += "^2"
                    else -> expression += action
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculator") },
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Scientific Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                FilterChip(
                    selected = isScientific,
                    onClick = { isScientific = !isScientific },
                    label = { Text("Scientific") }
                )
            }
            
            // Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatNumber(expression),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
                if (instantResult.isNotEmpty()) {
                    Text(
                        text = instantResult,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result,
                    style = MaterialTheme.typography.displayLarge,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // Keypad
            val standardButtons = listOf(
                listOf("C", "(", ")", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "⌫", "=")
            )

            val scientificButtons = listOf(
                listOf("sin", "cos", "tan", "log"),
                listOf("ln", "√", "x²", "π"),
                listOf("e", "^", "!", "Mod")
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isScientific) 1.5f else 1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isScientific) {
                    scientificButtons.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { btn ->
                                CalculatorButton(
                                    text = btn,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onAction(btn) },
                                    isScientificBtn = true
                                )
                            }
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 2.dp))
                }

                standardButtons.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { btn ->
                            CalculatorButton(
                                text = btn,
                                modifier = Modifier.weight(1f),
                                onClick = { onAction(btn) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit, isScientificBtn: Boolean = false) {
    val isOperator = text in listOf("÷", "×", "-", "+", "=")
    val isAction = text in listOf("C", "(", ")", "⌫")
    
    val containerColor = when {
        text == "=" -> MaterialTheme.colorScheme.primary
        isOperator -> MaterialTheme.colorScheme.secondaryContainer
        isAction || isScientificBtn -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    
    val contentColor = when {
        text == "=" -> MaterialTheme.colorScheme.onPrimary
        isOperator -> MaterialTheme.colorScheme.onSecondaryContainer
        text == "C" -> MaterialTheme.colorScheme.error
        isAction || isScientificBtn -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(100.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
    ) {
        if (text == "⌫") {
            Icon(Icons.Filled.Backspace, contentDescription = "Backspace", tint = contentColor)
        } else {
            Text(
                text = text,
                style = if (isScientificBtn) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
