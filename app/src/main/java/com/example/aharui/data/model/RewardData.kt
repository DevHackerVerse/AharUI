package com.example.aharui.data.model

data class RewardData(
    val pointsTotal: Int,
    val streakDays: Int,
    val badges: List<Badge>,
    val lastLoggedDate: String?
)