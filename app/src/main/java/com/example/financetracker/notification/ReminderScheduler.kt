package com.example.financetracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"
    private const val REQUEST_CODE = 123

    /**
     * Schedule a daily reminder notification at the specified hour and minute
     */
    fun scheduleDailyReminder(context: Context, hour: Int = 20, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Create a reliable broadcast intent
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            // Add a unique action to ensure intent is delivered
            action = "com.example.financetracker.DAILY_REMINDER"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            REQUEST_CODE, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Set time to trigger at specified hour and minute
        val calendar = Calendar.getInstance().apply {
            // Clear seconds and milliseconds to ensure precise timing
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // Set the user's selected time
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            
            // If the time has already passed for today, set it for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
                Log.d(TAG, "Time already passed today (${formatTime(hour, minute)}), scheduling for tomorrow")
            } else {
                Log.d(TAG, "Time hasn't passed yet today, scheduling for today (${formatTime(hour, minute)})")
            }
        }

        // Log the scheduled time
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val scheduledTime = calendar.time
        Log.d(TAG, "Scheduling daily reminder for: ${dateFormat.format(scheduledTime)}")

        try {
            // For all Android versions >= 6.0 (Marshmallow, API 23+), use reliable scheduling
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    // Try to use exact alarms for best reliability
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                        // For Android 12+ with exact alarm permission
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "Using setExactAndAllowWhileIdle for Android 12+")
                    } else {
                        // For Android 6-11
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "Using setExactAndAllowWhileIdle for Android 6-11")
                    }
                } catch (se: SecurityException) {
                    // Fall back to inexact but more compatible method
                    Log.w(TAG, "Security exception with exact alarms, using setAndAllowWhileIdle instead: ${se.message}")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                // For older Android versions (< 6.0)
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Using setExact for older Android versions")
            }
            
            // Always set up a repeating backup alarm that will fire every day
            // This helps ensure we eventually get a notification even if the exact one fails
            setupRepeatingBackupAlarm(context, hour, minute)
            
            Log.d(TAG, "Alarm scheduled successfully for ${dateFormat.format(calendar.time)}")
        } catch (e: Exception) {
            // If all else fails, set up a simple repeating alarm that will fire daily
            Log.e(TAG, "Error scheduling exact alarm: ${e.message}", e)
            setupRepeatingBackupAlarm(context, hour, minute)
        }
    }
    
    /**
     * Set up a daily repeating alarm as backup
     */
    private fun setupRepeatingBackupAlarm(context: Context, hour: Int, minute: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // Create a backup intent with different request code
            val backupIntent = Intent(context, DailyReminderReceiver::class.java).apply {
                action = "com.example.financetracker.DAILY_REMINDER_BACKUP"
            }
            
            val backupPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE + 1,  // Different request code
                backupIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            // Calculate first trigger time
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            
            // Set up repeating alarm that will fire every 24 hours
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                backupPendingIntent
            )
            
            Log.d(TAG, "Backup repeating alarm scheduled to fire daily at ${formatTime(hour, minute)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set up backup repeating alarm: ${e.message}", e)
        }
    }
    
    /**
     * Format time in 12-hour format
     */
    private fun formatTime(hour: Int, minute: Int): String {
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val amPm = if (hour >= 12) "PM" else "AM"
        val minuteStr = if (minute < 10) "0$minute" else minute.toString()
        return "$hour12:$minuteStr $amPm"
    }
    
    /**
     * Cancel the daily reminder if needed
     */
    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        try {
            Log.d(TAG, "Cancelling existing daily reminders")
            
            // Cancel main alarm
            val intent = Intent(context, DailyReminderReceiver::class.java)
            intent.action = "com.example.financetracker.DAILY_REMINDER"
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                REQUEST_CODE, 
                intent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            
            // Cancel backup alarm
            val backupIntent = Intent(context, DailyReminderReceiver::class.java)
            backupIntent.action = "com.example.financetracker.DAILY_REMINDER_BACKUP"
            val backupPendingIntent = PendingIntent.getBroadcast(
                context, 
                REQUEST_CODE + 1, 
                backupIntent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.cancel(backupPendingIntent)
            backupPendingIntent.cancel()
            
            Log.d(TAG, "All daily reminders cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling alarms: ${e.message}", e)
        }
    }
} 