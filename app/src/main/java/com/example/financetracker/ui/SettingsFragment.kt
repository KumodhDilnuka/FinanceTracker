package com.example.financetracker.ui

import android.app.Activity
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.financetracker.R
import com.example.financetracker.data.PrefsManager
import com.example.financetracker.data.Transaction
import com.example.financetracker.util.BackupUtil
import com.example.financetracker.util.CurrencyConverter
import com.example.financetracker.util.CurrencyFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File

class SettingsFragment : Fragment() {

    private lateinit var spinnerCurrency: Spinner
    private lateinit var buttonApplyCurrency: Button
    private lateinit var textInputLayoutBudget: TextInputLayout
    private lateinit var editTextBudget: TextInputEditText
    private lateinit var buttonSaveBudget: Button
    private lateinit var textBackupInfo: TextView
    private lateinit var buttonBackup: Button
    private lateinit var buttonRestore: Button
    
    private val REQUEST_OPEN_DOCUMENT = 1001
    private val REQUEST_STORAGE_PERMISSION = 1002
    private val REQUEST_MANAGE_STORAGE = 1003
    
    // Flag to track if spinner selection is from user interaction
    private var isUserInteraction = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize all the UI components
        initViews(view)
        setupCurrencySpinner()
        setupListeners()
        loadSettings()
    }
    
    override fun onResume() {
        super.onResume()
        updateBackupInfo()
        
        // Reset user interaction flag when fragment resumes
        isUserInteraction = false
        
        // Make sure the spinner shows the correct currency
        loadSettings()
        
        // Now enable user interaction tracking
        isUserInteraction = true
    }
    
    private fun initViews(view: View) {
        spinnerCurrency = view.findViewById(R.id.spinnerCurrency)
        buttonApplyCurrency = view.findViewById(R.id.buttonApplyCurrency)
        textInputLayoutBudget = view.findViewById(R.id.textInputLayoutBudget)
        editTextBudget = view.findViewById(R.id.editTextBudget)
        buttonSaveBudget = view.findViewById(R.id.buttonSaveBudget)
        textBackupInfo = view.findViewById(R.id.textBackupInfo)
        buttonBackup = view.findViewById(R.id.buttonBackup)
        buttonRestore = view.findViewById(R.id.buttonRestore)
    }
    
    private fun setupCurrencySpinner() {
        val currencies = CurrencyConverter.getAvailableCurrencies()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            currencies
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCurrency.adapter = adapter
        
        // Set the spinner to the saved currency
        val currentCurrency = PrefsManager.getCurrency()
        val currencyIndex = currencies.indexOf(currentCurrency)
        if (currencyIndex >= 0) {
            spinnerCurrency.setSelection(currencyIndex)
        }
        
        // Add spinner selection listener to update the budget hint and save the selected currency
        spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateBudgetHint()
                
                // Get the selected currency
                val selectedCurrency = parent?.getItemAtPosition(position).toString()
                
                // Save the selected currency - only if user manually changed it
                if (isUserInteraction && selectedCurrency != PrefsManager.getCurrency()) {
                    // We'll apply the currency change when the Apply button is clicked
                    // So we just update the hint text here
                    textInputLayoutBudget.helperText = "Click 'Apply Currency' to convert values to $selectedCurrency"
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    
    private fun updateBudgetHint() {
        val selectedCurrency = spinnerCurrency.selectedItem.toString()
        val currentCurrency = PrefsManager.getCurrency()
        
        if (selectedCurrency != currentCurrency) {
            textInputLayoutBudget.helperText = "Amount will be converted from $selectedCurrency to $currentCurrency"
        } else {
            textInputLayoutBudget.helperText = "Amount in $currentCurrency"
        }
    }
    
    private fun setupListeners() {
        buttonApplyCurrency.setOnClickListener {
            applyNewCurrency()
        }
        
        buttonSaveBudget.setOnClickListener {
            saveBudget()
        }
        
        buttonBackup.setOnClickListener {
            checkAndRequestStoragePermission()
        }
        
        buttonRestore.setOnClickListener {
            openRestoreFilePicker()
        }
    }
    
    private fun loadSettings() {
        // Temporarily disable user interaction tracking to avoid triggering events
        val wasUserInteraction = isUserInteraction
        isUserInteraction = false
        
        val currentCurrency = PrefsManager.getCurrency()
        val currencies = CurrencyConverter.getAvailableCurrencies()
        val currencyIndex = currencies.indexOf(currentCurrency)
        if (currencyIndex >= 0) {
            spinnerCurrency.setSelection(currencyIndex)
        }
        
        val budget = PrefsManager.getBudget()
        if (budget > 0) {
            editTextBudget.setText(budget.toString())
        }
        
        updateBudgetHint()
        updateBackupInfo()
        
        // Restore user interaction tracking
        isUserInteraction = wasUserInteraction
    }
    
    private fun updateBackupInfo() {
        val backupInfo = BackupUtil.getBackupInfo(requireContext())
        textBackupInfo.text = backupInfo ?: "No backup available"
    }
    
    private fun applyNewCurrency() {
        val oldCurrency = PrefsManager.getCurrency()
        val newCurrency = spinnerCurrency.selectedItem.toString()
        
        if (oldCurrency == newCurrency) {
            showMessage("Currency already set to $newCurrency")
            return
        }
        
        // Get conversion factor between old and new currency
        val factor = CurrencyConverter.getConversionFactor(oldCurrency, newCurrency)
        
        // Convert all transactions to new currency
        val transactions = PrefsManager.loadTransactions()
        
        // Use our own conversion logic to avoid type mismatches
        val updatedTransactions = transactions.map { transaction ->
            transaction.copy(amount = transaction.amount * factor)
        }
        
        // Also convert the budget
        val oldBudget = PrefsManager.getBudget()
        val newBudget = oldBudget * factor
        
        // Save updated transactions and currency setting
        PrefsManager.saveTransactions(updatedTransactions)
        PrefsManager.setCurrency(newCurrency)
        PrefsManager.setBudget(newBudget)
        
        // Update budget display
        if (oldBudget > 0) {
            editTextBudget.setText(newBudget.toString())
        }
        
        // Update the budget hint
        updateBudgetHint()
        
        showMessage("Currency changed to $newCurrency and all transactions converted")
    }
    
    private fun saveBudget() {
        val budgetStr = editTextBudget.text.toString().trim()
        if (budgetStr.isEmpty()) {
            showMessage("Please enter a monthly budget amount")
            return
        }
        
        try {
            val inputBudget = budgetStr.toDouble()
            if (inputBudget <= 0) {
                showMessage("Budget must be greater than zero")
                return
            }
            
            val currentCurrency = PrefsManager.getCurrency()
            // Get the currency the user is viewing/interacting with
            val selectedCurrency = spinnerCurrency.selectedItem.toString()
            
            // Convert budget value from the selected currency to the current system currency
            val finalBudget = if (selectedCurrency != currentCurrency) {
                // Get conversion factor between currencies
                val factor = CurrencyConverter.getConversionFactor(selectedCurrency, currentCurrency)
                inputBudget * factor
            } else {
                inputBudget
            }
            
            PrefsManager.setBudget(finalBudget)
            
            // Show a message with both original and converted amounts if conversion happened
            if (selectedCurrency != currentCurrency) {
                val factor = CurrencyConverter.getConversionFactor(selectedCurrency, currentCurrency)
                val originalFormatted = CurrencyFormatter.formatCurrency(inputBudget, selectedCurrency)
                val convertedFormatted = CurrencyFormatter.formatCurrency(finalBudget, currentCurrency)
                
                val conversionRate = "1 $selectedCurrency = ${factor.roundToTwoDecimals()} $currentCurrency"
                showMessage("Monthly budget set: $originalFormatted → $convertedFormatted ($conversionRate)")
            } else {
                showMessage("Monthly budget of ${CurrencyFormatter.formatCurrency(finalBudget, currentCurrency)} saved successfully")
            }
        } catch (e: NumberFormatException) {
            showMessage("Please enter a valid number")
        }
    }
    
    // Extension function to round to 2 decimal places for cleaner display
    private fun Double.roundToTwoDecimals(): Double {
        return (this * 100.0).toInt() / 100.0
    }
    
    private fun checkAndRequestStoragePermission() {
        // For Android 11 (API 30) and higher
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                createBackup()
            } else {
                showManageStoragePermissionDialog()
            }
        } 
        // For Android 10 (API 29) and lower
        else {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                createBackup()
            } else {
                requestPermissions(arrayOf(permission), REQUEST_STORAGE_PERMISSION)
            }
        }
    }
    
    private fun showManageStoragePermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Storage Permission Required")
            .setMessage("This app needs storage permission to save backups to your Downloads folder. Please grant the permission in the next screen.")
            .setPositiveButton("Grant Permission") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${requireContext().packageName}")
                    startActivityForResult(intent, REQUEST_MANAGE_STORAGE)
                } catch (e: Exception) {
                    // Fallback if specific intent not available
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(intent, REQUEST_MANAGE_STORAGE)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createBackup()
            } else {
                showMessage("Storage permission is required to save backups")
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
    
    private fun createBackup() {
        var backupFile: File? = null
        var errorMessage: String? = null
        
        // Try the primary backup method first
        try {
            backupFile = BackupUtil.createBackup(requireContext())
            
            // Verify the file exists and has content
            if (backupFile.exists() && backupFile.length() > 0) {
                val path = if (BackupUtil.BACKUP_FOLDER.isEmpty()) {
                    "Downloads/${backupFile.name}"
                } else {
                    "Downloads/${BackupUtil.BACKUP_FOLDER}/${backupFile.name}"
                }
                showMessage("Backup created: $path")
                updateBackupInfo()
                return
            } else {
                errorMessage = "File not created properly. Trying alternative method..."
                android.util.Log.w("SettingsFragment", errorMessage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Primary backup method failed: ${e.message}. Trying alternative method..."
            android.util.Log.e("SettingsFragment", errorMessage, e)
        }
        
        // If primary method failed, try the MediaStore method
        try {
            backupFile = BackupUtil.createBackupUsingMediaStore(requireContext())
            val path = if (BackupUtil.BACKUP_FOLDER.isEmpty()) {
                "Downloads/${backupFile.name}"
            } else {
                "Downloads/${BackupUtil.BACKUP_FOLDER}/${backupFile.name}"
            }
            showMessage("Backup created: $path")
            updateBackupInfo()
        } catch (e: Exception) {
            e.printStackTrace()
            
            // Both methods failed, show a detailed error
            val finalError = when {
                e.message?.contains("permission") == true -> 
                    "Storage permission denied. Please grant storage permissions in your device settings."
                
                e.message?.contains("directory") == true -> 
                    "Could not create the backup directory. Please check your device storage."
                
                else -> "Failed to create backup: ${e.message}"
            }
            
            // Show error dialog with more details
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Backup Failed")
                .setMessage("$finalError\n\nFirst attempt: $errorMessage\n\nSecond attempt: ${e.message}")
                .setPositiveButton("OK", null)
                .show()
            
            showMessage(finalError)
            
            // Log the error for debugging
            android.util.Log.e("SettingsFragment", "Both backup methods failed", e)
        }
    }
    
    private fun openRestoreFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, REQUEST_OPEN_DOCUMENT)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_OPEN_DOCUMENT -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri ->
                        try {
                            // Copy the content to a temporary file
                            val inputStream = requireContext().contentResolver.openInputStream(uri)
                            val tempFile = File(requireContext().cacheDir, "temp_backup.json")
                            
                            inputStream?.use { input ->
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            
                            // Restore from the temporary file
                            val success = BackupUtil.restoreFromFile(requireContext(), tempFile)
                            
                            if (success) {
                                showMessage("Backup restored successfully")
                                updateBackupInfo()
                                loadSettings() // Reload settings after restore
                            } else {
                                showMessage("Failed to restore backup")
                            }
                            
                            // Delete the temporary file
                            tempFile.delete()
                        } catch (e: Exception) {
                            showMessage("Error restoring backup: ${e.message}")
                        }
                    }
                }
            }
            REQUEST_MANAGE_STORAGE -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        createBackup()
                    } else {
                        showMessage("Storage permission is required to save backups")
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
    
    private fun showMessage(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }
} 