package com.example.aharui.data.model

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val iconRes: Int,
    val unlockedAt: Long? = null,
    val isUnlocked: Boolean = false
)