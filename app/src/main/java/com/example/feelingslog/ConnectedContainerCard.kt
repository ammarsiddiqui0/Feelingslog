package com.example.feelingslog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ConnectedContainerCard(
    container: LogContainer,
    childItems: List<LogContentItem>,
    isEditMode: Boolean,
    onUngroupItems: () -> Unit,
    onDeleteAll: () -> Unit,
    onToggleDirection: () -> Unit,
    onChangeSize: (String) -> Unit,
    modifier: Modifier = Modifier,
    renderItemContent: @Composable (LogContentItem) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val containerWidthModifier = when (container.size) {
        "SMALL" -> Modifier.fillMaxWidth(0.5f)
        "MEDIUM" -> Modifier.fillMaxWidth(0.75f)
        else -> Modifier.fillMaxWidth()
    }

    Card(
        modifier = modifier
            .then(containerWidthModifier)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (isEditMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connected Group (${container.layoutDirection.lowercase()})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Container Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (container.layoutDirection == "HORIZONTAL") "Switch to Vertical" else "Switch to Horizontal") },
                                onClick = {
                                    showMenu = false
                                    onToggleDirection()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Size: Small") },
                                onClick = {
                                    showMenu = false
                                    onChangeSize("SMALL")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Size: Medium") },
                                onClick = {
                                    showMenu = false
                                    onChangeSize("MEDIUM")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Size: Big (Full Width)") },
                                onClick = {
                                    showMenu = false
                                    onChangeSize("BIG")
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Un-group Items") },
                                onClick = {
                                    showMenu = false
                                    onUngroupItems()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Container & Contents", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // BoxWithConstraints for responsive layout fallback
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isNarrowScreen = maxWidth < 360.dp
                val useHorizontal = (container.layoutDirection == "HORIZONTAL") && !isNarrowScreen

                if (useHorizontal) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        childItems.forEach { child ->
                            Box(modifier = Modifier.weight(1f, fill = false)) {
                                renderItemContent(child)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        childItems.forEach { child ->
                            renderItemContent(child)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Container?") },
            text = { Text("What would you like to do with the items inside this connected group?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteAll()
                }) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onUngroupItems()
                }) {
                    Text("Un-group Items")
                }
            }
        )
    }
}
