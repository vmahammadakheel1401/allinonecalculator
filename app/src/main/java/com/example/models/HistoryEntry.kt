package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_entries")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val toolName: String,
    val inputSummary: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)
