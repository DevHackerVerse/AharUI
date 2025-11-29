package com.example.aharui.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val date: String, // YYYY-MM-DD
    val weightKg: Double? = null,
    val totalCalories: Int = 0,
    val waterMl: Int = 0,
    val stepCount: Int? = null,
    val sleepHours: Double? = null,
    val wellnessScore: Int? = null,
    val notes: String? = null
)
