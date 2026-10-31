package com.example.feelingslog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsSectionHeader("Appearance")
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Palette,
                    title = "Theme",
                    subtitle = "System default (Light/Dark)"
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.ColorLens,
                    title = "Color Scheme",
                    subtitle = "Default Material 3 theme"
                )
            }

            item {
                SettingsSectionHeader("Typography")
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.TextFields,
                    title = "Font Style",
                    subtitle = "Default System Font"
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.FormatSize,
                    title = "Text Size",
                    subtitle = "Standard"
                )
            }

            item {
                SettingsSectionHeader("Log preferences")
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.ViewAgenda,
                    title = "Default Layout",
                    subtitle = "Masonry Grid (2 columns)"
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Tag,
                    title = "Tagging",
                    subtitle = "Manage log tags"
                )
            }

            item {
                SettingsSectionHeader("Storage and data")
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Storage,
                    title = "Storage Usage",
                    subtitle = "Local Room Database"
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.DeleteSweep,
                    title = "Recently Deleted",
                    subtitle = "Coming soon - Recover deleted logs"
                )
            }

            item {
                SettingsSectionHeader("Backup and Restore")
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.CloudUpload,
                    title = "Export Backup",
                    subtitle = "Backup logs to file"
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.CloudDownload,
                    title = "Restore Backup",
                    subtitle = "Restore logs from file"
                )
            }

            item {
                SettingsSectionHeader("General")
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = "About FeelingsLog",
                    subtitle = "Version 1.0.0"
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Lock,
                    title = "Privacy & Security",
                    subtitle = "Local device storage only"
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
