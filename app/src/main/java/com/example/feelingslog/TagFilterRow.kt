package com.example.feelingslog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TagFilterRow(
    tags: List<Tag>,
    selectedTagId: String?,
    onSelectTag: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            FilterChip(
                selected = selectedTagId == null,
                onClick = { onSelectTag(null) },
                label = { Text("All") }
            )
        }
        items(tags) { tag ->
            val customColor = tag.colorHex?.let { parseColorHex(it) }
            FilterChip(
                selected = selectedTagId == tag.id,
                onClick = { onSelectTag(tag.id) },
                label = { Text("#${tag.name}") },
                colors = if (customColor != null) {
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = customColor.copy(alpha = 0.8f),
                        selectedLabelColor = Color.White
                    )
                } else FilterChipDefaults.filterChipColors()
            )
        }
    }
}

fun parseColorHex(colorHex: String): Color? {
    return try {
        val hex = colorHex.removePrefix("#")
        val colorInt = hex.toLong(16).toInt()
        if (hex.length == 6) Color(colorInt or 0xFF000000.toInt())
        else Color(colorInt)
    } catch (e: Exception) {
        null
    }
}
