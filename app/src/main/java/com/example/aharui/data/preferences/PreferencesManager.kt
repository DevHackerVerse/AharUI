package com.example.aharui.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.aharui.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ==================== EXISTING PREFERENCES ====================

    var currentUserId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit { putString(KEY_USER_ID, value) }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit { putBoolean(KEY_IS_LOGGED_IN, value) }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit { putBoolean(KEY_FIRST_LAUNCH, value) }

    var waterReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_WATER_REMINDER, true)
        set(value) = prefs.edit { putBoolean(KEY_WATER_REMINDER, value) }

    var mealReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_MEAL_REMINDER, true)
        set(value) = prefs.edit { putBoolean(KEY_MEAL_REMINDER, value) }

    var dailyWaterGoal: Int
        get() = prefs.getInt(KEY_WATER_GOAL, 2500)
        set(value) = prefs.edit { putInt(KEY_WATER_GOAL, value) }

    var lastStreakCheckDate: String?
        get() = prefs.getString(KEY_LAST_STREAK_CHECK, null)
        set(value) = prefs.edit { putString(KEY_LAST_STREAK_CHECK, value) }

    // ==================== USER PROFILE PREFERENCES (NEW) ====================

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit { putString(KEY_USER_NAME, value) }

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit { putString(KEY_USER_EMAIL, value) }

    var gender: Gender?
        get() = prefs.getString(KEY_GENDER, null)?.let {
            try {
                Gender.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
        set(value) = prefs.edit { putString(KEY_GENDER, value?.name) }

    var dateOfBirth: Long?
        get() = prefs.getLong(KEY_DATE_OF_BIRTH, 0L).takeIf { it > 0 }
        set(value) = prefs.edit { putLong(KEY_DATE_OF_BIRTH, value ?: 0L) }

    var heightCm: Double?
        get() = prefs.getFloat(KEY_HEIGHT_CM, 0f).toDouble().takeIf { it > 0 }
        set(value) = prefs.edit { putFloat(KEY_HEIGHT_CM, value?.toFloat() ?: 0f) }

    var currentWeightKg: Double?
        get() = prefs.getFloat(KEY_CURRENT_WEIGHT_KG, 0f).toDouble().takeIf { it > 0 }
        set(value) = prefs.edit { putFloat(KEY_CURRENT_WEIGHT_KG, value?.toFloat() ?: 0f) }

    var targetWeightKg: Double?
        get() = prefs.getFloat(KEY_TARGET_WEIGHT_KG, 0f).toDouble().takeIf { it > 0 }
        set(value) = prefs.edit { putFloat(KEY_TARGET_WEIGHT_KG, value?.toFloat() ?: 0f) }

    var goalType: GoalType?
        get() = prefs.getString(KEY_GOAL_TYPE, null)?.let {
            try {
                GoalType.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
        set(value) = prefs.edit { putString(KEY_GOAL_TYPE, value?.name) }

    var activityLevel: ActivityLevel?
        get() = prefs.getString(KEY_ACTIVITY_LEVEL, null)?.let {
            try {
                ActivityLevel.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
        set(value) = prefs.edit { putString(KEY_ACTIVITY_LEVEL, value?.name) }

    var dailyCalorieTarget: Int?
        get() = prefs.getInt(KEY_DAILY_CALORIE_TARGET, 0).takeIf { it > 0 }
        set(value) = prefs.edit { putInt(KEY_DAILY_CALORIE_TARGET, value ?: 0) }

    var dailyWaterTarget: Int?
        get() = prefs.getInt(KEY_DAILY_WATER_TARGET, 2500)
        set(value) = prefs.edit { putInt(KEY_DAILY_WATER_TARGET, value ?: 2500) }

    // ==================== HELPER METHODS ====================

    /**
     * Get complete user profile
     */
    fun getUserProfile(): UserProfile? {
        val userId = currentUserId ?: return null

        return UserProfile(
            userId = userId,
            name = userName ?: "User",
            email = userEmail,
            gender = gender,
            dateOfBirth = dateOfBirth,
            heightCm = heightCm,
            currentWeightKg = currentWeightKg,
            targetWeightKg = targetWeightKg,
            goalType = goalType ?: GoalType.GENERAL_HEALTH,
            dailyCalorieTarget = dailyCalorieTarget ?: 2000,
            dailyWaterTarget = dailyWaterTarget ?: 2500,
            activityLevel = activityLevel ?: ActivityLevel.MODERATELY_ACTIVE
        )
    }

    /**
     * Save complete user profile
     */
    fun saveUserProfile(profile: UserProfile) {
        currentUserId = profile.userId
        userName = profile.name
        userEmail = profile.email
        gender = profile.gender
        dateOfBirth = profile.dateOfBirth
        heightCm = profile.heightCm
        currentWeightKg = profile.currentWeightKg
        targetWeightKg = profile.targetWeightKg
        goalType = profile.goalType
        dailyCalorieTarget = profile.dailyCalorieTarget
        dailyWaterTarget = profile.dailyWaterTarget
        activityLevel = profile.activityLevel
    }

    /**
     * Check if user profile is complete
     */
    fun isProfileComplete(): Boolean {
        return currentUserId != null &&
                heightCm != null && heightCm!! > 0 &&
                currentWeightKg != null && currentWeightKg!! > 0 &&
                targetWeightKg != null && targetWeightKg!! > 0 &&
                goalType != null &&
                activityLevel != null
    }

    /**
     * Get profile completion percentage
     */
    fun getProfileCompletionPercentage(): Int {
        var completed = 0
        val total = 10

        if (userName != null) completed++
        if (userEmail != null) completed++
        if (gender != null) completed++
        if (dateOfBirth != null) completed++
        if (heightCm != null && heightCm!! > 0) completed++
        if (currentWeightKg != null && currentWeightKg!! > 0) completed++
        if (targetWeightKg != null && targetWeightKg!! > 0) completed++
        if (goalType != null) completed++
        if (activityLevel != null) completed++
        if (dailyCalorieTarget != null) completed++

        return (completed * 100) / total
    }

    fun clearAll() {
        prefs.edit { clear() }
    }

    companion object {
        private const val PREFS_NAME = "neurodiet_prefs"

        // Existing keys
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_WATER_REMINDER = "water_reminder"
        private const val KEY_MEAL_REMINDER = "meal_reminder"
        private const val KEY_WATER_GOAL = "water_goal"
        private const val KEY_LAST_STREAK_CHECK = "last_streak_check"

        // New user profile keys
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_GENDER = "gender"
        private const val KEY_DATE_OF_BIRTH = "date_of_birth"
        private const val KEY_HEIGHT_CM = "height_cm"
        private const val KEY_CURRENT_WEIGHT_KG = "current_weight_kg"
        private const val KEY_TARGET_WEIGHT_KG = "target_weight_kg"
        private const val KEY_GOAL_TYPE = "goal_type"
        private const val KEY_ACTIVITY_LEVEL = "activity_level"
        private const val KEY_DAILY_CALORIE_TARGET = "daily_calorie_target"
        private const val KEY_DAILY_WATER_TARGET = "daily_water_target"
    }
}