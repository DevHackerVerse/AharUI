package com.example.aharui.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_logs")
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val date: String,
    val mealType: String, // breakfast, lunch, etc.
    val foodName: String,
    val source: String, // manual, ai_plan, ocr
    val calories: Int,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
    val quantity: String? = null
)
