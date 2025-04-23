package com.example.financetracker.model

import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String,
    val type: TxType,
    val date: Long,
    val note: String = ""
) 