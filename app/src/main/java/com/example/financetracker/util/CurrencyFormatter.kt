package com.example.financetracker.util

import java.text.NumberFormat
import java.util.*

object CurrencyFormatter {
    
    fun formatCurrency(amount: Double, currencyCode: String): String {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        format.currency = Currency.getInstance(currencyCode)
        return format.format(amount)
    }
} 