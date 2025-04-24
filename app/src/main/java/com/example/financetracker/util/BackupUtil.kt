package com.example.financetracker.util

import android.content.Context
import android.os.Environment
import com.example.financetracker.data.Category
import com.example.financetracker.data.PrefsManager
import com.example.financetracker.model.Transaction
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

object BackupUtil {

    const val BACKUP_FOLDER = ""  // Save directly to Downloads
    const val INTERNAL_BACKUP_FOLDER = "backups"  // Folder name for internal storage
    
    enum class BackupStorageType {
        INTERNAL,  // App's internal storage
        EXTERNAL   // Downloads folder
    }
    
    data class BackupData(
        val categories: List<Category>,
        val transactions: List<Transaction>,
        val currency: String,
        val budget: Double,
        val backupDate: Long = System.currentTimeMillis()
    )
    
    /**
     * Creates a backup file in the selected storage type (Downloads or Internal)
     */
    fun createBackup(context: Context, storageType: BackupStorageType = BackupStorageType.EXTERNAL): File {
        return if (storageType == BackupStorageType.INTERNAL) {
            createInternalBackup(context)
        } else {
            createExternalBackup(context)
        }
    }
    
    /**
     * Creates a backup file in the app's internal storage
     */
    private fun createInternalBackup(context: Context): File {
        try {
            val transactions = PrefsManager.loadTransactions()
            val categories = PrefsManager.loadCategories()
            val budget = PrefsManager.getBudget()
            val currency = PrefsManager.getCurrency()
            
            val backupData = mapOf(
                "transactions" to transactions,
                "categories" to categories,
                "budget" to budget,
                "currency" to currency
            )
            
            val json = Gson().toJson(backupData)
            
            // Create the internal backups directory if it doesn't exist
            val backupFolder = File(context.filesDir, INTERNAL_BACKUP_FOLDER)
            if (!backupFolder.exists()) {
                val created = backupFolder.mkdirs()
                if (!created) {
                    throw IOException("Failed to create directory: ${backupFolder.absolutePath}")
                }
            }
            
            // Create a backup file with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val backupFileName = "FinanceTracker_backup_$timestamp.json"
            val backupFile = File(backupFolder, backupFileName)
            
            // Write data to the file using FileOutputStream
            FileOutputStream(backupFile).use { stream ->
                stream.write(json.toByteArray())
            }
            
            // Log success
            android.util.Log.d("BackupUtil", "Internal backup created successfully at: ${backupFile.absolutePath}")
            
            return backupFile
        } catch (e: Exception) {
            android.util.Log.e("BackupUtil", "Internal backup creation failed: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Creates a backup file in the Downloads folder (External storage)
     */
    private fun createExternalBackup(context: Context): File {
        try {
            val transactions = PrefsManager.loadTransactions()
            val categories = PrefsManager.loadCategories()
            val budget = PrefsManager.getBudget()
            val currency = PrefsManager.getCurrency()
            
            val backupData = mapOf(
                "transactions" to transactions,
                "categories" to categories,
                "budget" to budget,
                "currency" to currency
            )
            
            val json = Gson().toJson(backupData)
            
            // Create a backup folder in Downloads if it doesn't exist
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val backupFolder = File(downloadsDir, BACKUP_FOLDER)
            if (!backupFolder.exists()) {
                val created = backupFolder.mkdirs()
                if (!created) {
                    throw IOException("Failed to create directory: ${backupFolder.absolutePath}")
                }
            }
            
            // Create a backup file with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val backupFileName = "FinanceTracker_backup_$timestamp.json"
            val backupFile = File(backupFolder, backupFileName)
            
            // Write data to the file using Java's FileWriter for better compatibility
            try {
                val writer = java.io.FileWriter(backupFile)
                writer.write(json)
                writer.flush()
                writer.close()
                
                // Verify the file was created
                if (!backupFile.exists() || backupFile.length() == 0L) {
                    throw IOException("File was not created or is empty")
                }
                
                // Log success
                android.util.Log.d("BackupUtil", "External backup created successfully at: ${backupFile.absolutePath}")
                
                return backupFile
            } catch (e: Exception) {
                android.util.Log.e("BackupUtil", "Error writing to file: ${e.message}", e)
                throw e
            }
        } catch (e: Exception) {
            android.util.Log.e("BackupUtil", "External backup creation failed: ${e.message}", e)
            throw e
        }
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
            
            // Extract categories if available
            val categoriesJson = gson.toJson(backupData["categories"])
            val categoriesType: Type = object : TypeToken<List<Category>>() {}.type
            val categories: List<Category> = try {
                gson.fromJson(categoriesJson, categoriesType)
            } catch (e: Exception) {
                // If categories not found, use existing ones
                PrefsManager.loadCategories()
            }
            
            // Extract budget and currency
            val budget = (backupData["budget"] as? Double) ?: 0.0
            val currency = backupData["currency"] as? String ?: "USD"
            
            // Save restored data
            PrefsManager.saveTransactions(transactions)
            PrefsManager.saveCategories(categories)
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
        // Check the Downloads folder first
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val downloadsBackupFolder = File(downloadsDir, BACKUP_FOLDER)
        
        // Also check internal storage 
        val internalBackupFolder = File(context.filesDir, INTERNAL_BACKUP_FOLDER)
        
        // Check both folders
        val folders = listOf(downloadsBackupFolder, internalBackupFolder)
        
        // Get all backup files from both locations
        val allBackupFiles = mutableListOf<File>()
        
        for (folder in folders) {
            if (folder.exists()) {
                folder.listFiles()?.let { files ->
                    allBackupFiles.addAll(files.filter { 
                        (it.name.startsWith("FinanceTracker_backup_") || it.name.startsWith("backup_")) && 
                        it.name.endsWith(".json") 
                    })
                }
            }
        }
        
        if (allBackupFiles.isEmpty()) {
            return null
        }
        
        // Get the most recent backup
        val mostRecentBackup = allBackupFiles.maxByOrNull { it.lastModified() } ?: return null
        
        // Format the last modified date
        val lastModified = Date(mostRecentBackup.lastModified())
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(lastModified)
        
        // Indicate storage location
        val storageType = if (mostRecentBackup.absolutePath.contains(context.filesDir.absolutePath)) {
            "Internal Storage"
        } else {
            "External Storage"
        }
        
        return "Last backup: $formattedDate ($storageType)"
    }
    
    /**
     * Alternative method to create a backup file using MediaStore API (for Android 10+)
     * This is used as a fallback when direct file access fails
     */
    fun createBackupUsingMediaStore(context: Context): File {
        try {
            val transactions = PrefsManager.loadTransactions()
            val budget = PrefsManager.getBudget()
            val currency = PrefsManager.getCurrency()
            
            val backupData = mapOf(
                "transactions" to transactions,
                "budget" to budget,
                "currency" to currency
            )
            
            val json = Gson().toJson(backupData)
            
            // Create a temporary file first
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val backupFileName = "FinanceTracker_backup_$timestamp.json"
            val tempFile = File(context.cacheDir, backupFileName)
            
            // Write data to the temporary file
            tempFile.writeText(json)
            
            // Now copy this file to Downloads using ContentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, backupFileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "${android.os.Environment.DIRECTORY_DOWNLOADS}/$BACKUP_FOLDER")
                }
            }
            
            val contentResolver = context.contentResolver
            val uri = contentResolver.insert(
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw IOException("Failed to create new MediaStore record")
            
            // Copy content to the new file
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IOException("Failed to open output stream")
            
            // Delete temporary file
            tempFile.delete()
            
            // Return the original file object with the correct path 
            // (we can't access the actual file directly, but we need to return something)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val backupFolder = File(downloadsDir, BACKUP_FOLDER)
            val backupFile = File(backupFolder, backupFileName)
            
            android.util.Log.d("BackupUtil", "Backup created using MediaStore at: ${downloadsDir.absolutePath}/$BACKUP_FOLDER/$backupFileName")
            
            return backupFile
        } catch (e: Exception) {
            android.util.Log.e("BackupUtil", "MediaStore backup failed: ${e.message}", e)
            throw e
        }
    }
} 