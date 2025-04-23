package com.example.financetracker.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.financetracker.R
import com.example.financetracker.data.PrefsManager
import com.example.financetracker.data.Transaction
import com.example.financetracker.util.BackupUtil
import com.example.financetracker.util.CurrencyConverter
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class SettingsFragment : Fragment() {

    private lateinit var spinnerCurrency: Spinner
    private lateinit var buttonApplyCurrency: Button
    private lateinit var editTextBudget: TextInputEditText
    private lateinit var buttonSaveBudget: Button
    private lateinit var textBackupInfo: TextView
    private lateinit var buttonBackup: Button
    private lateinit var buttonRestore: Button
    
    private val REQUEST_OPEN_DOCUMENT = 1001
    
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
    }
    
    private fun initViews(view: View) {
        spinnerCurrency = view.findViewById(R.id.spinnerCurrency)
        buttonApplyCurrency = view.findViewById(R.id.buttonApplyCurrency)
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
    }
    
    private fun setupListeners() {
        buttonApplyCurrency.setOnClickListener {
            applyNewCurrency()
        }
        
        buttonSaveBudget.setOnClickListener {
            saveBudget()
        }
        
        buttonBackup.setOnClickListener {
            createBackup()
        }
        
        buttonRestore.setOnClickListener {
            openRestoreFilePicker()
        }
    }
    
    private fun loadSettings() {
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
        
        updateBackupInfo()
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
        
        // Save updated transactions and currency setting
        PrefsManager.saveTransactions(updatedTransactions)
        PrefsManager.setCurrency(newCurrency)
        
        showMessage("Currency changed to $newCurrency and all transactions converted")
    }
    
    private fun saveBudget() {
        val budgetStr = editTextBudget.text.toString().trim()
        if (budgetStr.isEmpty()) {
            showMessage("Please enter a budget amount")
            return
        }
        
        try {
            val budget = budgetStr.toDouble()
            if (budget <= 0) {
                showMessage("Budget must be greater than zero")
                return
            }
            
            PrefsManager.setBudget(budget)
            showMessage("Budget saved successfully")
        } catch (e: NumberFormatException) {
            showMessage("Please enter a valid number")
        }
    }
    
    private fun createBackup() {
        try {
            val backupFile = BackupUtil.createBackup(requireContext())
            showMessage("Backup created: ${backupFile.name}")
            updateBackupInfo()
        } catch (e: Exception) {
            showMessage("Failed to create backup: ${e.message}")
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
        if (requestCode == REQUEST_OPEN_DOCUMENT && resultCode == Activity.RESULT_OK) {
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
        super.onActivityResult(requestCode, resultCode, data)
    }
    
    private fun showMessage(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }
} 