package com.example.feelingslog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.UUID

@Composable
fun TagSelectionDialog(
    allAvailableTags: List<Tag>,
    currentAssignedTags: List<Tag>,
    onDismiss: () -> Unit,
    onSaveTags: (List<Tag>) -> Unit
) {
    var assignedTags by remember { mutableStateOf(currentAssignedTags) }
    var newTagName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#2196F3") }

    val presetColors = listOf("#2196F3", "#4CAF50", "#FF9800", "#9C27B0", "#E91E63", "#607D8B", "#FF5722")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Log Tags", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Assigned Tags:", style = MaterialTheme.typography.labelMedium)
                if (assignedTags.isEmpty()) {
                    Text("No tags assigned yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        assignedTags.forEach { tag ->
                            AssistChip(
                                onClick = { assignedTags = assignedTags.filter { it.id != tag.id } },
                                label = { Text("#${tag.name}") },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }

                HorizontalDivider()

                Text("All Available Tags:", style = MaterialTheme.typography.labelMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allAvailableTags) { tag ->
                        val isAssigned = assignedTags.any { it.id == tag.id }
                        FilterChip(
                            selected = isAssigned,
                            onClick = {
                                if (isAssigned) {
                                    assignedTags = assignedTags.filter { it.id != tag.id }
                                } else {
                                    assignedTags = assignedTags + tag
                                }
                            },
                            label = { Text("#${tag.name}") }
                        )
                    }
                }

                HorizontalDivider()

                Text("Create New Tag:", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Tag Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Color picker row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presetColors.forEach { hex ->
                        val color = parseColorHex(hex) ?: Color.Blue
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selectedColorHex == hex) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                Button(
                    onClick = {
                        val trimmed = newTagName.trim()
                        if (trimmed.isNotEmpty()) {
                            val normalized = trimmed.lowercase()
                            val tagId = UUID.nameUUIDFromBytes(normalized.toByteArray()).toString()
                            val newTag = Tag(id = tagId, name = trimmed, colorHex = selectedColorHex)
                            if (!assignedTags.any { it.id == newTag.id }) {
                                assignedTags = assignedTags + newTag
                            }
                            newTagName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add New Tag")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSaveTags(assignedTags) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
