package com.example.feelingslog

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    logId: String?,
    onNavigateBack: () -> Unit,
    viewModel: LogViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(logId != null) }
    val scope = rememberCoroutineScope()

    // Load existing log data
    LaunchedEffect(logId) {
        if (logId != null) {
            val log = viewModel.getLog(logId)
            log?.let {
                title = it.title
                content = (it.blocks.firstOrNull { b -> b is LogContentBlock.Text } as? LogContentBlock.Text)?.text ?: ""
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (logId == null) "New Log" else "Edit Log") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (logId == null) {
                            viewModel.addLog(title, content)
                        } else {
                            scope.launch {
                                val log = viewModel.getLog(logId)
                                log?.let {
                                    viewModel.updateLog(it, title, content)
                                }
                            }
                        }
                        onNavigateBack()
                    }) {
                        Text("Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // Placeholders for Tags and Attachments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Tags")
                    Text("Tags (Coming Soon)", style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.Attachment, contentDescription = "Attachments")
                    Text("Attachments (Coming Soon)", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
