package com.example.aharui.data.repository

import com.example.aharui.data.local.dao.UserProfileDao
import com.example.aharui.data.local.entity.UserProfileEntity
import com.example.aharui.data.model.*
import com.example.aharui.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val userProfileDao: UserProfileDao
) {

    /**
     * Get user profile as Flow (auto-updates when data changes)
     */
    fun getUserProfile(userId: String): Flow<UserProfile?> {
        return userProfileDao.getProfile(userId).map { entity ->
            entity?.toModel()
        }
    }

    /**
     * Get user profile synchronously (for one-time reads)
     * FIXED: Now properly retrieves profile without hanging
     */
    suspend fun getUserProfileSync(userId: String): UserProfile? {
        return userProfileDao.getProfileSync(userId)?.toModel()
    }

    /**
     * Save or update user profile
     */
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            userProfileDao.insertOrUpdateProfile(profile.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to save profile")
        }
    }

    /**
     * Update current weight
     */
    suspend fun updateWeight(userId: String, weight: Double): Result<Unit> {
        return try {
            userProfileDao.updateWeight(userId, weight)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update weight")
        }
    }

    /**
     * Update daily calorie target
     */
    suspend fun updateCalorieTarget(userId: String, target: Int): Result<Unit> {
        return try {
            userProfileDao.updateCalorieTarget(userId, target)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update calorie target")
        }
    }

    /**
     * Check if profile exists
     */
    suspend fun profileExists(userId: String): Boolean {
        return userProfileDao.profileExists(userId)
    }

    /**
     * Check if profile is complete (has required fields)
     */
    suspend fun isProfileComplete(userId: String): Boolean {
        val profile = getUserProfileSync(userId) ?: return false
        return profile.heightCm != null && profile.heightCm!! > 0 &&
                profile.currentWeightKg != null && profile.currentWeightKg!! > 0 &&
                profile.targetWeightKg != null && profile.targetWeightKg!! > 0
    }

    /**
     * Delete profile
     */
    suspend fun deleteProfile(userId: String): Result<Unit> {
        return try {
            userProfileDao.deleteProfile(userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete profile")
        }
    }

    // ============= MAPPING FUNCTIONS =============

    private fun UserProfileEntity.toModel() = UserProfile(
        userId = userId,
        name = name ?: "User",
        email = null, // Not stored in your entity
        gender = gender?.let {
            try {
                Gender.valueOf(it)
            } catch (e: Exception) {
                null
            }
        },
        dateOfBirth = dob,
        heightCm = heightCm,
        currentWeightKg = currentWeightKg,
        targetWeightKg = targetWeightKg,
        goalType = goalType?.let {
            try {
                GoalType.valueOf(it)
            } catch (e: Exception) {
                GoalType.GENERAL_HEALTH
            }
        } ?: GoalType.GENERAL_HEALTH,
        dailyCalorieTarget = dailyCalorieTarget ?: 2000,
        dailyWaterTarget = 2500, // Not in your entity, using default
        activityLevel = activityLevel?.let {
            try {
                ActivityLevel.valueOf(it)
            } catch (e: Exception) {
                ActivityLevel.MODERATELY_ACTIVE
            }
        } ?: ActivityLevel.MODERATELY_ACTIVE
    )

    private fun UserProfile.toEntity() = UserProfileEntity(
        userId = userId,
        name = name,
        gender = gender?.name,
        dob = dateOfBirth,
        heightCm = heightCm,
        currentWeightKg = currentWeightKg,
        targetWeightKg = targetWeightKg,
        goalType = goalType.name,
        dailyCalorieTarget = dailyCalorieTarget,
        activityLevel = activityLevel.name
    )

}