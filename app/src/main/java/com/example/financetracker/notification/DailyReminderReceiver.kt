package com.example.financetracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import com.example.financetracker.util.ReminderPrefs

class DailyReminderReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "DailyReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received alarm with action: ${intent.action}")
        
        // Initialize prefs to ensure they're available
        ReminderPrefs.init(context)
        
        // Log current time for debugging
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedTime = dateFormat.format(Date(currentTime))
        Log.d(TAG, "Processing alarm at $formattedTime")
        
        // Process the notification request
        try {
            Log.d(TAG, "Sending daily transaction reminder notification")
            NotificationHelper.sendDailyTransactionReminder(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error in receiver: ${e.message}", e)
            
            // Emergency fallback - try one more time with a different approach
            try {
                Log.d(TAG, "Trying emergency fallback notification")
                
                // Get the notification manager directly
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                
                // Create a basic notification
                val builder = android.app.Notification.Builder(context)
                    .setContentTitle("Transaction Reminder")
                    .setContentText("Remember to add your daily transactions")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                
                // Apply proper channel for Android O+
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    builder.setChannelId("finance_tracker_channel")
                }
                
                // Show notification
                notificationManager.notify(999, builder.build())
                Log.d(TAG, "Emergency notification displayed")
            } catch (e2: Exception) {
                Log.e(TAG, "Emergency notification failed: ${e2.message}", e2)
            }
        }
        
        // Schedule the next alarm to maintain the daily reminder
        try {
            val hour = ReminderPrefs.getReminderHour()
            val minute = ReminderPrefs.getReminderMinute()
            Log.d(TAG, "Rescheduling next reminder for $hour:$minute")
            com.example.financetracker.notification.ReminderScheduler
                .scheduleDailyReminder(context, hour, minute)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule next notification: ${e.message}", e)
        }
    }
} 