package com.example.financetracker

import android.app.AlarmManager
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.example.financetracker.data.PrefsManager as DataPrefsManager
import com.example.financetracker.util.PrefsManager as UtilPrefsManager
import com.example.financetracker.util.NotificationHelper
import com.example.financetracker.notification.ReminderScheduler
import com.example.financetracker.notification.NotificationHelper as AppNotificationHelper

class FinanceTrackerApp : Application() {
    
    companion object {
        private const val TAG = "FinanceTrackerApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize data.PrefsManager first
        DataPrefsManager.init(this)
        
        // Then initialize util.PrefsManager (should be done automatically by data.PrefsManager,
        // but we're calling it explicitly to be safe)
        UtilPrefsManager.init(this)
        
        // Only set LKR as the default currency if no currency has been set at all
        val currentCurrency = UtilPrefsManager.getCurrency()
        if (currentCurrency.isEmpty()) {
            UtilPrefsManager.setCurrency("LKR")
        }
        
        // Create notification channel for budget alerts
        NotificationHelper.createNotificationChannel(this)
        
        // Send a test notification immediately to check if notifications work
        try {
            Log.d(TAG, "Sending immediate test notification")
            AppNotificationHelper.sendDailyTransactionReminder(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending test notification: ${e.message}", e)
        }
        
        // Schedule daily transaction reminder (defaults to 8:00 PM)
        scheduleReminders()
    }
    
    private fun scheduleReminders() {
        try {
            Log.d(TAG, "Scheduling daily transaction reminder")
            
            // Check permission for Android 12+ (API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "App doesn't have permission to schedule exact alarms")
                    // Schedule using inexact alarms instead - handled in ReminderScheduler
                }
            }
            
            // This will now handle permission issues internally
            ReminderScheduler.scheduleDailyReminder(this)
        } catch (e: Exception) {
            // Log error but don't crash the app
            Log.e(TAG, "Error scheduling reminders: ${e.message}", e)
        }
    }
} 