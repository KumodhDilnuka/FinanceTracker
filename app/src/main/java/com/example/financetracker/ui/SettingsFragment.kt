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
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatDelegate
import com.example.financetracker.R
import com.example.financetracker.data.PrefsManager
import com.example.financetracker.data.Transaction
import com.example.financetracker.util.BackupUtil
import com.example.financetracker.util.CurrencyConverter
import com.example.financetracker.util.CurrencyFormatter
import com.example.financetracker.util.PDFExporter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import android.os.Build
import java.util.*
import java.text.SimpleDateFormat
import com.example.financetracker.util.ReminderPrefs
import com.example.financetracker.notification.ReminderScheduler
import android.app.TimePickerDialog
import com.example.financetracker.notification.NotificationHelper

class SettingsFragment : Fragment() {

    private lateinit var spinnerCurrency: Spinner
    private lateinit var buttonApplyCurrency: Button
    private lateinit var textInputLayoutBudget: TextInputLayout
    private lateinit var editTextBudget: TextInputEditText
    private lateinit var buttonSaveBudget: Button
    private lateinit var textBackupInfo: TextView
    private lateinit var buttonBackup: Button
    private lateinit var buttonRestore: Button
    private lateinit var switchDarkMode: com.google.android.material.switchmaterial.SwitchMaterial
    private lateinit var radioGroupBackupStorage: RadioGroup
    private lateinit var radioInternalStorage: RadioButton
    private lateinit var radioExternalStorage: RadioButton
    private lateinit var switchAppLock: com.google.android.material.switchmaterial.SwitchMaterial
    private lateinit var buttonChangePasscode: com.google.android.material.button.MaterialButton
    private lateinit var buttonExportPDF: com.google.android.material.button.MaterialButton
    private lateinit var textReminderTime: TextView
    private lateinit var buttonChangeReminderTime: com.google.android.material.button.MaterialButton
    
    private val REQUEST_OPEN_DOCUMENT = 1001
    private val REQUEST_STORAGE_PERMISSION = 1002
    private val REQUEST_MANAGE_STORAGE = 1003
    private val REQUEST_PDF_PERMISSION = 1004
    
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
        
        // Update passcode UI based on current state
        val passcodeEnabled = com.example.financetracker.util.SecurityManager.isPasscodeEnabled(requireContext())
        switchAppLock.isChecked = passcodeEnabled
        buttonChangePasscode.visibility = if (passcodeEnabled) View.VISIBLE else View.GONE
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
        switchDarkMode = view.findViewById(R.id.switchDarkMode)
        
        // Initialize PDF export button
        buttonExportPDF = view.findViewById(R.id.buttonExportPDF)
        
        // Initialize storage option radio buttons
        radioGroupBackupStorage = view.findViewById(R.id.radioGroupBackupStorage)
        radioInternalStorage = view.findViewById(R.id.radioInternalStorage)
        radioExternalStorage = view.findViewById(R.id.radioExternalStorage)
        
        // Set default selection based on preference or use external by default
        if (PrefsManager.getUseInternalStorageForBackup()) {
            radioInternalStorage.isChecked = true
        } else {
            radioExternalStorage.isChecked = true
        }
        
        // Initialize security controls
        switchAppLock = view.findViewById(R.id.switchAppLock)
        buttonChangePasscode = view.findViewById(R.id.buttonChangePasscode)
        
        // Set initial state from preferences
        switchAppLock.isChecked = com.example.financetracker.util.SecurityManager.isPasscodeEnabled(requireContext())
        buttonChangePasscode.visibility = if (switchAppLock.isChecked) View.VISIBLE else View.GONE
        
        // Initialize reminder time views
        textReminderTime = view.findViewById(R.id.textReminderTime)
        buttonChangeReminderTime = view.findViewById(R.id.buttonChangeReminderTime)
        
