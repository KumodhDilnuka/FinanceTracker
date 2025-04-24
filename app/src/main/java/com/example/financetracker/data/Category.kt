package com.example.financetracker.data

data class Category(
    val name: String,
    val type: TxType,
    val emoji: String = "" // Default to empty string
)
