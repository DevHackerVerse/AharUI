package com.example.aharui.data.repository

import com.example.aharui.data.local.dao.RewardDao
import com.example.aharui.data.local.entity.RewardEntity
import com.example.aharui.data.model.Badge
import com.example.aharui.data.model.RewardData
import com.example.aharui.util.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardRepository @Inject constructor(
    private val rewardDao: RewardDao
) {
    private val gson = Gson()

    fun getRewards(userId: String): Flow<RewardData?> {
        return rewardDao.getRewards(userId).map { entity ->
            entity?.let {
                RewardData(
                    pointsTotal = it.pointsTotal,
                    streakDays = it.streakDays,
                    badges = deserializeBadges(it.badges),
                    lastLoggedDate = null
                )
            }
        }
    }

    suspend fun addPoints(userId: String, points: Int): Result<Unit> {
        return try {
            rewardDao.addPoints(userId, points)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to add points")
        }
    }

    suspend fun updateStreak(userId: String, streak: Int): Result<Unit> {
        return try {
            rewardDao.updateStreak(userId, streak)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update streak")
        }
    }

    suspend fun updateBadges(userId: String, badges: List<Badge>): Result<Unit> {
        return try {
            val entity = rewardDao.getRewards(userId).first()
            if (entity != null) {
                val updatedEntity = entity.copy(badges = serializeBadges(badges))
                rewardDao.insertOrUpdateRewards(updatedEntity)
                Result.Success(Unit)
            } else {
                Result.Error("Rewards not found for user")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update badges")
        }
    }

    suspend fun unlockBadge(userId: String, badgeId: String): Result<Unit> {
        return try {
            val rewardData = getRewards(userId).first()
            if (rewardData != null) {
                val updatedBadges = rewardData.badges.map { badge ->
                    if (badge.id == badgeId && !badge.isUnlocked) {
                        badge.copy(
                            isUnlocked = true,
                            unlockedAt = System.currentTimeMillis()
                        )
                    } else {
                        badge
                    }
                }
                updateBadges(userId, updatedBadges)
            } else {
                Result.Error("Rewards not found")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to unlock badge")
        }
    }

    suspend fun initializeRewards(userId: String): Result<Unit> {
        return try {
            val initialBadges = getInitialBadges()
            val entity = RewardEntity(
                userId = userId,
                pointsTotal = 0,
                streakDays = 0,
                badges = serializeBadges(initialBadges)
            )
            rewardDao.insertOrUpdateRewards(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to initialize rewards")
        }
    }

    private fun getInitialBadges(): List<Badge> {
        return listOf(
            Badge(
                id = "first_meal",
                name = "First Steps",
                description = "Log your first meal",
                iconRes = 0,
                isUnlocked = false,
                unlockedAt = null
            ),
            Badge(
                id = "hydration_hero",
                name = "Hydration Hero",
                description = "Meet your water goal for 3 consecutive days",
                iconRes = 0,
                isUnlocked = false,
                unlockedAt = null
            ),
            Badge(
                id = "perfect_week",
                name = "Perfect Week",
                description = "Log a meal every day for a full week",
                iconRes = 0,
                isUnlocked = false,
                unlockedAt = null
            ),
            Badge(
                id = "scanner_pro",
                name = "Scanner Pro",
                description = "Scan 10 different food items",
                iconRes = 0,
                isUnlocked = false,
                unlockedAt = null
            ),
            Badge(
                id = "weight_warrior",
                name = "Weight Warrior",
                description = "Log your weight for 7 consecutive days",
                iconRes = 0,
                isUnlocked = false,
                unlockedAt = null
            ),
            Badge(
                id = "calorie_master",
                name = "Calorie Master",
                description = "Stay within your calorie target for 5 days",
                iconRes = 0,
                isUnlocked = false,
                unlockedAt = null
            ),
            Badge(
                id = "streak_rookie",
                name = "Streak Rookie",
                description = "Maintain a 3-day streak",
                iconRes = 0,
                isUnlocked = false,
                unlockedAt = null
            ),
            Badge(
                id = "streak_master",
                name = "Streak Master",
                description = "Maintain a 30-day streak",
                iconRes = 0,
                isUnlocked = false,
                unlockedAt = null
            ),
            Badge(
                id = "water_champion",
                name = "Water Champion",
                description = "Log 10 liters of water total",
                iconRes = 0,
                isUnlocked = false,
                unlockedAt = null
            ),
            Badge(
                id = "nutrition_expert",
                name = "Nutrition Expert",
                description = "Log 50 meals",
                iconRes = 0,
                isUnlocked = false,
                unlockedAt = null
            )
        )
    }

    private fun serializeBadges(badges: List<Badge>): String {
        return gson.toJson(badges)
    }

    private fun deserializeBadges(json: String): List<Badge> {
        return try {
            val type = object : TypeToken<List<Badge>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}