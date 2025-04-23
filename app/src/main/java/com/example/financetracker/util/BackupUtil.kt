package com.example.financetracker.util

import android.content.Context
import com.example.financetracker.data.Category
import com.example.financetracker.data.PrefsManager
import com.example.financetracker.model.Transaction
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

object BackupUtil {

    private const val BACKUP_FOLDER = "backups"
    
    data class BackupData(
        val categories: List<Category>,
        val transactions: List<Transaction>,
        val currency: String,
        val budget: Double,
        val backupDate: Long = System.currentTimeMillis()
    )
    
    /**
     * Creates a backup file in the app's internal storage
     */
    fun createBackup(context: Context): File {
        val transactions = PrefsManager.loadTransactions()
        val budget = PrefsManager.getBudget()
        val currency = PrefsManager.getCurrency()
        
        val backupData = mapOf(
            "transactions" to transactions,
            "budget" to budget,
            "currency" to currency
        )
        
        val json = Gson().toJson(backupData)
        
        // Create backup folder if it doesn't exist
        val backupFolder = File(context.filesDir, BACKUP_FOLDER)
        if (!backupFolder.exists()) {
            backupFolder.mkdirs()
        }
        
        // Create a backup file with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        val backupFile = File(backupFolder, "backup_$timestamp.json")
        
        // Write data to the file
        backupFile.writeText(json)
        
        return backupFile
    }
    
    /**
     * Restores data from the provided backup file
     */
    fun restoreFromFile(context: Context, file: File): Boolean {
        try {
            val json = file.readText()
            val gson = Gson()
            
            // Parse the backup data
            val mapType: Type = object : TypeToken<Map<String, Any>>() {}.type
            val backupData: Map<String, Any> = gson.fromJson(json, mapType)
            
            // Extract transactions
            val transactionsJson = gson.toJson(backupData["transactions"])
            val transactionsType: Type = object : TypeToken<List<Transaction>>() {}.type
            val transactions: List<Transaction> = gson.fromJson(transactionsJson, transactionsType)
            
            // Extract budget and currency
            val budget = (backupData["budget"] as? Double) ?: 0.0
            val currency = backupData["currency"] as? String ?: "USD"
            
            // Save restored data
            PrefsManager.saveTransactions(transactions)
            PrefsManager.setBudget(budget)
            PrefsManager.setCurrency(currency)
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Gets backup file info if it exists
     */
    fun getBackupInfo(context: Context): String? {
        val backupFolder = File(context.filesDir, BACKUP_FOLDER)
        if (!backupFolder.exists() || backupFolder.listFiles()?.isEmpty() == true) {
            return null
        }
        
        // Get the most recent backup
        val backupFiles = backupFolder.listFiles()?.sortedByDescending { it.lastModified() }
        val mostRecentBackup = backupFiles?.firstOrNull() ?: return null
        
        // Format the last modified date
        val lastModified = Date(mostRecentBackup.lastModified())
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(lastModified)
        
        return "Last backup: $formattedDate"
    }
} 