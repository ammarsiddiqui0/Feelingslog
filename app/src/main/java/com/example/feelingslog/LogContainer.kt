package com.example.feelingslog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "log_containers",
    foreignKeys = [
        ForeignKey(
            entity = FeelingLog::class,
            parentColumns = ["id"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("logId")]
)
data class LogContainer(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val logId: String,
    val topLevelOrdinal: Int,
    @ColumnInfo(defaultValue = "VERTICAL")
    val layoutDirection: String = "VERTICAL", // "HORIZONTAL" or "VERTICAL"
    @ColumnInfo(defaultValue = "MEDIUM")
    val size: String = "MEDIUM"               // "SMALL", "MEDIUM", "BIG"
)
