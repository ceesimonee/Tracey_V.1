package com.example.tracey.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tool_items")
data class ToolItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val toolType: String,     // "Savings", "Debt", or "LendBorrow"
    val title: String,
    val subtitle: String = "", // e.g., "Lent to Alex" or "Borrowed from Sam"
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val isCompleted: Boolean = false
)