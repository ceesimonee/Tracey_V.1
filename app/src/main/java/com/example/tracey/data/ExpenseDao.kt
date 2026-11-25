package com.example.tracey.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE username = :username ORDER BY dateMillis DESC")
    fun getExpensesByUser(username: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // Money Tools CRUD
    @Query("SELECT * FROM tool_items WHERE username = :username ORDER BY id DESC")
    fun getToolItemsByUser(username: String): Flow<List<ToolItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToolItem(item: ToolItem)

    @Update
    suspend fun updateToolItem(item: ToolItem)

    @Delete
    suspend fun deleteToolItem(item: ToolItem)

    // Chat History Persistence
    @Query("SELECT * FROM chat_messages WHERE username = :username ORDER BY id ASC")
    fun getChatMessagesByUser(username: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE username = :username")
    suspend fun clearChatMessagesForUser(username: String)
}