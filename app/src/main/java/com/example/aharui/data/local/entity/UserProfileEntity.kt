package com.example.aharui.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val name: String? = null,
    val gender: String? = null,
    val dob: Long? = null,
    val heightCm: Double? = null,
    val currentWeightKg: Double? = null,
    val targetWeightKg: Double? = null,
    val goalType: String? = null,
    val dailyCalorieTarget: Int? = null,
    val activityLevel: String? = null
    // Omitting list types for simplicity in Room; these can be handled with TypeConverters or separate tables if needed.
)
