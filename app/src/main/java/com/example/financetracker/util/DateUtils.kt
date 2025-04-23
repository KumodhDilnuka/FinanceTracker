package com.example.financetracker.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    
    fun formatDate(timestamp: Long, pattern: String = "MMM dd, yyyy"): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(date)
    }
    
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }
} 