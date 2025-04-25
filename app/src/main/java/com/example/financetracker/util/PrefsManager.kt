package com.example.financetracker.util

import android.content.Context
import android.content.SharedPreferences
import com.example.financetracker.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.financetracker.data.Category

object PrefsManager {
    
    private lateinit var prefs: SharedPreferences
    private const val PREFS_NAME = "finance_tracker_prefs"
    private const val KEY_TRANSACTIONS = "transactions"
    private const val KEY_BUDGET = "budget"
    private const val KEY_CURRENCY = "currency"
    private const val KEY_CATEGORIES = "categories"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_USE_INTERNAL_STORAGE = "use_internal_storage"
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveTransactions(transactions: List<Transaction>) {
        val gson = Gson()
        val json = gson.toJson(transactions)
        prefs.edit().putString(KEY_TRANSACTIONS, json).apply()
    }
    
    fun loadTransactions(): List<Transaction> {
        val gson = Gson()
        val json = prefs.getString(KEY_TRANSACTIONS, "")
        
        return if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            val type = object : TypeToken<List<Transaction>>() {}.type
            gson.fromJson(json, type)
        }
    }
    
    fun setBudget(budget: Double) {
        prefs.edit().putFloat(KEY_BUDGET, budget.toFloat()).apply()
    }
    
    fun getBudget(): Double {
        return prefs.getFloat(KEY_BUDGET, 0f).toDouble()
    }
    
    fun setCurrency(currencyCode: String) {
        prefs.edit().putString(KEY_CURRENCY, currencyCode).apply()
    }
    
    fun getCurrency(): String {
        return prefs.getString(KEY_CURRENCY, "") ?: ""
    }
    
    // Theme mode methods
    fun setThemeMode(isDarkMode: Boolean) {
        prefs.edit().putBoolean(KEY_THEME_MODE, isDarkMode).apply()
    }
    
    fun getThemeMode(): Boolean {
        return prefs.getBoolean(KEY_THEME_MODE, false)
    }
    
    // Category methods to support data.PrefsManager
    fun saveCategories(categories: List<Category>) {
        val gson = Gson()
        val json = gson.toJson(categories)
        prefs.edit().putString(KEY_CATEGORIES, json).apply()
    }
    
    fun loadCategories(): List<Category> {
        val gson = Gson()
        val json = prefs.getString(KEY_CATEGORIES, null)
        
        if (json.isNullOrEmpty()) {
            return getDefaultCategories()
        }
        
        val type = object : TypeToken<List<Category>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }
    
    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }
    
    private fun getDefaultCategories(): List<Category> {
        return listOf(
            Category("Salary", com.example.financetracker.data.TxType.INCOME, "\uD83D\uDCB8"),
            Category("Gifts", com.example.financetracker.data.TxType.INCOME, "🎁"),
            Category("Food", com.example.financetracker.data.TxType.EXPENSE, "🍔"),
            Category("Transport", com.example.financetracker.data.TxType.EXPENSE, "🚗"),
            Category("Entertainment", com.example.financetracker.data.TxType.EXPENSE, "🎬"),
            Category("Housing", com.example.financetracker.data.TxType.EXPENSE, "🏠"),
            Category("Utilities", com.example.financetracker.data.TxType.EXPENSE, "💡"),
            Category("Healthcare", com.example.financetracker.data.TxType.EXPENSE, "\uD83C\uDFE5")
        )
    }
    
    /**
     * Gets the preference for backup storage location
     * @return true if internal storage should be used, false for external
     */
    fun getUseInternalStorageForBackup(): Boolean {
        return prefs.getBoolean(KEY_USE_INTERNAL_STORAGE, false) // Default to external storage
    }
    
    /**
     * Sets the preference for backup storage location
     * @param useInternal true for internal storage, false for external
     */
    fun setUseInternalStorageForBackup(useInternal: Boolean) {
        prefs.edit().putBoolean(KEY_USE_INTERNAL_STORAGE, useInternal).apply()
    }
} 