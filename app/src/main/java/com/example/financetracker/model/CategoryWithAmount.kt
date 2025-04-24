package com.example.financetracker.model

data class CategoryWithAmount(
    val name: String,
    val amount: Double,
    val emoji: String = ""
) 