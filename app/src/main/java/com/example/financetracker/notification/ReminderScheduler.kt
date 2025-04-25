package com.example.financetracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"
    private const val REQUEST_CODE = 123

    /**
     * Schedule a daily reminder notification at the specified hour and minute
     */
    fun scheduleDailyReminder(context: Context, hour: Int = 20, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            REQUEST_CODE, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // FOR TESTING: Set time to trigger 1 minute from now
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 1) // Trigger in 1 minute for testing
        }

        /*
        // Normal scheduling code - comment out for testing
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // If the time has already passed for today, set it for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        */

        // Log the scheduled time
        val scheduledTime = calendar.time
        Log.d(TAG, "Scheduling daily reminder for: $scheduledTime")

        try {
            // Try to set exact alarm first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12+ (API 31+), check if we have permission for exact alarms
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Exact alarm scheduled successfully")
                } else {
                    // Fall back to inexact alarm
                    Log.d(TAG, "No permission for exact alarms, using inexact alarm instead")
                    scheduleInexactAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // For Android 6.0 to 11
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Exact alarm scheduled successfully")
            } else {
                // For older Android versions
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Exact alarm scheduled successfully")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when scheduling exact alarm: ${e.message}")
            // Fall back to inexact alarm if security exception occurs
            scheduleInexactAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}")
            // Try inexact alarm as a last resort
            try {
                scheduleInexactAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to schedule even an inexact alarm: ${e2.message}")
            }
        }
    }
    
    /**
     * Schedule an inexact alarm as fallback
     */
    private fun scheduleInexactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        // Set a repeating alarm - less precise but more battery-friendly
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            AlarmManager.INTERVAL_DAY, // Repeat daily
            pendingIntent
        )
        Log.d(TAG, "Inexact daily repeating alarm scheduled as fallback")
    }
    
    /**
     * Cancel the daily reminder if needed
     */
    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            REQUEST_CODE, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Daily reminder cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling alarm: ${e.message}")
        }
    }
} 