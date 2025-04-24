package com.example.financetracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.financetracker.data.PrefsManager
import com.example.financetracker.data.TxType
import com.example.financetracker.notification.NotificationHelper
import com.example.financetracker.util.PrefsManager as UtilPrefsManager

class FinanceApplication : Application() {
    
    companion object {
        const val CHANNEL_ID = "finance_tracker_channel"
        private const val TAG = "FinanceApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize PrefsManager
        PrefsManager.init(this)
        UtilPrefsManager.init(this)
        
        // Apply theme mode
        applyThemeMode()
        
        // Create notification channel
        createNotificationChannel()
        
        // Check budget after app startup
        Handler(Looper.getMainLooper()).postDelayed({
            checkBudgetStatus()
        }, 3000) // 3 seconds delay
    }
    
    private fun applyThemeMode() {
        val isDarkMode = UtilPrefsManager.getThemeMode()
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    
    private fun checkBudgetStatus() {
        Log.d(TAG, "Checking budget status...")
        
        // Get current budget and expenses
        val budget = PrefsManager.getBudget()
        if (budget <= 0) {
            Log.d(TAG, "No budget set, skipping notification check")
            return
        }
        
        // Calculate current month expenses only
        val transactions = PrefsManager.loadTransactions()
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        
        val monthlyExpenses = transactions
            .filter { transaction -> 
                val transactionCal = java.util.Calendar.getInstance().apply { timeInMillis = transaction.date }
                transaction.type == TxType.EXPENSE &&
                transactionCal.get(java.util.Calendar.MONTH) == currentMonth &&
                transactionCal.get(java.util.Calendar.YEAR) == currentYear
            }
            .sumOf { it.amount }
        
        // Check budget status for notification
        Log.d(TAG, "Checking notification with monthly expenses: $monthlyExpenses, budget: $budget (${monthlyExpenses/budget*100}%)")
        NotificationHelper.checkBudgetStatus(this, monthlyExpenses, budget)
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