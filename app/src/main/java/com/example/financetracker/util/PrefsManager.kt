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
    
    private fun getDefaultCategories(): List<Category> {
        return listOf(
            Category("Salary", com.example.financetracker.data.TxType.INCOME, "💰"),
            Category("Gifts", com.example.financetracker.data.TxType.INCOME, "🎁"),
            Category("Food", com.example.financetracker.data.TxType.EXPENSE, "🍔"),
            Category("Transport", com.example.financetracker.data.TxType.EXPENSE, "🚗"),
            Category("Entertainment", com.example.financetracker.data.TxType.EXPENSE, "🎬"),
            Category("Housing", com.example.financetracker.data.TxType.EXPENSE, "🏠"),
            Category("Utilities", com.example.financetracker.data.TxType.EXPENSE, "💡"),
            Category("Healthcare", com.example.financetracker.data.TxType.EXPENSE, "��")
        )
    }
} 