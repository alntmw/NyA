package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "performance_logs")
data class PerformanceLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,            // Epoch millis of submission
    val timeTakenMillis: Long,      // Stopwatch time taken for this segment (since last submit/reset)
    val intervalMillis: Long,       // Real clock interval since the previous submission
    val label: String = ""          // Optional label/note for this specific log run
)
