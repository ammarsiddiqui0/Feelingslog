package com.example.feelingslog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    var currentLog by remember { mutableStateOf<FeelingLog?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var originalTitle by remember { mutableStateOf("") }
    var originalContent by remember { mutableStateOf("") }

    var isEditMode by remember(logId) { mutableStateOf(logId == null) }
    var isLoading by remember { mutableStateOf(logId != null) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val hasUnsavedChanges = (title != originalTitle || content != originalContent)

    // Load existing log data
    LaunchedEffect(logId) {
        if (logId != null) {
            val log = viewModel.getLog(logId)
            log?.let {
                currentLog = it
                title = it.title
                originalTitle = it.title
                val textContent = (it.blocks.firstOrNull { b -> b is LogContentBlock.Text } as? LogContentBlock.Text)?.text ?: ""
                content = textContent
                originalContent = textContent
            }
            isLoading = false
        }
    }

    fun handleBack() {
        if (isEditMode && hasUnsavedChanges) {
            showDiscardDialog = true
        } else if (isEditMode && logId != null) {
            isEditMode = false
        } else {
            onNavigateBack()
        }
    }

    BackHandler {
        handleBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isEditMode && logId == null -> "New Log"
                            isEditMode -> "Edit Log"
                            else -> "View Log"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditMode) {
                        TextButton(onClick = {
                            if (hasUnsavedChanges) {
                                showDiscardDialog = true
                            } else if (logId != null) {
                                isEditMode = false
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = {
                            if (logId == null) {
                                viewModel.addLog(title, content)
                                onNavigateBack()
                            } else {
                                scope.launch {
                                    val log = currentLog ?: viewModel.getLog(logId)
                                    log?.let {
                                        viewModel.updateLog(it, title, content)
                                        originalTitle = title
                                        originalContent = content
                                        currentLog = it.copy(title = title)
                                    }
                                    isEditMode = false
                                }
                            }
                        }) {
                            Text("Save")
                        }
                    } else {
                        IconButton(onClick = { isEditMode = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete Log", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
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
                if (isEditMode) {
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
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Move to Recently Deleted?") },
            text = { Text("This log will be moved to Recently Deleted and kept for 30 days before permanent deletion.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    currentLog?.let { viewModel.softDeleteLog(it) }
                    onNavigateBack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Unsaved Changes?") },
            text = { Text("Are you sure you want to discard your changes?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    title = originalTitle
                    content = originalContent
                    if (logId != null) {
                        isEditMode = false
                    } else {
                        onNavigateBack()
                    }
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}
