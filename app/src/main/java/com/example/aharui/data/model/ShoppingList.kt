package com.example.aharui.data.model

data class ShoppingList(
    val id: Long = 0,
    val title: String,
    val items: List<ShoppingItem>,
    val createdAt: Long = System.currentTimeMillis()
)

data class ShoppingItem(
    val name: String,
    val quantity: String,
    val isChecked: Boolean = false
)