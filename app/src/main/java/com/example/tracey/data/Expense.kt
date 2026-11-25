package com.example.tracey.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val title: String,
    val amount: Double,
    val type: String = "Expense",           // "Expense" or "Income"
    val walletName: String = "Main Wallet", // Multi-wallet support
    val category: String,
    val status: String,                     // "Paid" or "Pending"
    val dateMillis: Long,
    val notes: String = ""
)