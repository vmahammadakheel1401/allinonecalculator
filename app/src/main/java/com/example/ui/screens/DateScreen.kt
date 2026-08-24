package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.models.HistoryEntry
import com.example.storage.AppDatabase
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateScreen(navController: NavController, database: AppDatabase) {
    var selectedTab by remember { mutableStateOf(0) }
    val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")
    val shortDateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Date Calculator") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.testTag("back_button")
                    ) {
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
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CompareArrows, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Date Difference")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add / Subtract")
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (selectedTab == 0) {
                    DateDifferenceTab(
                        fullFormatter = fullDateFormatter,
                        shortFormatter = shortDateFormatter,
                        database = database
                    )
                } else {
                    DateAddSubtractTab(
                        fullFormatter = fullDateFormatter,
                        shortFormatter = shortDateFormatter,
                        database = database
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDifferenceTab(
    fullFormatter: DateTimeFormatter,
    shortFormatter: DateTimeFormatter,
    database: AppDatabase
) {
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(30)) }
    var includeEndDay by remember { mutableStateOf(false) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Calculate absolute difference and order
    val (earlierDate, laterDate, isNegative) = if (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {
        Triple(startDate, endDate, false)
    } else {
        Triple(endDate, startDate, true)
    }

    val rawDays = ChronoUnit.DAYS.between(earlierDate, laterDate)
    val totalDays = if (includeEndDay) rawDays + 1 else rawDays

    val period = Period.between(earlierDate, laterDate)
    val weeks = totalDays / 7
    val remainingDaysOfWeek = totalDays % 7

    // Working days calculation (Monday through Friday)
    val workingDays = remember(earlierDate, laterDate, includeEndDay) {
        var count = 0L
        var cur = earlierDate
        val endLimit = if (includeEndDay) laterDate.plusDays(1) else laterDate
        while (cur.isBefore(endLimit)) {
            val dayOfWeek = cur.dayOfWeek
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                count++
            }
            cur = cur.plusDays(1)
        }
        count
    }
    val weekendDays = totalDays - workingDays

    fun saveHistory() {
        coroutineScope.launch {
            database.historyDao().insert(
                HistoryEntry(
                    toolName = "Date Difference",
                    inputSummary = "${startDate.format(shortFormatter)} to ${endDate.format(shortFormatter)}${if (includeEndDay) " (incl. end)" else ""}",
                    result = "$totalDays days (${period.years}y ${period.months}m ${period.days}d), $workingDays work days"
                )
            )
        }
    }

    LaunchedEffect(startDate, endDate, includeEndDay) {
        saveHistory()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Quick Presets
        Text(
            text = "Quick Presets:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                SuggestionChip(
                    onClick = {
                        startDate = LocalDate.now()
                        endDate = LocalDate.now().plusDays(7)
                    },
                    label = { Text("Next 7 Days") }
                )
            }
            item {
                SuggestionChip(
                    onClick = {
                        startDate = LocalDate.now()
                        endDate = LocalDate.now().plusDays(30)
                    },
                    label = { Text("Next 30 Days") }
                )
            }
            item {
                SuggestionChip(
                    onClick = {
                        startDate = LocalDate.now()
                        val lastDayOfMonth = startDate.withDayOfMonth(startDate.lengthOfMonth())
                        endDate = lastDayOfMonth
                    },
                    label = { Text("End of Month") }
                )
            }
            item {
                SuggestionChip(
                    onClick = {
                        startDate = LocalDate.now()
                        endDate = LocalDate.of(startDate.year, 12, 31)
                    },
                    label = { Text("End of Year (${startDate.year})") }
                )
            }
        }

        // Start Date Card
        Surface(
            onClick = { showStartPicker = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "START DATE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = startDate.format(fullFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Swap Dates & Include End Day Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { includeEndDay = !includeEndDay }
            ) {
                Checkbox(
                    checked = includeEndDay,
                    onCheckedChange = { includeEndDay = it }
                )
                Text("Include end day (+1 day)", style = MaterialTheme.typography.bodyMedium)
            }

            FilledTonalIconButton(
                onClick = {
                    val temp = startDate
                    startDate = endDate
                    endDate = temp
                }
            ) {
                Icon(Icons.Filled.SwapVert, contentDescription = "Swap Dates")
            }
        }

        // End Date Card
        Surface(
            onClick = { showEndPicker = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "END DATE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = endDate.format(fullFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
        }

        // Result Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total Difference",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$totalDays Days",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isNegative) {
                    Text(
                        text = "(End date is before Start date)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Detailed Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Difference Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Calendar Units Breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Years, Months & Days", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${period.years} yr, ${period.months} mo, ${period.days} d",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Weeks & Days", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$weeks weeks${if (remainingDaysOfWeek > 0) " + $remainingDaysOfWeek days" else ""}",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Business days
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Working / Business Days", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("$workingDays days (Mon-Fri)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Weekend Days", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("$weekendDays days", fontWeight = FontWeight.SemiBold)
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Equivalent in Hours", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${totalDays * 24} hours", fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showStartPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        startDate = LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                    }
                    showStartPicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.toEpochDay() * 24 * 60 * 60 * 1000
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        endDate = LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                    }
                    showEndPicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateAddSubtractTab(
    fullFormatter: DateTimeFormatter,
    shortFormatter: DateTimeFormatter,
    database: AppDatabase
) {
    var baseDate by remember { mutableStateOf(LocalDate.now()) }
    var isAdd by remember { mutableStateOf(true) }

    var yearsStr by remember { mutableStateOf("0") }
    var monthsStr by remember { mutableStateOf("0") }
    var weeksStr by remember { mutableStateOf("0") }
    var daysStr by remember { mutableStateOf("7") }

    var showDatePicker by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val y = yearsStr.toLongOrNull() ?: 0L
    val m = monthsStr.toLongOrNull() ?: 0L
    val w = weeksStr.toLongOrNull() ?: 0L
    val d = daysStr.toLongOrNull() ?: 0L

    val resultDate = remember(baseDate, isAdd, y, m, w, d) {
        if (isAdd) {
            baseDate.plusYears(y).plusMonths(m).plusWeeks(w).plusDays(d)
        } else {
            baseDate.minusYears(y).minusMonths(m).minusWeeks(w).minusDays(d)
        }
    }

    val daysFromToday = ChronoUnit.DAYS.between(LocalDate.now(), resultDate)

    fun saveHistory() {
        coroutineScope.launch {
            val op = if (isAdd) "+" else "-"
            val durationDesc = buildList {
                if (y > 0) add("$y yr")
                if (m > 0) add("$m mo")
                if (w > 0) add("$w wk")
                if (d > 0 || isEmpty()) add("$d d")
            }.joinToString(", ")

            database.historyDao().insert(
                HistoryEntry(
                    toolName = "Date Adjuster",
                    inputSummary = "${baseDate.format(shortFormatter)} $op $durationDesc",
                    result = "${resultDate.format(fullFormatter)}"
                )
            )
        }
    }

    LaunchedEffect(baseDate, isAdd, y, m, w, d) {
        saveHistory()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Base Date Card
        Surface(
            onClick = { showDatePicker = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "STARTING FROM DATE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = baseDate.format(fullFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (baseDate.isEqual(LocalDate.now())) "Today" else "${ChronoUnit.DAYS.between(LocalDate.now(), baseDate)} days from today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(Icons.Filled.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Add or Subtract Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = isAdd,
                onClick = { isAdd = true },
                leadingIcon = {
                    if (isAdd) Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                label = { Text("Add to Date (+)") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = !isAdd,
                onClick = { isAdd = false },
                leadingIcon = {
                    if (!isAdd) Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                label = { Text("Subtract from Date (-)") },
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Presets
        Text(
            text = "Quick Jump:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                SuggestionChip(
                    onClick = {
                        yearsStr = "0"; monthsStr = "0"; weeksStr = "0"; daysStr = "1"
                    },
                    label = { Text("1 Day") }
                )
            }
            item {
                SuggestionChip(
                    onClick = {
                        yearsStr = "0"; monthsStr = "0"; weeksStr = "1"; daysStr = "0"
                    },
                    label = { Text("1 Week") }
                )
            }
            item {
                SuggestionChip(
                    onClick = {
                        yearsStr = "0"; monthsStr = "1"; weeksStr = "0"; daysStr = "0"
                    },
                    label = { Text("1 Month") }
                )
            }
            item {
                SuggestionChip(
                    onClick = {
                        yearsStr = "0"; monthsStr = "3"; weeksStr = "0"; daysStr = "0"
                    },
                    label = { Text("3 Months") }
                )
            }
            item {
                SuggestionChip(
                    onClick = {
                        yearsStr = "0"; monthsStr = "6"; weeksStr = "0"; daysStr = "0"
                    },
                    label = { Text("6 Months") }
                )
            }
            item {
                SuggestionChip(
                    onClick = {
                        yearsStr = "1"; monthsStr = "0"; weeksStr = "0"; daysStr = "0"
                    },
                    label = { Text("1 Year") }
                )
            }
        }

        // Duration Adjustment Inputs Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Adjust Duration:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Row for Years and Months
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = yearsStr,
                        onValueChange = { input ->
                            yearsStr = input.filter { it.isDigit() }.take(3)
                        },
                        label = { Text("Years") },
                        suffix = { Text("yr") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = monthsStr,
                        onValueChange = { input ->
                            monthsStr = input.filter { it.isDigit() }.take(3)
                        },
                        label = { Text("Months") },
                        suffix = { Text("mo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row for Weeks and Days
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = weeksStr,
                        onValueChange = { input ->
                            weeksStr = input.filter { it.isDigit() }.take(4)
                        },
                        label = { Text("Weeks") },
                        suffix = { Text("wk") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = daysStr,
                        onValueChange = { input ->
                            daysStr = input.filter { it.isDigit() }.take(4)
                        },
                        label = { Text("Days") },
                        suffix = { Text("d") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Result Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isAdd) "Target Future Date" else "Target Past Date",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = resultDate.format(fullFormatter),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        daysFromToday == 0L -> "Occurs TODAY"
                        daysFromToday > 0 -> "In $daysFromToday days from today"
                        else -> "${-daysFromToday} days ago"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }

        // Calendar Meta Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Date Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Day of the Year", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Day ${resultDate.dayOfYear} of ${resultDate.lengthOfYear()}", fontWeight = FontWeight.Medium)
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Calendar Week", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Week ${resultDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}", fontWeight = FontWeight.Medium)
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Leap Year Status", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (resultDate.isLeapYear) "Leap Year (366 days)" else "Common Year (365 days)", fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = baseDate.toEpochDay() * 24 * 60 * 60 * 1000
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        baseDate = LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                    }
                    showDatePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

