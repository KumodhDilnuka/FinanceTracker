package com.example.financetracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.financetracker.data.PrefsManager
import com.example.financetracker.data.TxType
import com.example.financetracker.notification.NotificationHelper

class FinanceApplication : Application() {
    
    companion object {
        const val CHANNEL_ID = "finance_tracker_channel"
        private const val TAG = "FinanceApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize PrefsManager
        PrefsManager.init(this)
        
        // Create notification channel
        createNotificationChannel()
        
        // Check budget after app startup
        Handler(Looper.getMainLooper()).postDelayed({
            checkBudgetStatus()
        }, 3000) // 3 seconds delay
    }
    
    private fun checkBudgetStatus() {
        Log.d(TAG, "Checking budget status...")
        
        // Get current budget and expenses
        val budget = PrefsManager.getBudget()
        if (budget <= 0) {
            Log.d(TAG, "No budget set, skipping notification check")
            return
        }
        
        val transactions = PrefsManager.loadTransactions()
        val totalExpenses = transactions
            .filter { it.type == TxType.EXPENSE }
            .sumOf { it.amount }
        
        // Check budget status for notification
        Log.d(TAG, "Checking notification with expenses: $totalExpenses, budget: $budget")
        NotificationHelper.checkBudgetStatus(this, totalExpenses, budget)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Finance Tracker"
            val descriptionText = "Budget alerts and notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
} 