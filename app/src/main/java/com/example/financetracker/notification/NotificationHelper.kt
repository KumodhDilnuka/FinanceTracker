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
import com.example.financetracker.data.PrefsManager
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
        
        val currency = PrefsManager.getCurrency()
        val budget = PrefsManager.getBudget()
        val spent = (budget * spentPercentage / 100)
        val message = "You've used $spentPercentage% of your monthly budget " +
                      "(${formatAmount(spent, currency)} of ${formatAmount(budget, currency)})"
        
        val builder = NotificationCompat.Builder(context, FinanceApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Budget Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        try {
            if (checkNotificationPermission(context)) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BUDGET_APPROACH, builder.build())
                Log.d(TAG, "Budget approach notification sent: $message")
            } else {
                Log.d(TAG, "Missing notification permission")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification: ${e.message}", e)
        }
    }
    
    fun notifyBudgetExceeded(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val currency = PrefsManager.getCurrency()
        val budget = PrefsManager.getBudget()
        val spent = PrefsManager.loadTransactions()
            .filter { it.type == com.example.financetracker.data.TxType.EXPENSE }
            .sumOf { it.amount }
        
        val message = "You have exceeded your monthly budget by " +
                      "${formatAmount(spent - budget, currency)}! " +
                      "(${formatAmount(spent, currency)} of ${formatAmount(budget, currency)})"
        
        val builder = NotificationCompat.Builder(context, FinanceApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Budget Exceeded")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        try {
            if (checkNotificationPermission(context)) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BUDGET_EXCEEDED, builder.build())
                Log.d(TAG, "Budget exceeded notification sent: $message")
            } else {
                Log.d(TAG, "Missing notification permission")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification: ${e.message}", e)
        }
    }
    
    private fun formatAmount(amount: Double, currency: String): String {
        return when (currency) {
            "USD" -> "$${String.format("%.2f", amount)}"
            "LKR" -> "Rs. ${String.format("%.2f", amount)}"
            "EUR" -> "€${String.format("%.2f", amount)}"
            "GBP" -> "£${String.format("%.2f", amount)}"
            else -> "$currency ${String.format("%.2f", amount)}"
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
        if (budget <= 0) {
            Log.d(TAG, "Skipping budget check because no budget is set")
            return
        }
        
        Log.d(TAG, "Checking budget: spent=$spent, budget=$budget")
        val percentage = ((spent / budget) * 100).toInt()
        
        when {
            // If over budget, show the exceeded notification
            spent > budget -> {
                Log.d(TAG, "Budget EXCEEDED! ($percentage% spent)")
                notifyBudgetExceeded(context)
            }
            // If approaching budget (90% or more), show the approaching notification
            percentage >= 90 -> {
                Log.d(TAG, "Budget approaching limit ($percentage% spent)")
                notifyApproachingBudget(context, percentage)
            }
            else -> {
                Log.d(TAG, "Budget is fine ($percentage% spent)")
            }
        }
    }
} 