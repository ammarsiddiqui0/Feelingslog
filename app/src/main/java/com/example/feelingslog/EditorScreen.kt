package com.example.feelingslog

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    logId: String?,
    onNavigateBack: () -> Unit,
    viewModel: LogViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentLogId = remember(logId) { logId ?: UUID.randomUUID().toString() }

    val audioPlaybackManager = remember { AudioPlaybackManager(context) }
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(audioPlaybackManager)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(audioPlaybackManager)
        }
    }

    var currentLogWithDetails by remember { mutableStateOf<FeelingLogWithDetails?>(null) }
    var title by remember { mutableStateOf("") }
    var originalTitle by remember { mutableStateOf("") }

    var itemsState by remember { mutableStateOf<List<LogContentItem>>(emptyList()) }
    var originalItemsState by remember { mutableStateOf<List<LogContentItem>>(emptyList()) }

    var tagsState by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var containersState by remember { mutableStateOf<List<LogContainer>>(emptyList()) }

    // Atmosphere states
    var backgroundImageFileNameState by remember { mutableStateOf<String?>(null) }
    var backgroundDimState by remember { mutableFloatStateOf(0.3f) }
    var coverImageFileNameState by remember { mutableStateOf<String?>(null) }
    var themeAudioFileNameState by remember { mutableStateOf<String?>(null) }
    var themeAudioAutoPlayState by remember { mutableStateOf(false) }

    var isEditMode by remember(logId) { mutableStateOf(logId == null) }
    var isLoading by remember { mutableStateOf(logId != null) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAtmosphereDialog by remember { mutableStateOf(false) }

    // Selection mode for containers
    var selectedItemIds by remember { mutableStateOf(setOf<String>()) }

    val hasUnsavedChanges = (title != originalTitle || itemsState != originalItemsState)

    // System File Pickers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val result = viewModel.copyUriToLocalMedia(context, it, "img")
            result?.let { (fileName, mimeType) ->
                val newItem = LogContentItem(
                    logId = currentLogId,
                    type = ContentType.IMAGE,
                    topLevelOrdinal = itemsState.size,
                    fileName = fileName,
                    mimeType = mimeType,
                    size = ItemSize.MEDIUM,
                    alignment = ItemAlignment.LEFT
                )
                itemsState = itemsState + newItem
            }
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val result = viewModel.copyUriToLocalMedia(context, it, "aud")
            result?.let { (fileName, mimeType) ->
                val newItem = LogContentItem(
                    logId = currentLogId,
                    type = ContentType.AUDIO,
                    topLevelOrdinal = itemsState.size,
                    fileName = fileName,
                    mimeType = mimeType,
                    size = ItemSize.MEDIUM
                )
                itemsState = itemsState + newItem
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val result = viewModel.copyUriToLocalMedia(context, it, "doc")
            result?.let { (fileName, mimeType) ->
                val newItem = LogContentItem(
                    logId = currentLogId,
                    type = ContentType.DOCUMENT,
                    topLevelOrdinal = itemsState.size,
                    fileName = fileName,
                    mimeType = mimeType
                )
                itemsState = itemsState + newItem
            }
        }
    }

    val backgroundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val result = viewModel.copyUriToLocalMedia(context, it, "bg")
            result?.let { (fileName, _) ->
                backgroundImageFileNameState = fileName
            }
        }
    }

    val themeAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val result = viewModel.copyUriToLocalMedia(context, it, "theme_aud")
            result?.let { (fileName, _) ->
                themeAudioFileNameState = fileName
                val file = viewModel.getMediaFile(fileName)
                audioPlaybackManager.playThemeAudio(file, themeAudioAutoPlayState)
            }
        }
    }

    // Load existing log data
    LaunchedEffect(logId) {
        if (logId != null) {
            val details = viewModel.getLogWithDetails(logId)
            details?.let {
                currentLogWithDetails = it
                title = it.log.title
                originalTitle = it.log.title
                val sortedItems = it.contentItems.sortedBy { item -> item.topLevelOrdinal ?: 0 }
                itemsState = sortedItems
                originalItemsState = sortedItems
                tagsState = it.tags
                containersState = it.containers

                backgroundImageFileNameState = it.log.backgroundImageFileName
                backgroundDimState = it.log.backgroundDim
                coverImageFileNameState = it.log.coverImageFileName
                themeAudioFileNameState = it.log.themeAudioFileName
                themeAudioAutoPlayState = it.log.themeAudioAutoPlay

                if (it.log.themeAudioFileName != null) {
                    val audioFile = viewModel.getMediaFile(it.log.themeAudioFileName!!)
                    audioPlaybackManager.playThemeAudio(audioFile, it.log.themeAudioAutoPlay)
                }
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
            audioPlaybackManager.stopThemeAudio()
            onNavigateBack()
        }
    }

    BackHandler {
        handleBack()
    }

    fun moveItemUp(index: Int) {
        if (index > 0) {
            val list = itemsState.toMutableList()
            val temp = list[index]
            list[index] = list[index - 1]
            list[index - 1] = temp
            itemsState = list.mapIndexed { idx, item -> item.copy(topLevelOrdinal = idx) }
        }
    }

    fun moveItemDown(index: Int) {
        if (index < itemsState.size - 1) {
            val list = itemsState.toMutableList()
            val temp = list[index]
            list[index] = list[index + 1]
            list[index + 1] = temp
            itemsState = list.mapIndexed { idx, item -> item.copy(topLevelOrdinal = idx) }
        }
    }

    fun deleteItem(index: Int) {
        val list = itemsState.toMutableList()
        list.removeAt(index)
        itemsState = list.mapIndexed { idx, item -> item.copy(topLevelOrdinal = idx) }
    }

    val backgroundFile = backgroundImageFileNameState?.let { viewModel.getMediaFile(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            when {
                                isEditMode && logId == null -> "New Log"
                                isEditMode -> "Edit Log"
                                else -> "View Log"
                            }
                        )
                        if (themeAudioFileNameState != null) {
                            val isThemePlaying by audioPlaybackManager.isThemeAudioPlaying.collectAsState()
                            IconButton(
                                onClick = { audioPlaybackManager.toggleThemeAudio() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isThemePlaying) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                    contentDescription = "Toggle Theme Audio"
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAtmosphereDialog = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "Atmosphere Settings")
                    }

                    if (isEditMode) {
                        TextButton(onClick = { handleBack() }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = {
                            val activeLog = currentLogWithDetails?.log ?: FeelingLog(id = currentLogId, title = title.ifBlank { "Untitled Log" })
                            val finalLog = activeLog.copy(
                                title = title.ifBlank { "Untitled Log" },
                                backgroundImageFileName = backgroundImageFileNameState,
                                backgroundDim = backgroundDimState,
                                coverImageFileName = coverImageFileNameState,
                                themeAudioFileName = themeAudioFileNameState,
                                themeAudioAutoPlay = themeAudioAutoPlayState
                            )
                            val finalItems = itemsState.map { it.copy(logId = currentLogId) }
                            val finalContainers = containersState.map { it.copy(logId = currentLogId) }
                            viewModel.saveFullLog(finalLog, finalContainers, finalItems, tagsState)
                            audioPlaybackManager.stopThemeAudio()
                            if (logId == null) {
                                onNavigateBack()
                            } else {
                                originalTitle = title
                                originalItemsState = finalItems
                                isEditMode = false
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
        },
        bottomBar = {
            if (isEditMode) {
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Prominent Connect Selected Items Bar when >= 2 items selected
                        if (selectedItemIds.size >= 2) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${selectedItemIds.size} items selected for group", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { selectedItemIds = emptySet() }) {
                                        Text("Clear")
                                    }
                                    Button(
                                        onClick = {
                                            val newContainerId = UUID.randomUUID().toString()
                                            val nextOrdinal = containersState.size
                                            val newContainer = LogContainer(
                                                id = newContainerId,
                                                logId = currentLogId,
                                                topLevelOrdinal = nextOrdinal,
                                                layoutDirection = "VERTICAL",
                                                size = "MEDIUM"
                                            )
                                            containersState = containersState + newContainer

                                            val updatedItems = itemsState.map { item ->
                                                if (item.id in selectedItemIds) {
                                                    val childOrd = itemsState.filter { it.containerId == newContainerId }.size
                                                    item.copy(containerId = newContainerId, topLevelOrdinal = null, itemOrdinal = childOrd)
                                                } else {
                                                    item
                                                }
                                            }
                                            itemsState = updatedItems
                                            selectedItemIds = emptySet()
                                        }
                                    ) {
                                        Text("Connect selected items")
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(onClick = {
                                val newItem = LogContentItem(
                                    logId = currentLogId,
                                    type = ContentType.TEXT,
                                    topLevelOrdinal = itemsState.size,
                                    textContent = ""
                                )
                                itemsState = itemsState + newItem
                            }) {
                                Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Text")
                            }

                            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Image")
                            }

                            Button(onClick = { audioPickerLauncher.launch("audio/*") }) {
                                Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Audio")
                            }

                            Button(onClick = { documentPickerLauncher.launch("*/*") }) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Doc")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (backgroundFile != null && backgroundFile.exists()) {
                LocalImage(
                    file = backgroundFile,
                    contentDescription = "Log Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = backgroundDimState))
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val topLevelEntries = remember(itemsState, containersState) {
                    val standalone = itemsState.filter { it.containerId == null }.map { TopLevelEntry.StandaloneItem(it) }
                    val groups = containersState.map { container ->
                        val children = itemsState.filter { it.containerId == container.id }.sortedBy { it.itemOrdinal }
                        TopLevelEntry.ContainerGroup(container, children)
                    }
                    (standalone + groups).sortedBy { it.topLevelOrdinal }
                }

                LaunchedEffect(topLevelEntries) {
                    val normalizedContainers = containersState.map { container ->
                        val idx = topLevelEntries.indexOfFirst { it is TopLevelEntry.ContainerGroup && it.container.id == container.id }
                        container.copy(topLevelOrdinal = if (idx != -1) idx else container.topLevelOrdinal)
                    }
                    val normalizedItems = itemsState.map { item ->
                        if (item.containerId == null) {
                            val idx = topLevelEntries.indexOfFirst { it is TopLevelEntry.StandaloneItem && it.item.id == item.id }
                            item.copy(topLevelOrdinal = if (idx != -1) idx else item.topLevelOrdinal)
                        } else {
                            item
                        }
                    }
                    containersState = normalizedContainers
                    itemsState = normalizedItems
                }

                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isEditMode) {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("Log Title") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = if (backgroundFile != null) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else Color.Transparent,
                                        unfocusedContainerColor = if (backgroundFile != null) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else Color.Transparent
                                    )
                                )
                            } else {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (backgroundFile != null) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (!isEditMode) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tip: Check boxes on items below to group them into a Connected Container.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (backgroundFile != null) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (topLevelEntries.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isEditMode) "Add content blocks below (+ Text, + Image, + Audio, + Doc)" else "No content in this log",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (backgroundFile != null) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        itemsIndexed(topLevelEntries) { _, entry ->
                            when (entry) {
                                is TopLevelEntry.StandaloneItem -> {
                                    val item = entry.item
                                    val originalIndex = itemsState.indexOfFirst { it.id == item.id }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (isEditMode) {
                                            Checkbox(
                                                checked = item.id in selectedItemIds,
                                                onCheckedChange = { checked ->
                                                    selectedItemIds = if (checked) selectedItemIds + item.id else selectedItemIds - item.id
                                                }
                                            )
                                        }

                                        Box(modifier = Modifier.weight(1f)) {
                                            ContentBlockCard(
                                                item = item,
                                                index = originalIndex,
                                                totalCount = itemsState.size,
                                                isEditMode = isEditMode,
                                                viewModel = viewModel,
                                                audioPlaybackManager = audioPlaybackManager,
                                                coverImageFileName = coverImageFileNameState,
                                                onSetCover = { coverImageFileNameState = it },
                                                onRemoveCover = { coverImageFileNameState = null },
                                                onUpdateItem = { updatedItem ->
                                                    itemsState = itemsState.map { if (it.id == updatedItem.id) updatedItem else it }
                                                },
                                                onMoveUp = { if (originalIndex != -1) moveItemUp(originalIndex) },
                                                onMoveDown = { if (originalIndex != -1) moveItemDown(originalIndex) },
                                                onDelete = { if (originalIndex != -1) deleteItem(originalIndex) }
                                            )
                                        }
                                    }
                                }

                                is TopLevelEntry.ContainerGroup -> {
                                    val container = entry.container
                                    val childItems = entry.childItems

                                    ConnectedContainerCard(
                                        container = container,
                                        childItems = childItems,
                                        isEditMode = isEditMode,
                                        onUngroupItems = {
                                            containersState = containersState.filter { it.id != container.id }
                                            val unlinked = childItems.map { it.copy(containerId = null, topLevelOrdinal = itemsState.size) }
                                            itemsState = itemsState.map { item -> unlinked.find { it.id == item.id } ?: item }
                                        },
                                        onDeleteAll = {
                                            containersState = containersState.filter { it.id != container.id }
                                            val childIds = childItems.map { it.id }
                                            itemsState = itemsState.filter { it.id !in childIds }
                                        },
                                        onToggleDirection = {
                                            containersState = containersState.map {
                                                if (it.id == container.id) it.copy(layoutDirection = if (it.layoutDirection == "HORIZONTAL") "VERTICAL" else "HORIZONTAL") else it
                                            }
                                        },
                                        onChangeSize = { newSize ->
                                            containersState = containersState.map {
                                                if (it.id == container.id) it.copy(size = newSize) else it
                                            }
                                        }
                                    ) { childItem ->
                                        val originalIndex = itemsState.indexOfFirst { it.id == childItem.id }
                                        ContentBlockCard(
                                            item = childItem,
                                            index = originalIndex,
                                            totalCount = itemsState.size,
                                            isEditMode = isEditMode,
                                            viewModel = viewModel,
                                            audioPlaybackManager = audioPlaybackManager,
                                            coverImageFileName = coverImageFileNameState,
                                            onSetCover = { coverImageFileNameState = it },
                                            onRemoveCover = { coverImageFileNameState = null },
                                            onUpdateItem = { updatedItem ->
                                                itemsState = itemsState.map { if (it.id == updatedItem.id) updatedItem else it }
                                            },
                                            onMoveUp = { if (originalIndex != -1) moveItemUp(originalIndex) },
                                            onMoveDown = { if (originalIndex != -1) moveItemDown(originalIndex) },
                                            onDelete = { if (originalIndex != -1) deleteItem(originalIndex) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        var showTagDialog by remember { mutableStateOf(false) }
                        val allAvailableTags by viewModel.allTags.collectAsState()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isEditMode) { showTagDialog = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Label,
                                contentDescription = "Tags",
                                tint = if (backgroundFile != null) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            if (tagsState.isEmpty()) {
                                Text(
                                    text = if (isEditMode) "Add tags (Tap to manage)" else "No tags",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (backgroundFile != null) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    tagsState.forEach { tag ->
                                        TagChip(tag.name)
                                    }
                                }
                            }
                        }

                        if (showTagDialog) {
                            TagSelectionDialog(
                                allAvailableTags = allAvailableTags,
                                currentAssignedTags = tagsState,
                                onDismiss = { showTagDialog = false },
                                onSaveTags = {
                                    tagsState = it
                                    showTagDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAtmosphereDialog) {
        AlertDialog(
            onDismissRequest = { showAtmosphereDialog = false },
            title = { Text("Log Atmosphere Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Background Image", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { backgroundPickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (backgroundImageFileNameState == null) "Select Background" else "Change Background")
                        }
                        if (backgroundImageFileNameState != null) {
                            OutlinedButton(
                                onClick = { backgroundImageFileNameState = null },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Remove")
                            }
                        }
                    }

                    if (backgroundImageFileNameState != null) {
                        Text("Background Dim: ${(backgroundDimState * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = backgroundDimState,
                            onValueChange = { backgroundDimState = it },
                            valueRange = 0f..1f
                        )
                    }

                    HorizontalDivider()

                    Text("Theme Audio (Atmosphere)", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { themeAudioPickerLauncher.launch("audio/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (themeAudioFileNameState == null) "Select Theme Audio" else "Change Audio")
                        }
                        if (themeAudioFileNameState != null) {
                            OutlinedButton(
                                onClick = {
                                    audioPlaybackManager.stopThemeAudio()
                                    themeAudioFileNameState = null
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Remove")
                            }
                        }
                    }

                    if (themeAudioFileNameState != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Autoplay on Open", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = themeAudioAutoPlayState,
                                onCheckedChange = { themeAudioAutoPlayState = it }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAtmosphereDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Move to Recently Deleted?") },
            text = { Text("This log will be moved to Recently Deleted and kept for 30 days before permanent deletion.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    currentLogWithDetails?.log?.let { viewModel.softDeleteLog(it) }
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
                    itemsState = originalItemsState
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

@Composable
fun ContentBlockCard(
    item: LogContentItem,
    index: Int,
    totalCount: Int,
    isEditMode: Boolean,
    viewModel: LogViewModel,
    audioPlaybackManager: AudioPlaybackManager,
    coverImageFileName: String?,
    onSetCover: (String?) -> Unit,
    onRemoveCover: () -> Unit,
    onUpdateItem: (LogContentItem) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    val itemWidthModifier = when (item.size) {
        ItemSize.SMALL -> Modifier.fillMaxWidth(0.5f)
        ItemSize.MEDIUM -> Modifier.fillMaxWidth(0.75f)
        else -> Modifier.fillMaxWidth()
    }

    Card(
        modifier = Modifier
            .then(itemWidthModifier)
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (isEditMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Block ${index + 1}: ${item.type.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.type == ContentType.IMAGE) {
                            val isCover = coverImageFileName == item.fileName
                            TextButton(onClick = {
                                if (isCover) onRemoveCover() else onSetCover(item.fileName)
                            }) {
                                Text(if (isCover) "Cover ✓" else "Set Cover", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (index > 0) {
                            IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                            }
                        }
                        if (index < totalCount - 1) {
                            IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                            }
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            when (item.type) {
                ContentType.TEXT -> {
                    val fontFamily = when (item.fontStyle) {
                        FontStyleOption.SERIF -> FontFamily.Serif
                        FontStyleOption.MONOSPACE -> FontFamily.Monospace
                        FontStyleOption.CURSIVE -> FontFamily.Cursive
                        else -> FontFamily.Default
                    }

                    if (isEditMode) {
                        OutlinedTextField(
                            value = item.textContent ?: "",
                            onValueChange = { onUpdateItem(item.copy(textContent = it)) },
                            label = { Text("Text Content") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontFamily = fontFamily)
                        )
                    } else {
                        Text(
                            text = item.textContent ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = fontFamily,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                ContentType.IMAGE -> {
                    val mediaFile = item.fileName?.let { viewModel.getMediaFile(it) }

                    if (mediaFile != null && mediaFile.exists()) {
                        LocalImage(
                            file = mediaFile,
                            contentDescription = item.caption ?: "Log Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Image file: ${item.fileName ?: "unknown"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (isEditMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = item.caption ?: "",
                            onValueChange = { onUpdateItem(item.copy(caption = it)) },
                            label = { Text("Image Caption (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Size:", style = MaterialTheme.typography.labelSmall)
                            ItemSize.entries.forEach { s ->
                                FilterChip(
                                    selected = item.size == s,
                                    onClick = { onUpdateItem(item.copy(size = s)) },
                                    label = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    } else if (!item.caption.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ContentType.AUDIO -> {
                    val mediaFile = item.fileName?.let { viewModel.getMediaFile(it) }
                    var isPlaying by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    audioPlaybackManager.pauseInlineAudio()
                                    isPlaying = false
                                } else {
                                    if (mediaFile != null && mediaFile.exists()) {
                                        audioPlaybackManager.playInlineAudio(mediaFile) {
                                            isPlaying = false
                                        }
                                        isPlaying = true
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = if (isPlaying) "Pause Audio" else "Play Audio",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.fileName ?: "Audio File",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                ContentType.DOCUMENT -> {
                    val mediaFile = item.fileName?.let { viewModel.getMediaFile(it) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.InsertDriveFile,
                            contentDescription = "Document Attachment",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.fileName ?: "Document",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (mediaFile != null && mediaFile.exists()) {
                                    FileProviderUtil.openDocument(context, mediaFile, item.mimeType)
                                }
                            }
                        ) {
                            Text("Open")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocalImage(
    file: File,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val bitmap = remember(file) {
        if (file.exists()) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("Image unavailable", style = MaterialTheme.typography.bodySmall)
        }
    }
}
