package com.example.feelingslog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogClick: (String) -> Unit,
    onAddLogClick: () -> Unit = {},
    viewModel: LogViewModel = viewModel()
) {
    val logsWithDetails by viewModel.logsWithDetails.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    var selectedTagId by remember { mutableStateOf<String?>(null) }

    val filteredLogs = remember(logsWithDetails, selectedTagId) {
        if (selectedTagId == null) {
            logsWithDetails
        } else {
            logsWithDetails.filter { details ->
                details.tags.any { it.id == selectedTagId }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FeelingsLog", fontWeight = FontWeight.Bold) },
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
            // Tag Filter Row
            if (allTags.isNotEmpty()) {
                TagFilterRow(
                    tags = allTags,
                    selectedTagId = selectedTagId,
                    onSelectTag = { selectedTagId = it }
                )
            }

            // Masonry Grid
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalItemSpacing = 12.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredLogs) { logWithDetails ->
                    FeelingLogCard(
                        logWithDetails = logWithDetails,
                        onClick = { onLogClick(logWithDetails.log.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun FeelingLogCard(logWithDetails: FeelingLogWithDetails, onClick: () -> Unit) {
    val log = logWithDetails.log
    val context = LocalContext.current
    val repository = remember { LogRepository(context) }

    val coverFileName = log.backgroundImageFileName
        ?: log.coverImageFileName
        ?: logWithDetails.contentItems.firstOrNull { it.type == ContentType.IMAGE }?.fileName

    val coverFile = coverFileName?.let { repository.getMediaFile(it) }
    val previewText = logWithDetails.contentItems
        .filter { it.type == ContentType.TEXT }
        .sortedBy { it.topLevelOrdinal ?: 0 }
        .firstOrNull()?.textContent ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            if (coverFile != null && coverFile.exists()) {
                LocalImage(
                    file = coverFile,
                    contentDescription = log.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (log.backgroundImageFileName != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = log.backgroundDim))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = log.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (coverFile != null && coverFile.exists()) Color.White else MaterialTheme.colorScheme.onSurface
                )

                if (previewText.isNotEmpty()) {
                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (coverFile != null && coverFile.exists()) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    logWithDetails.tags.take(3).forEach { tag ->
                        TagChip(tag.name)
                    }
                }
            }
        }
    }
}

@Composable
fun TagChip(tag: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = tag,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
