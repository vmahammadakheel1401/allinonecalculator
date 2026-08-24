package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.models.HistoryEntry
import com.example.storage.AppDatabase
import com.example.utilities.ExpressionEvaluator
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(navController: NavController, database: AppDatabase) {
    var topHistoryExpression by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("0") }
    var livePreviewResult by remember { mutableStateOf("") }
    var isJustCalculated by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var isScientific by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val evaluator = remember { ExpressionEvaluator() }
    val numberFormatter = remember { DecimalFormat("#,###.########") }

    val displayScrollState = rememberScrollState()
    val topScrollState = rememberScrollState()

    // Keep scroll at the end of the text as input changes
    LaunchedEffect(currentInput) {
        displayScrollState.animateScrollTo(displayScrollState.maxValue)
    }

    LaunchedEffect(topHistoryExpression) {
        topScrollState.animateScrollTo(topScrollState.maxValue)
    }

    // Compute live preview as user types
    LaunchedEffect(currentInput, isJustCalculated) {
        if (!isJustCalculated && currentInput.isNotEmpty() && currentInput != "0" && !isError) {
            val hasOperator = currentInput.any { it in "+-×÷%^#" || currentInput.contains("sin") || currentInput.contains("cos") || currentInput.contains("√") }
            if (hasOperator) {
                try {
                    val eval = evaluator.evaluate(currentInput)
                    if (!eval.isNaN() && !eval.isInfinite()) {
                        val formatted = numberFormatter.format(eval)
                        livePreviewResult = "= $formatted"
                    } else {
                        livePreviewResult = ""
                    }
                } catch (_: Exception) {
                    livePreviewResult = ""
                }
            } else {
                livePreviewResult = ""
            }
        } else {
            livePreviewResult = ""
        }
    }

    fun formatDisplay(input: String): String {
        return input.replace(Regex("\\d+(?:\\.\\d+)?")) { match ->
            val number = match.value.toDoubleOrNull() ?: return@replace match.value
            // Only format if number doesn't end in a trailing dot
            if (match.value.endsWith(".")) match.value else numberFormatter.format(number)
        }
    }

    fun onAction(action: String) {
        if (isError) {
            currentInput = "0"
            isError = false
            isJustCalculated = false
        }

        when (action) {
            "AC", "C" -> {
                if (currentInput != "0" && !isJustCalculated) {
                    currentInput = "0"
                } else {
                    currentInput = "0"
                    topHistoryExpression = ""
                }
                livePreviewResult = ""
                isJustCalculated = false
                isError = false
            }

            "⌫" -> {
                if (isJustCalculated) {
                    isJustCalculated = false
                } else if (currentInput.isNotEmpty() && currentInput != "0") {
                    currentInput = currentInput.dropLast(1)
                    if (currentInput.isEmpty() || currentInput == "-") {
                        currentInput = "0"
                    }
                }
            }

            "=" -> {
                if (currentInput.isNotEmpty() && currentInput != "0" && !isJustCalculated) {
                    try {
                        val evalResult = evaluator.evaluate(currentInput)
                        if (evalResult.isNaN() || evalResult.isInfinite()) {
                            isError = true
                            currentInput = "Error"
                            return
                        }
                        val formattedResult = numberFormatter.format(evalResult)

                        // Set the equation upstairs on top
                        topHistoryExpression = "$currentInput ="
                        val previousExpr = currentInput

                        // Set calculated result in the main display
                        currentInput = formattedResult
                        isJustCalculated = true
                        isError = false
                        livePreviewResult = ""

                        // Save to database
                        coroutineScope.launch {
                            database.historyDao().insert(
                                HistoryEntry(
                                    toolName = "Calculator",
                                    inputSummary = previousExpr,
                                    result = formattedResult
                                )
                            )
                        }
                    } catch (_: Exception) {
                        isError = true
                        currentInput = "Error"
                    }
                }
            }

            "±" -> {
                if (isJustCalculated) {
                    isJustCalculated = false
                }
                if (currentInput == "0") {
                    // Do nothing
                } else if (currentInput.startsWith("-") && !currentInput.substring(1).any { it in "+-×÷" }) {
                    currentInput = currentInput.substring(1)
                } else if (!currentInput.any { it in "+-×÷" }) {
                    currentInput = "-$currentInput"
                } else {
                    // Wrap with negation or toggle last number
                    currentInput = "-($currentInput)"
                }
            }

            "+", "-", "×", "÷", "%" -> {
                val operatorSymbol = action
                if (isJustCalculated) {
                    // Chain with the previous calculated answer
                    isJustCalculated = false
                    val cleanVal = currentInput.replace(",", "")
                    currentInput = "$cleanVal$operatorSymbol"
                } else {
                    if (currentInput.endsWith("+") || currentInput.endsWith("-") ||
                        currentInput.endsWith("×") || currentInput.endsWith("÷") ||
                        currentInput.endsWith("%")
                    ) {
                        currentInput = currentInput.dropLast(1) + operatorSymbol
                    } else {
                        currentInput += operatorSymbol
                    }
                }
            }

            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" -> {
                if (isJustCalculated) {
                    // Start fresh calculation after an equals output
                    isJustCalculated = false
                    currentInput = action
                } else {
                    if (currentInput == "0") {
                        currentInput = action
                    } else {
                        currentInput += action
                    }
                }
            }

            "." -> {
                if (isJustCalculated) {
                    isJustCalculated = false
                    currentInput = "0."
                } else {
                    // Find the current active number segment
                    val lastSegment = currentInput.split(Regex("[+\\-×÷%^()]")).lastOrNull() ?: ""
                    if (!lastSegment.contains(".")) {
                        if (lastSegment.isEmpty()) {
                            currentInput += "0."
                        } else {
                            currentInput += "."
                        }
                    }
                }
            }

            "sin", "cos", "tan", "log", "ln", "√" -> {
                if (isJustCalculated) {
                    isJustCalculated = false
                    currentInput = "$action("
                } else {
                    if (currentInput == "0") {
                        currentInput = "$action("
                    } else {
                        currentInput += "$action("
                    }
                }
            }

            "x²" -> {
                if (isJustCalculated) {
                    isJustCalculated = false
                    val cleanVal = currentInput.replace(",", "")
                    currentInput = "$cleanVal^2"
                } else {
                    currentInput += "^2"
                }
            }

            "π" -> {
                if (isJustCalculated || currentInput == "0") {
                    isJustCalculated = false
                    currentInput = "π"
                } else {
                    currentInput += "π"
                }
            }

            "e" -> {
                if (isJustCalculated || currentInput == "0") {
                    isJustCalculated = false
                    currentInput = "e"
                } else {
                    currentInput += "e"
                }
            }

            else -> {
                if (isJustCalculated) {
                    isJustCalculated = false
                    currentInput = action
                } else {
                    if (currentInput == "0") {
                        currentInput = action
                    } else {
                        currentInput += action
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculator") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("history") }
                    ) {
                        Icon(Icons.Filled.History, contentDescription = "History")
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
            // Mode Indicator Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isScientific,
                    onClick = { isScientific = !isScientific },
                    label = { Text("Scientific") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Science,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                if (isJustCalculated) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Result",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Display Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                // Top Expression (Shows completed calculation when = is pressed or past operation)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(topScrollState),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = if (topHistoryExpression.isNotEmpty()) formatDisplay(topHistoryExpression) else " ",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = if (topHistoryExpression.length > 20) 18.sp else 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Live Preview Result while typing (e.g. "= 65")
                AnimatedVisibility(
                    visible = livePreviewResult.isNotEmpty() && !isJustCalculated,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = livePreviewResult,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Main Output Display (Current Input or Final Result)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(displayScrollState)
                        .testTag("calculator_display"),
                    horizontalArrangement = Arrangement.End
                ) {
                    val displayText = if (isJustCalculated || isError) currentInput else formatDisplay(currentInput)
                    val fontSize = when {
                        displayText.length > 14 -> 36.sp
                        displayText.length > 9 -> 44.sp
                        else -> 54.sp
                    }

                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = fontSize),
                        color = when {
                            isError -> MaterialTheme.colorScheme.error
                            isJustCalculated -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }

                Divider(
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }

            // Keypad Section
            val standardButtons = listOf(
                listOf(if (currentInput != "0") "C" else "AC", "±", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "⌫", "=")
            )

            val scientificButtons = listOf(
                listOf("sin", "cos", "tan", "log"),
                listOf("ln", "√", "x²", "^"),
                listOf("π", "e", "(", ")")
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isScientific) 1.6f else 1.15f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isScientific) {
                    scientificButtons.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
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
                    Divider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }

                standardButtons.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { btn ->
                            CalculatorButton(
                                text = btn,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag(
                                        when (btn) {
                                            "=" -> "equals_button"
                                            "C", "AC" -> "clear_button"
                                            "⌫" -> "backspace_button"
                                            "+" -> "add_button"
                                            "-" -> "subtract_button"
                                            "×" -> "multiply_button"
                                            "÷" -> "divide_button"
                                            else -> "btn_$btn"
                                        }
                                    ),
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
fun CalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isScientificBtn: Boolean = false
) {
    val isOperator = text in listOf("÷", "×", "-", "+", "=")
    val isAction = text in listOf("C", "AC", "±", "%", "(", ")", "⌫")

    val containerColor = when {
        text == "=" -> MaterialTheme.colorScheme.primary
        isOperator -> MaterialTheme.colorScheme.secondaryContainer
        text in listOf("C", "AC") -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        isAction || isScientificBtn -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
    }

    val contentColor = when {
        text == "=" -> MaterialTheme.colorScheme.onPrimary
        isOperator -> MaterialTheme.colorScheme.onSecondaryContainer
        text in listOf("C", "AC") -> MaterialTheme.colorScheme.error
        isAction || isScientificBtn -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
    ) {
        if (text == "⌫") {
            Icon(
                Icons.Filled.Backspace,
                contentDescription = "Backspace",
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = text,
                style = if (isScientificBtn) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = if (text == "=" || isOperator) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
