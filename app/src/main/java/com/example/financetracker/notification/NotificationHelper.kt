package com.example.financetracker.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.financetracker.FinanceApplication
import com.example.financetracker.R
import com.example.financetracker.ui.MainActivity

object NotificationHelper {
    
    private const val NOTIFICATION_ID_BUDGET_APPROACH = 100
    private const val NOTIFICATION_ID_BUDGET_EXCEEDED = 101
    private const val TAG = "NotificationHelper"
    
    fun notifyApproachingBudget(context: Context, spentPercentage: Int) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val builder = NotificationCompat.Builder(context, FinanceApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Budget Alert")
            .setContentText("You've used $spentPercentage% of your monthly budget")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        try {
            if (checkNotificationPermission(context)) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BUDGET_APPROACH, builder.build())
                Log.d(TAG, "Budget approach notification sent")
            } else {
                Log.d(TAG, "Missing notification permission")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification: ${e.message}")
        }
    }
    
    fun notifyBudgetExceeded(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val builder = NotificationCompat.Builder(context, FinanceApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Budget Exceeded")
            .setContentText("You have exceeded your monthly budget!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        try {
            if (checkNotificationPermission(context)) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BUDGET_EXCEEDED, builder.build())
                Log.d(TAG, "Budget exceeded notification sent")
            } else {
                Log.d(TAG, "Missing notification permission")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification: ${e.message}")
        }
    }
    
    private fun checkNotificationPermission(context: Context): Boolean {
        // For Android 13 and above, check for POST_NOTIFICATIONS permission
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == 
                    PackageManager.PERMISSION_GRANTED
        } else {
            // For older Android versions, permission is granted at install time
            true
        }
    }
    
    fun checkBudgetStatus(context: Context, spent: Double, budget: Double) {
        if (budget <= 0) return
        
        val percentage = ((spent / budget) * 100).toInt()
        
        when {
            spent > budget -> notifyBudgetExceeded(context)
            percentage >= 90 -> notifyApproachingBudget(context, percentage)
        }
    }
} 