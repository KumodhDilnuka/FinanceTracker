package com.example.financetracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

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
                // Reschedule the daily reminder notifications
                // ReminderScheduler now handles permission errors internally
                ReminderScheduler.scheduleDailyReminder(context)
            } catch (e: Exception) {
                // Log error but don't crash the app
                Log.e(TAG, "Error rescheduling reminders after boot: ${e.message}", e)
            }
        }
    }
} 