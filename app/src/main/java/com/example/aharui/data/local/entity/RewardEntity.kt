package com.example.aharui.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey val userId: String,
    val pointsTotal: Int = 0,
    val streakDays: Int = 0,
    // Storing a list of badges as a JSON string for simplicity.
    val badges: String
)
