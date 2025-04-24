package com.example.financetracker.util

import com.example.financetracker.model.Transaction

object CurrencyConverter {
    
    // This is a simplified implementation
    // In a real app, you would use a currency API
    
    fun formatCurrency(amount: Double, currencyCode: String): String {
        // Delegate to CurrencyFormatter
        return CurrencyFormatter.formatCurrency(amount, currencyCode)
    }
    
    fun getAvailableCurrencies(): List<String> {
        return listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "LKR")
    }
    
    fun getConversionFactor(fromCurrency: String, toCurrency: String): Double {
        // This is a mock implementation
        // In a real app, you would use real exchange rates
        val mockRates = mapOf(
            "USD" to 1.0,
            "EUR" to 0.85,
            "GBP" to 0.75,
            "JPY" to 110.0,
            "CAD" to 1.25,
            "AUD" to 1.35,
            "LKR" to 320.0  // Approximate exchange rate for Sri Lankan Rupee
        )
        
        val fromRate = mockRates[fromCurrency] ?: 1.0
        val toRate = mockRates[toCurrency] ?: 1.0
        
        return toRate / fromRate
    }
    
    fun convertTransactions(transactions: List<Transaction>, factor: Double): List<Transaction> {
        return transactions.map { transaction ->
            transaction.copy(amount = transaction.amount * factor)
        }
    }
} 