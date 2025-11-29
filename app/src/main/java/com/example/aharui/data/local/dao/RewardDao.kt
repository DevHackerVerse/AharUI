package com.example.aharui.data.local.dao

import androidx.room.*
import com.example.aharui.data.local.entity.RewardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRewards(rewards: RewardEntity)

    @Query("SELECT * FROM rewards WHERE userId = :userId")
    fun getRewards(userId: String): Flow<RewardEntity?>

    @Query("UPDATE rewards SET pointsTotal = pointsTotal + :points WHERE userId = :userId")
    suspend fun addPoints(userId: String, points: Int)

    @Query("UPDATE rewards SET streakDays = :streak WHERE userId = :userId")
    suspend fun updateStreak(userId: String, streak: Int)

    @Query("DELETE FROM rewards WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}