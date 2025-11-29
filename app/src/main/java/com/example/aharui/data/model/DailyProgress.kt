package com.example.aharui.data.model

data class DailyProgress(
    val date: String,
    val totalCalories: Int,
    val calorieTarget: Int,
    val waterMl: Int,
    val waterTarget: Int,
    val weightKg: Double?,
    val mealsLogged: Int,
    val wellnessScore: Int?
)