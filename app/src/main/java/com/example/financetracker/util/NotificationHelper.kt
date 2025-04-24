package com.example.financetracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.financetracker.R

object NotificationHelper {
    
    private const val CHANNEL_ID = "finance_tracker_channel"
    private const val BUDGET_NOTIFICATION_ID = 1001
    
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Finance Tracker Notifications"
            val descriptionText = "Notifications for budget alerts"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun checkBudgetStatus(context: Context, currentExpenses: Double, budget: Double) {
        if (budget <= 0) return // No budget set
        
        val percentage = (currentExpenses / budget) * 100
        
        // Only show notification when budget is exceeded (100% or more)
        if (currentExpenses > budget) {
            showBudgetAlertNotification(context, percentage.toInt(), budget, currentExpenses)
        }
    }
    
    private fun showBudgetAlertNotification(context: Context, percentage: Int, budget: Double, currentExpense: Double) {
        val currency = PrefsManager.getCurrency()
        val formattedBudget = CurrencyFormatter.formatCurrency(budget, currency)
        val formattedExpense = CurrencyFormatter.formatCurrency(currentExpense, currency)
        
        val title = "Monthly Budget Alert"
        val message = "You've used $percentage% of your monthly budget ($formattedExpense of $formattedBudget)"
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(BUDGET_NOTIFICATION_ID, builder.build())
    }
} 