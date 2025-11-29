package com.example.aharui.data.local.dao

import androidx.room.*
import com.example.aharui.data.local.entity.ShoppingListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateList(list: ShoppingListEntity): Long

    @Query("SELECT * FROM shopping_lists WHERE userId = :userId ORDER BY id DESC")
    fun getLists(userId: String): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE id = :listId")
    fun getListById(listId: Long): Flow<ShoppingListEntity?>

    @Delete
    suspend fun deleteList(list: ShoppingListEntity)

    @Query("DELETE FROM shopping_lists WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}