        // Set initial values
        updateReminderTimeText()
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
            if (radioExternalStorage.isChecked) {
                checkAndRequestStoragePermission()
            } else {
                // Internal storage doesn't need permissions
                createInternalBackup()
            }
        }
        
        buttonRestore.setOnClickListener {
            openRestoreFilePicker()
        }
        
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            applyDarkMode(isChecked)
        }
        
        // Setup PDF export button
        buttonExportPDF.setOnClickListener {
            exportToPDF()
        }
        
        // Save the backup storage preference when changed
        radioGroupBackupStorage.setOnCheckedChangeListener { _, checkedId ->
            val useInternalStorage = checkedId == R.id.radioInternalStorage
            PrefsManager.setUseInternalStorageForBackup(useInternalStorage)
        }
        
        // Setup app lock switch
        switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Enable passcode
                if (com.example.financetracker.util.SecurityManager.isPasscodeEnabled(requireContext())) {
                    // Passcode is already set, just show the change button
                    buttonChangePasscode.visibility = View.VISIBLE
                } else {
                    // Launch passcode creation
                    PasscodeActivity.startForCreation(requireContext())
                }
            } else {
                // Disable passcode
                if (com.example.financetracker.util.SecurityManager.isPasscodeEnabled(requireContext())) {
                    // Verify passcode before disabling
                    showPasscodeDisableConfirmation()
                }
            }
        }
        
        // Setup change passcode button
        buttonChangePasscode.setOnClickListener {
            PasscodeActivity.startForChange(requireContext())
        }
        
        // Setup reminder time button
        buttonChangeReminderTime.setOnClickListener {
            showTimePickerDialog()
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
        
        // Set dark mode switch based on saved preference
        switchDarkMode.isChecked = com.example.financetracker.util.PrefsManager.getThemeMode()
        
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
        // For Android 10+ (Q), we can use the MediaStore API or scoped storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            createExternalBackup()
            return
        }
        
        // For older Android versions, check WRITE_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {
            
            // Request the permission
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
        } else {
            // Permission already granted, proceed with backup
            createExternalBackup()
        }
    }
    
    private fun createExternalBackup() {
        try {
            val backupFile = BackupUtil.createBackup(requireContext(), BackupUtil.BackupStorageType.EXTERNAL)
            Snackbar.make(
                requireView(),
                "Backup saved to Downloads: ${backupFile.name}",
                Snackbar.LENGTH_LONG
            ).show()
            updateBackupInfo()
        } catch (e: Exception) {
            Snackbar.make(
                requireView(),
                "Backup failed: ${e.message}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
    
    private fun createInternalBackup() {
        try {
            val backupFile = BackupUtil.createBackup(requireContext(), BackupUtil.BackupStorageType.INTERNAL)
            Snackbar.make(
                requireView(),
                "Backup saved to internal storage: ${backupFile.name}",
                Snackbar.LENGTH_LONG
            ).show()
            updateBackupInfo()
        } catch (e: Exception) {
            Snackbar.make(
                requireView(),
                "Backup failed: ${e.message}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
    
    private fun openRestoreFilePicker() {
        // If internal storage is selected, use our custom picker dialog
        if (radioInternalStorage.isChecked) {
            showInternalStorageRestoreDialog()
        } else {
            // For external storage, use the system file picker
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, REQUEST_OPEN_DOCUMENT)
        }
    }
    
    /**
     * Shows a dialog with a list of available internal storage backup files
     */
    private fun showInternalStorageRestoreDialog() {
        // Get list of backup files from internal storage
        val backupFolder = File(requireContext().filesDir, BackupUtil.INTERNAL_BACKUP_FOLDER)
        
        // Log path for debugging
        val logTag = "SettingsFragment"
        android.util.Log.d(logTag, "Looking for backups in: ${backupFolder.absolutePath}")
        android.util.Log.d(logTag, "Folder exists: ${backupFolder.exists()}")
        
        if (!backupFolder.exists()) {
            showMessage("No backup folder found. Please create a backup first.")
            return
        }
        
        val allFiles = backupFolder.listFiles()
        android.util.Log.d(logTag, "Files found: ${allFiles?.size ?: 0}")
        
        if (allFiles == null || allFiles.isEmpty()) {
            showMessage("No files found in backup folder")
            return
        }
        
        // Get all backup files and sort by date (newest first)
        val backupFiles = allFiles.filter { 
            val isBackup = (it.name.startsWith("FinanceTracker_backup_") || it.name.startsWith("backup_")) && 
                    it.name.endsWith(".json")
            android.util.Log.d(logTag, "File: ${it.name}, isBackup: $isBackup")
            isBackup
        }.sortedByDescending { it.lastModified() }
        
        if (backupFiles.isEmpty()) {
            showMessage("No backup files found in the backup folder")
            return
        }
        
        // Create a list of backup file names with dates for the dialog
        val backupDates = backupFiles.map { file ->
            val date = Date(file.lastModified())
            val formatter = SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault())
            "${formatter.format(date)} (${file.name})"
        }.toTypedArray()
        
        // Show dialog to select a backup file
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Backup to Restore")
            .setItems(backupDates) { _, index ->
                // User selected a backup file - confirm restore
                val selectedFile = backupFiles[index]
                
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Confirm Restore")
                    .setMessage("Restore backup from ${backupDates[index]}? This will replace all current data.")
                    .setPositiveButton("Restore") { _, _ -> 
                        restoreFromFile(selectedFile)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Restores data from the provided backup file
     */
    private fun restoreFromFile(file: File) {
        try {
            val success = BackupUtil.restoreFromFile(requireContext(), file)
            
            if (success) {
                showMessage("Backup restored successfully")
                updateBackupInfo()
                loadSettings() // Reload settings after restore
            } else {
                showMessage("Failed to restore backup")
            }
        } catch (e: Exception) {
            showMessage("Error restoring backup: ${e.message}")
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
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
                        createExternalBackup()
                    } else {
                        showMessage("Storage permission is required to save backups")
                    }
                }
            }
            REQUEST_DISABLE_PASSCODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    // Passcode verified, disable passcode
                    com.example.financetracker.util.SecurityManager.setPasscodeEnabled(requireContext(), false)
                    com.example.financetracker.util.SecurityManager.clearPasscode(requireContext())
                    buttonChangePasscode.visibility = View.GONE
                    showMessage("App lock disabled")
                } else {
                    // Failed verification or canceled, reset the switch
                    switchAppLock.isChecked = true
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
    
    private fun showMessage(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }
    
    private fun applyDarkMode(isDarkMode: Boolean) {
        // Save the theme preference
        com.example.financetracker.util.PrefsManager.setThemeMode(isDarkMode)
        
        // Apply the theme mode
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    
    /**
     * Shows a confirmation dialog before disabling passcode
     */
    private fun showPasscodeDisableConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Disable App Lock")
            .setMessage("Are you sure you want to disable app lock? This will remove your passcode protection.")
            .setPositiveButton("Disable") { _, _ ->
                // Create an intent to verify the current passcode
                val intent = Intent(requireContext(), PasscodeActivity::class.java)
                intent.putExtra(PasscodeActivity.EXTRA_MODE, PasscodeActivity.MODE_VERIFY)
                startActivityForResult(intent, REQUEST_DISABLE_PASSCODE)
            }
            .setNegativeButton("Cancel") { _, _ ->
                // User canceled, reset the switch
                switchAppLock.isChecked = true
            }
            .show()
    }
    
    /**
     * Process permission request results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createExternalBackup()
                } else {
                    showSnackbar("Storage permission denied. Cannot create backup.")
                }
            }
            REQUEST_PDF_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    exportToPDF()
                } else {
                    showSnackbar("Storage permission denied. Cannot export PDF.")
                }
            }
        }
    }

    /**
     * Export transaction data to PDF
     */
    private fun exportToPDF() {
        if (PrefsManager.loadTransactions().isEmpty()) {
            showSnackbar("No transactions to export")
            return
        }
        
        val useInternalStorage = radioInternalStorage.isChecked
        
        if (!useInternalStorage) {
            // Check for storage permission if using external storage
            if (!checkStoragePermission()) {
                requestStoragePermission(REQUEST_PDF_PERMISSION)
                return
            }
        }
        
        try {
            // Show loading indicator
            showProgressDialog("Generating PDF...")
            
            // Run PDF generation in a background thread to avoid UI freezing
            Thread {
                try {
                    val pdfPath = PDFExporter.exportTransactionsToPDF(requireContext(), useInternalStorage)
                    
                    // Update UI on the main thread
                    requireActivity().runOnUiThread {
                        dismissProgressDialog()
                        
                        if (pdfPath != null) {
                            showPdfGeneratedDialog(pdfPath)
                        } else {
                            showSnackbar("Failed to generate PDF")
                        }
                    }
                } catch (e: Exception) {
                    requireActivity().runOnUiThread {
                        dismissProgressDialog()
                        showSnackbar("Error: ${e.message}")
                    }
                }
            }.start()
            
        } catch (e: Exception) {
            dismissProgressDialog()
            showSnackbar("Error generating PDF: ${e.message}")
        }
    }
    
    /**
     * Show a dialog after the PDF is generated
     */
    private fun showPdfGeneratedDialog(pdfPath: String) {
        val file = File(pdfPath)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("PDF Generated Successfully")
            .setMessage("PDF saved to:\n$pdfPath")
            .setPositiveButton("Open") { _, _ ->
                openPdfFile(file)
            }
            .setNegativeButton("Share") { _, _ ->
                sharePdfFile(file)
            }
            .setNeutralButton("Close", null)
            .show()
    }
    
    /**
     * Open the generated PDF file
     */
    private fun openPdfFile(file: File) {
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "Open PDF with"))
        } catch (e: Exception) {
            showSnackbar("No PDF viewer app found or error opening file")
        }
    }
    
    /**
     * Share the generated PDF file
     */
    private fun sharePdfFile(file: File) {
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Finance Tracker Report")
                putExtra(Intent.EXTRA_TEXT, "Attached is my financial report from Finance Tracker app.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "Share PDF via"))
        } catch (e: Exception) {
            showSnackbar("Error sharing file: ${e.message}")
        }
    }
    
    /**
     * Show a progress dialog while processing
     */
    private fun showProgressDialog(message: String) {
        // Use the existing progress dialog implementation or create a new one
    }
    
    /**
     * Dismiss the progress dialog
     */
    private fun dismissProgressDialog() {
        // Use the existing progress dialog implementation or create a new one
    }
    
    /**
     * Show a snackbar message
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
    }
    
    /**
     * Check if we have storage permission
     */
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val writePermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val readPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Request storage permission
     */
    private fun requestStoragePermission(requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", requireContext().packageName))
                startActivityForResult(intent, REQUEST_MANAGE_STORAGE)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, REQUEST_MANAGE_STORAGE)
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                requestCode
            )
        }
    }

    /**
     * Update the reminder time text with the current setting
     */
    private fun updateReminderTimeText() {
        textReminderTime.text = ReminderPrefs.getFormattedReminderTime()
    }
    
    /**
     * Show time picker dialog to set the reminder time
     */
    private fun showTimePickerDialog() {
        // Get current reminder time
        val currentHour = ReminderPrefs.getReminderHour()
        val currentMinute = ReminderPrefs.getReminderMinute()
        
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                // Save the new time
                ReminderPrefs.setReminderTime(hourOfDay, minute)
                
                // Update the display
                updateReminderTimeText()
                
                // Reschedule the reminder with the new time
                rescheduleReminder(hourOfDay, minute)
            },
            currentHour,
            currentMinute,
            false // Not 24-hour format
        )
        
        timePickerDialog.show()
    }
    
    /**
     * Reschedule the daily reminder with the new time
     */
    private fun rescheduleReminder(hour: Int, minute: Int) {
        try {
            // Cancel the existing reminder
            ReminderScheduler.cancelDailyReminder(requireContext())
            
            // Schedule a new reminder with the updated time
            ReminderScheduler.scheduleDailyReminder(requireContext(), hour, minute)
            
            // Show simple confirmation
            showSnackbar("Daily reminder set for " + ReminderPrefs.getFormattedReminderTime())
            
        } catch (e: Exception) {
            showSnackbar("Error updating reminder: ${e.message}")
        }
    }

    companion object {
        private const val REQUEST_OPEN_DOCUMENT = 1001
        private const val REQUEST_STORAGE_PERMISSION = 1002
        private const val REQUEST_MANAGE_STORAGE = 1003
        private const val REQUEST_DISABLE_PASSCODE = 1004
    }
} 