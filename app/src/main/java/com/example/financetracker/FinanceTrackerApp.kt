package com.example.financetracker

import android.app.Application
import com.example.financetracker.data.PrefsManager as DataPrefsManager
import com.example.financetracker.util.PrefsManager as UtilPrefsManager
import com.example.financetracker.util.NotificationHelper

class FinanceTrackerApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize data.PrefsManager first
        DataPrefsManager.init(this)
        
        // Then initialize util.PrefsManager (should be done automatically by data.PrefsManager,
        // but we're calling it explicitly to be safe)
        UtilPrefsManager.init(this)
        
        // Create notification channel for budget alerts
        NotificationHelper.createNotificationChannel(this)
    }
} 