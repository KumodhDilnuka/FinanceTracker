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
    
    /**
     * Checks if the given timestamp is within the current day
     */
    fun isCurrentDay(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)
        
        calendar.timeInMillis = timestamp
        val transactionDay = calendar.get(Calendar.DAY_OF_YEAR)
        val transactionYear = calendar.get(Calendar.YEAR)
        
        return currentDay == transactionDay && currentYear == transactionYear
    }
    
    /**
     * Checks if the given timestamp is within the current week
     */
    fun isCurrentWeek(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)
        
        calendar.timeInMillis = timestamp
        val transactionWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val transactionYear = calendar.get(Calendar.YEAR)
        
        return currentWeek == transactionWeek && currentYear == transactionYear
    }
    
    /**
     * Checks if the given timestamp is within the current month
     */
    fun isCurrentMonth(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        calendar.timeInMillis = timestamp
        val transactionMonth = calendar.get(Calendar.MONTH)
        val transactionYear = calendar.get(Calendar.YEAR)
        
        return currentMonth == transactionMonth && currentYear == transactionYear
    }
    
    /**
     * Checks if the given timestamp is within the current year
     */
    fun isCurrentYear(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        
        calendar.timeInMillis = timestamp
        val transactionYear = calendar.get(Calendar.YEAR)
        
        return currentYear == transactionYear
    }
    
    /**
     * Gets the start timestamp of the current month
     */
    fun getCurrentMonthStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Gets the end timestamp of the current month
     */
    fun getCurrentMonthEnd(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
} 