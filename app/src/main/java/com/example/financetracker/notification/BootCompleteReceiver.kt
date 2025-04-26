package com.example.financetracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.financetracker.util.ReminderPrefs

/**
 * BroadcastReceiver that listens for BOOT_COMPLETED action to reschedule notifications
 * after device reboot.
 */
class BootCompleteReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootCompleteReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, rescheduling daily reminders")
            try {
                // Initialize reminder preferences
                ReminderPrefs.init(context)
                
                // Get user's preferred reminder time
                val reminderHour = ReminderPrefs.getReminderHour()
                val reminderMinute = ReminderPrefs.getReminderMinute()
                
                // Reschedule the daily reminder notifications with user's preferred time
                ReminderScheduler.scheduleDailyReminder(context, reminderHour, reminderMinute)
            } catch (e: Exception) {
                // Log error but don't crash the app
                Log.e(TAG, "Error rescheduling reminders after boot: ${e.message}", e)
            }
        }
    }
} 