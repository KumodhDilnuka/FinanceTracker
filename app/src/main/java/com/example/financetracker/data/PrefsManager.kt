package com.example.financetracker.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.financetracker.model.Transaction

/**
 * Legacy wrapper that delegates to the new util.PrefsManager class
 */
object PrefsManager {
    private const val PREFS_NAME = "finance_prefs"
    private const val KEY_CATEGORIES = "categories"
    private const val KEY_TRANSACTIONS = "transactions"
    private const val KEY_BUDGET = "budget"
    private const val KEY_CURRENCY = "currency"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_USE_INTERNAL_STORAGE = "use_internal_storage"
    private const val TAG = "PrefsManager"
    
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Clean up any test data
        removeTestData()
        
        com.example.financetracker.util.PrefsManager.init(context)
    }
    
    fun saveCategories(categories: List<Category>) {
        try {
            // Check if prefs is initialized
            if (!::prefs.isInitialized) {
                Log.e(TAG, "SharedPreferences not initialized - falling back to util implementation")
                // Just use the util implementation
                com.example.financetracker.util.PrefsManager.saveCategories(categories)
                return
            }
            
            val json = gson.toJson(categories)
            prefs.edit().putString(KEY_CATEGORIES, json).apply()
            
            // Forward to util implementation
            com.example.financetracker.util.PrefsManager.saveCategories(categories)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving categories: ${e.message}")
            // Fall back to util implementation
            com.example.financetracker.util.PrefsManager.saveCategories(categories)
        }
    }
    
    fun loadCategories(): List<Category> {
        // Use the util implementation to ensure consistency
        return com.example.financetracker.util.PrefsManager.loadCategories()
    }
    
    fun saveTransactions(transactions: List<Transaction>) {
        try {
            if (!::prefs.isInitialized) {
                Log.e(TAG, "SharedPreferences not initialized - falling back to util implementation")
                com.example.financetracker.util.PrefsManager.saveTransactions(transactions)
                return
            }
            
            val json = gson.toJson(transactions)
            prefs.edit().putString(KEY_TRANSACTIONS, json).apply()
            
            com.example.financetracker.util.PrefsManager.saveTransactions(transactions)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transactions: ${e.message}")
            com.example.financetracker.util.PrefsManager.saveTransactions(transactions)
        }
    }
    
    fun loadTransactions(): List<Transaction> {
        // Use the util implementation to ensure consistency
        return com.example.financetracker.util.PrefsManager.loadTransactions()
    }
    
    fun setBudget(budget: Double) {
        try {
            if (!::prefs.isInitialized) {
                Log.e(TAG, "SharedPreferences not initialized - falling back to util implementation")
                com.example.financetracker.util.PrefsManager.setBudget(budget)
                return
            }
            
            prefs.edit().putFloat(KEY_BUDGET, budget.toFloat()).apply()
            
            com.example.financetracker.util.PrefsManager.setBudget(budget)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting budget: ${e.message}")
            com.example.financetracker.util.PrefsManager.setBudget(budget)
        }
    }
    
    fun getBudget(): Double {
        // Use the util implementation to ensure consistency
        return com.example.financetracker.util.PrefsManager.getBudget()
    }
    
    fun setCurrency(currencyCode: String) {
        try {
            if (!::prefs.isInitialized) {
                Log.e(TAG, "SharedPreferences not initialized - falling back to util implementation")
                com.example.financetracker.util.PrefsManager.setCurrency(currencyCode)
                return
            }
            
            prefs.edit().putString(KEY_CURRENCY, currencyCode).apply()
            
            com.example.financetracker.util.PrefsManager.setCurrency(currencyCode)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting currency: ${e.message}")
            com.example.financetracker.util.PrefsManager.setCurrency(currencyCode)
        }
    }
    
    fun getCurrency(): String {
        // Use the util implementation to ensure consistency
        return com.example.financetracker.util.PrefsManager.getCurrency()
    }
    
    fun isOnboardingCompleted(): Boolean {
        try {
            if (!::prefs.isInitialized) {
                return com.example.financetracker.util.PrefsManager.isOnboardingCompleted()
            }
            
            // Check both the legacy and new implementation
            val legacyValue = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
            
            // If already completed in legacy system, ensure it's also set in new system
            if (legacyValue) {
                com.example.financetracker.util.PrefsManager.setOnboardingCompleted(true)
            }
            
            // Use the new implementation's value
            return com.example.financetracker.util.PrefsManager.isOnboardingCompleted()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking onboarding status: ${e.message}")
            return com.example.financetracker.util.PrefsManager.isOnboardingCompleted()
        }
    }
    
    fun setOnboardingCompleted(completed: Boolean) {
        try {
            if (!::prefs.isInitialized) {
                com.example.financetracker.util.PrefsManager.setOnboardingCompleted(completed)
                return
            }
            
            // Update both legacy and new implementations
            prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
            com.example.financetracker.util.PrefsManager.setOnboardingCompleted(completed)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting onboarding status: ${e.message}")
            com.example.financetracker.util.PrefsManager.setOnboardingCompleted(completed)
        }
    }
    
    /**
     * Removes any test data from the stored transactions
     */
    private fun removeTestData() {
        try {
            val transactions = loadTransactions()
            
            // Filter out test transactions (named "Test Expense")
            val filteredTransactions = transactions.filter { it.title != "Test Expense" }
            
            // If we removed any transactions, save the filtered list
            if (filteredTransactions.size < transactions.size) {
                Log.d(TAG, "Removed ${transactions.size - filteredTransactions.size} test transactions")
                saveTransactions(filteredTransactions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing test data: ${e.message}")
        }
    }
    
    private fun getDefaultCategories(): List<Category> {
        return listOf(
            Category("Salary", TxType.INCOME),
            Category("Gifts", TxType.INCOME),
            Category("Food", TxType.EXPENSE),
            Category("Transport", TxType.EXPENSE),
            Category("Entertainment", TxType.EXPENSE),
            Category("Housing", TxType.EXPENSE),
            Category("Utilities", TxType.EXPENSE),
            Category("Healthcare", TxType.EXPENSE)
        )
    }
    
    /**
     * Gets the preference for backup storage location
     * @return true if internal storage should be used, false for external
     */
    fun getUseInternalStorageForBackup(): Boolean {
        // Delegate to the util implementation
        return com.example.financetracker.util.PrefsManager.getUseInternalStorageForBackup()
    }
    
    /**
     * Sets the preference for backup storage location
     * @param useInternal true for internal storage, false for external
     */
    fun setUseInternalStorageForBackup(useInternal: Boolean) {
        try {
            if (!::prefs.isInitialized) {
                Log.e(TAG, "SharedPreferences not initialized - falling back to util implementation")
                com.example.financetracker.util.PrefsManager.setUseInternalStorageForBackup(useInternal)
                return
            }
            
            prefs.edit().putBoolean(KEY_USE_INTERNAL_STORAGE, useInternal).apply()
            
            // Also update in the util implementation
            com.example.financetracker.util.PrefsManager.setUseInternalStorageForBackup(useInternal)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting backup storage preference: ${e.message}")
            com.example.financetracker.util.PrefsManager.setUseInternalStorageForBackup(useInternal)
        }
    }
} 