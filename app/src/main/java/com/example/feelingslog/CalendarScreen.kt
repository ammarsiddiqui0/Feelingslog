package com.example.feelingslog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onLogClick: (String) -> Unit,
    viewModel: LogViewModel = viewModel()
) {
    // Consume calendarLogs to include both active and soft-deleted logs
    val logs by viewModel.calendarLogs.collectAsState()

    // Map logs by date key "YYYY-MM-DD"
    val logsByDate = remember(logs) {
        val map = mutableMapOf<String, MutableList<FeelingLog>>()
        val cal = Calendar.getInstance()
        logs.forEach { log ->
            cal.timeInMillis = log.createdAt
            val key = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
            map.getOrPut(key) { mutableListOf() }.add(log)
        }
        map
    }

    val todayCalendar = remember { Calendar.getInstance() }
    val todayYear = todayCalendar.get(Calendar.YEAR)
    val todayMonth = todayCalendar.get(Calendar.MONTH)
    val todayDay = todayCalendar.get(Calendar.DAY_OF_MONTH)

    var currentYear by remember { mutableIntStateOf(todayYear) }
    var currentMonth by remember { mutableIntStateOf(todayMonth) } // 0-indexed (0 = Jan, 11 = Dec)

    var selectedYear by remember { mutableIntStateOf(todayYear) }
    var selectedMonth by remember { mutableIntStateOf(todayMonth) }
    var selectedDay by remember { mutableIntStateOf(todayDay) }

    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val selectedDateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val monthTitle = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
        }
        monthFormatter.format(cal.time)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Month Header with Prev/Next Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (currentMonth == 0) {
                        currentMonth = 11
                        currentYear -= 1
                    } else {
                        currentMonth -= 1
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Month"
                    )
                }

                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    if (currentMonth == 11) {
                        currentMonth = 0
                        currentYear += 1
                    } else {
                        currentMonth += 1
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Month"
                    )
                }
            }

            // Day of Week Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid Calculations
            val monthCal = remember(currentYear, currentMonth) {
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                }
            }
            val startDayOffset = monthCal.get(Calendar.DAY_OF_WEEK) - 1
            val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val totalCells = startDayOffset + daysInMonth

            val rows = (totalCells + 6) / 7
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - startDayOffset + 1

                            if (cellIndex in startDayOffset until totalCells) {
                                val isToday = (currentYear == todayYear && currentMonth == todayMonth && dayNum == todayDay)
                                val isSelected = (currentYear == selectedYear && currentMonth == selectedMonth && dayNum == selectedDay)
                                val dateKey = "$currentYear-$currentMonth-$dayNum"
                                val dayLogs = logsByDate[dateKey] ?: emptyList()
                                val hasActiveLogs = dayLogs.any { it.deletedAt == null }
                                val hasDeletedLogs = dayLogs.any { it.deletedAt != null }

                                DayCell(
                                    day = dayNum,
                                    isToday = isToday,
                                    isSelected = isSelected,
                                    hasActiveLogs = hasActiveLogs,
                                    hasDeletedLogs = hasDeletedLogs,
                                    onClick = {
                                        selectedYear = currentYear
                                        selectedMonth = currentMonth
                                        selectedDay = dayNum
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Display logs for selected date below calendar
            val selectedDateKey = "$selectedYear-$selectedMonth-$selectedDay"
            val selectedDateLogs = logsByDate[selectedDateKey] ?: emptyList()
            val selectedDateText = remember(selectedYear, selectedMonth, selectedDay) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }
                selectedDateFormatter.format(cal.time)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Logs for $selectedDateText",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (selectedDateLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No logs on this date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(selectedDateLogs) { log ->
                            val isDeleted = log.deletedAt != null
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (!isDeleted) Modifier.clickable { onLogClick(log.id) }
                                        else Modifier
                                    ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                colors = if (isDeleted) {
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                } else CardDefaults.cardColors()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = log.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (isDeleted) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = "Recently Deleted",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }

                                    val previewText = (log.blocks.firstOrNull { it is LogContentBlock.Text } as? LogContentBlock.Text)?.text ?: ""
                                    if (previewText.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = previewText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }

                                    if (isDeleted) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { viewModel.restoreLog(log) },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Icon(
                                                Icons.Default.Restore,
                                                contentDescription = "Restore Log",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasActiveLogs: Boolean,
    hasDeletedLogs: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )

            if (hasActiveLogs || hasDeletedLogs) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (hasActiveLogs) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (hasDeletedLogs) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.error, CircleShape)
                        )
                    }
                }
            }
        }
    }
}
