package com.example.feelingslog

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class Tag(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String, // Preserves original display capitalization
    val colorHex: String? = null
)
