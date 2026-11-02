package com.example.feelingslog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyDeletedScreen(
    onNavigateBack: () -> Unit,
    viewModel: LogViewModel = viewModel()
) {
    val deletedLogsWithDetails by viewModel.deletedLogsWithDetails.collectAsState()
    var logToPermanentlyDelete by remember { mutableStateOf<FeelingLog?>(null) }
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recently Deleted", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        if (deletedLogsWithDetails.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recently deleted logs",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(deletedLogsWithDetails) { item ->
                    val log = item.log
                    val deletedAt = log.deletedAt ?: System.currentTimeMillis()
                    val daysElapsed = ((System.currentTimeMillis() - deletedAt) / (1000 * 60 * 60 * 24)).toInt()
                    val daysRemaining = maxOf(0, 30 - daysElapsed)

                    val deletedDateText = remember(deletedAt) {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = deletedAt
                        dateFormatter.format(cal.time)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
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
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "Expires in $daysRemaining ${if (daysRemaining == 1) "day" else "days"}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            val previewText = item.contentItems
                                .filter { it.type == ContentType.TEXT }
                                .sortedBy { it.topLevelOrdinal ?: 0 }
                                .firstOrNull()?.textContent ?: ""

                            if (previewText.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = previewText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Deleted on $deletedDateText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { logToPermanentlyDelete = log }
                                ) {
                                    Icon(
                                        Icons.Default.DeleteForever,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = { viewModel.restoreLog(log) }
                                ) {
                                    Icon(
                                        Icons.Default.Restore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    logToPermanentlyDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { logToPermanentlyDelete = null },
            title = { Text("Permanently Delete Log?") },
            text = { Text("This log and its associated media will be permanently deleted. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        logToPermanentlyDelete = null
                        viewModel.permanentlyDeleteLog(log)
                    }
                ) {
                    Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { logToPermanentlyDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
