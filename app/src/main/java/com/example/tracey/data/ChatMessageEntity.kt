package com.example.tracey.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val loggedTitle: String? = null,
    val loggedAmount: Double? = null,
    val loggedType: String? = null,
    val loggedCategory: String? = null,
    val loggedWallet: String? = null
)