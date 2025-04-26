package com.example.financetracker

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
import com.example.financetracker.util.ReminderPrefs

class FinanceTrackerApp : Application() {
    
    companion object {
        private const val TAG = "FinanceTrackerApp"
        const val CHANNEL_ID = "finance_tracker_channel"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize data.PrefsManager first
        DataPrefsManager.init(this)
        
        // Then initialize util.PrefsManager
        UtilPrefsManager.init(this)
        
        // Initialize reminder preferences
        ReminderPrefs.init(this)
        
        // Only set LKR as the default currency if no currency has been set at all
        val currentCurrency = UtilPrefsManager.getCurrency()
        if (currentCurrency.isEmpty()) {
            UtilPrefsManager.setCurrency("LKR")
        }
        
        // Create notification channel (must happen before scheduling notifications)
        createNotificationChannel()
        
        // Schedule daily transaction reminder
        scheduleReminders()
    }
    
    private fun createNotificationChannel() {
        // Create the notification channel on Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Reminders"
            val descriptionText = "Daily transaction reminder notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }
    
    private fun scheduleReminders() {
        try {
            // First, cancel any existing reminders to avoid duplicates
            ReminderScheduler.cancelDailyReminder(this)
            
            Log.d(TAG, "Scheduling daily transaction reminder")
            
            // Check permission for Android 12+ (API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "App doesn't have permission to schedule exact alarms")
                    // Schedule using inexact alarms instead - handled in ReminderScheduler
                }
            }
            
            // Get reminder time from preferences
            val reminderHour = ReminderPrefs.getReminderHour()
            val reminderMinute = ReminderPrefs.getReminderMinute()
            
            Log.d(TAG, "Setting reminder for ${reminderHour}:${reminderMinute}")
            
            // Schedule with user's preferred time
            ReminderScheduler.scheduleDailyReminder(this, reminderHour, reminderMinute)
        } catch (e: Exception) {
            // Log error but don't crash the app
            Log.e(TAG, "Error scheduling reminders: ${e.message}", e)
        }
    }
} 