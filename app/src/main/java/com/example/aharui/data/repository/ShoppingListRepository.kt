package com.example.aharui.data.repository

import com.example.aharui.data.local.dao.ShoppingListDao
import com.example.aharui.data.local.entity.ShoppingListEntity
import com.example.aharui.data.model.ShoppingItem
import com.example.aharui.data.model.ShoppingList
import com.example.aharui.util.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingListRepository @Inject constructor(
    private val shoppingListDao: ShoppingListDao
) {
    private val gson = Gson()

    fun getShoppingLists(userId: String): Flow<List<ShoppingList>> {
        return shoppingListDao.getLists(userId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun getShoppingListById(listId: Long): Flow<ShoppingList?> {
        return shoppingListDao.getListById(listId).map { it?.toModel() }
    }

    suspend fun createShoppingList(userId: String, title: String, items: List<ShoppingItem>): Result<Long> {
        return try {
            val entity = ShoppingListEntity(
                userId = userId,
                title = title,
                items = gson.toJson(items)
            )
            val id = shoppingListDao.insertOrUpdateList(entity)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to create shopping list")
        }
    }

    suspend fun updateShoppingList(list: ShoppingList, userId: String): Result<Unit> {
        return try {
            val entity = ShoppingListEntity(
                id = list.id,
                userId = userId,
                title = list.title,
                items = gson.toJson(list.items)
            )
            shoppingListDao.insertOrUpdateList(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update shopping list")
        }
    }

    suspend fun deleteShoppingList(list: ShoppingList, userId: String): Result<Unit> {
        return try {
            val entity = ShoppingListEntity(
                id = list.id,
                userId = userId,
                title = list.title,
                items = gson.toJson(list.items)
            )
            shoppingListDao.deleteList(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete shopping list")
        }
    }

    private fun ShoppingListEntity.toModel(): ShoppingList {
        val itemsList: List<ShoppingItem> = try {
            val type = object : TypeToken<List<ShoppingItem>>() {}.type
            gson.fromJson(items, type)
        } catch (e: Exception) {
            emptyList()
        }
        return ShoppingList(
            id = id,
            title = title,
            items = itemsList
        )
    }
}