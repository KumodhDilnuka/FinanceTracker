package com.example.financetracker.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility class to manage reminder preferences
 */
object ReminderPrefs {
    private const val PREFS_NAME = "reminder_preferences"
    private const val KEY_REMINDER_HOUR = "reminder_hour"
    private const val KEY_REMINDER_MINUTE = "reminder_minute"
    
    private const val DEFAULT_HOUR = 20 // 8 PM
    private const val DEFAULT_MINUTE = 0
    
    private lateinit var prefs: SharedPreferences
    
    /**
     * Initialize the preferences
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Save the reminder time
     */
    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
    }
    
    /**
     * Get the reminder hour (24-hour format)
     */
    fun getReminderHour(): Int {
        return prefs.getInt(KEY_REMINDER_HOUR, DEFAULT_HOUR)
    }
    
    /**
     * Get the reminder minute
     */
    fun getReminderMinute(): Int {
        return prefs.getInt(KEY_REMINDER_MINUTE, DEFAULT_MINUTE)
    }
    
    /**
     * Format the reminder time as a string (in 12-hour format with AM/PM)
     */
    fun getFormattedReminderTime(): String {
        val hour = getReminderHour()
        val minute = getReminderMinute()
        
        val hourIn12Format = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        
        val amPm = if (hour >= 12) "PM" else "AM"
        val minuteStr = if (minute < 10) "0$minute" else minute.toString()
        
        return "$hourIn12Format:$minuteStr $amPm"
    }
} 