package com.example.aharui.data.local

import androidx.room.TypeConverter
import com.example.aharui.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    // Badge List Converters
    @TypeConverter
    fun fromBadgeList(value: List<Badge>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toBadgeList(value: String): List<Badge> {
        val listType = object : TypeToken<List<Badge>>() {}.type
        return gson.fromJson(value, listType)
    }

    // ShoppingItem List Converters
    @TypeConverter
    fun fromShoppingItemList(value: List<ShoppingItem>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toShoppingItemList(value: String): List<ShoppingItem> {
        val listType = object : TypeToken<List<ShoppingItem>>() {}.type
        return gson.fromJson(value, listType)
    }

    // Enum Converters
    @TypeConverter
    fun fromGender(value: Gender?): String? {
        return value?.name
    }

    @TypeConverter
    fun toGender(value: String?): Gender? {
        return value?.let { Gender.valueOf(it) }
    }

    @TypeConverter
    fun fromGoalType(value: GoalType): String {
        return value.name
    }

    @TypeConverter
    fun toGoalType(value: String): GoalType {
        return GoalType.valueOf(value)
    }

    @TypeConverter
    fun fromActivityLevel(value: ActivityLevel): String {
        return value.name
    }

    @TypeConverter
    fun toActivityLevel(value: String): ActivityLevel {
        return ActivityLevel.valueOf(value)
    }
}