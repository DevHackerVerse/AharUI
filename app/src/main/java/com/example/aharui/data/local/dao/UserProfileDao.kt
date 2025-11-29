package com.example.aharui.data.local.dao

import androidx.room.*
import com.example.aharui.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    fun getProfile(userId: String): Flow<UserProfileEntity?>

    // ADD THIS for synchronous access
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getProfileSync(userId: String): UserProfileEntity?

    @Query("UPDATE user_profiles SET currentWeightKg = :weight WHERE userId = :userId")
    suspend fun updateWeight(userId: String, weight: Double)

    @Query("UPDATE user_profiles SET dailyCalorieTarget = :target WHERE userId = :userId")
    suspend fun updateCalorieTarget(userId: String, target: Int)

    @Query("DELETE FROM user_profiles WHERE userId = :userId")
    suspend fun deleteProfile(userId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM user_profiles WHERE userId = :userId)")
    suspend fun profileExists(userId: String): Boolean
}