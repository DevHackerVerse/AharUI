package com.example.aharui.data.model

data class WaterLog(
    val id: Long = 0,
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis()
)