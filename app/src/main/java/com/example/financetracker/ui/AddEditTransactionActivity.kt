package com.example.financetracker.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.financetracker.R
import com.example.financetracker.data.Category
import com.example.financetracker.data.PrefsManager
import com.example.financetracker.data.Transaction
import com.example.financetracker.data.TxType
import com.example.financetracker.notification.NotificationHelper
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class AddEditTransactionActivity : AppCompatActivity() {
    
    private lateinit var textTitle: TextView
    private lateinit var radioGroupType: RadioGroup
    private lateinit var radioIncome: RadioButton
    private lateinit var radioExpense: RadioButton
    private lateinit var editTextTitle: TextInputEditText
    private lateinit var editTextAmount: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var textDate: TextView
    private lateinit var buttonPickDate: Button
    private lateinit var editTextNotes: TextInputEditText
    private lateinit var buttonSave: Button
    
    private var transactionDate = System.currentTimeMillis()
    private var editingTransactionId: String? = null
    private var incomeCategories: List<String> = emptyList()
    private var expenseCategories: List<String> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_transaction)
        
        // Initialize UI components
        initViews()
        
        // Load existing transaction if editing
        editingTransactionId = intent.getStringExtra("transaction_id")
        if (editingTransactionId != null) {
            loadExistingTransaction(editingTransactionId!!)
            textTitle.text = "Edit Transaction"
        } else {
            textTitle.text = "Add Transaction"
        }
        
        // Setup date picker
        setupDatePicker()
        
        // Load categories
        loadCategories()
        
        // Update spinner based on current selection
        updateCategorySpinner()
        
        // Setup listeners
        setupListeners()
    }
    
    private fun initViews() {
        textTitle = findViewById(R.id.textTitle)
        radioGroupType = findViewById(R.id.radioGroupType)
        radioIncome = findViewById(R.id.radioIncome)
        radioExpense = findViewById(R.id.radioExpense)
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextAmount = findViewById(R.id.editTextAmount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        textDate = findViewById(R.id.textDate)
        buttonPickDate = findViewById(R.id.buttonPickDate)
        editTextNotes = findViewById(R.id.editTextNotes)
        buttonSave = findViewById(R.id.buttonSave)
        
        // Set current date
        updateDateDisplay()
    }
    
    private fun loadExistingTransaction(transactionId: String) {
        val transactions = PrefsManager.loadTransactions()
        val transaction = transactions.find { it.id == transactionId }
        
        if (transaction != null) {
            // Fill the form with existing data
            editTextTitle.setText(transaction.title)
            editTextAmount.setText(transaction.amount.toString())
            editTextNotes.setText(transaction.note)
            transactionDate = transaction.date
            
            if (transaction.type == TxType.INCOME) {
                radioIncome.isChecked = true
            } else {
                radioExpense.isChecked = true
            }
            
            // Update the date display
            updateDateDisplay()
        }
    }
    
    private fun setupDatePicker() {
        buttonPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = transactionDate
            
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                    transactionDate = selectedCalendar.timeInMillis
                    updateDateDisplay()
                },
                year, month, day
            )
            
            datePickerDialog.show()
        }
    }
    
    private fun loadCategories() {
        val categories = PrefsManager.loadCategories()
        
        incomeCategories = categories
            .filter { it.type == TxType.INCOME }
            .map { it.name }
            
        expenseCategories = categories
            .filter { it.type == TxType.EXPENSE }
            .map { it.name }
    }
    
    private fun updateCategorySpinner() {
        val isIncome = radioIncome.isChecked
        val categories = PrefsManager.loadCategories().filter { 
            it.type == if (isIncome) TxType.INCOME else TxType.EXPENSE 
        }
        
        // Create adapter with custom view to display emoji + category name
        val adapter = object : ArrayAdapter<Category>(
            this, 
            android.R.layout.simple_spinner_item, 
            categories
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val category = getItem(position)
                
                if (view is TextView && category != null) {
                    val displayText = if (category.emoji != null && category.emoji.isNotEmpty()) {
                        "${category.emoji} ${category.name}"
                    } else {
                        category.name
                    }
                    view.text = displayText
                }
                
                return view
            }
            
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val category = getItem(position)
                
                if (view is TextView && category != null) {
                    val displayText = if (category.emoji != null && category.emoji.isNotEmpty()) {
                        "${category.emoji} ${category.name}"
                    } else {
                        category.name
                    }
                    view.text = displayText
                }
                
                return view
            }
        }
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
        
        // Set selected category if we're editing
        if (editingTransactionId != null) {
            val transactions = PrefsManager.loadTransactions()
            val transaction = transactions.find { it.id == editingTransactionId }
            
            if (transaction != null) {
                val position = categories.indexOfFirst { it.name == transaction.category }
                if (position >= 0) {
                    spinnerCategory.setSelection(position)
                }
            }
        }
    }
    
    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        textDate.text = dateFormat.format(Date(transactionDate))
    }
    
    private fun setupListeners() {
        // Radio group change listener to update category spinner
        radioGroupType.setOnCheckedChangeListener { _, _ ->
            updateCategorySpinner()
        }
        
        // Save button click listener
        buttonSave.setOnClickListener {
            saveTransaction()
        }
    }
    
    private fun saveTransaction() {
        // Validate input
        val title = editTextTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }
        
        val amountStr = editTextAmount.text.toString().trim()
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            return
        }
        
        val amount: Double
        try {
            amount = amountStr.toDouble()
            if (amount <= 0) {
                Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (spinnerCategory.adapter.count == 0) {
            Toast.makeText(this, "No categories available. Please add categories first.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get the selected Category object to extract the name
        val selectedCategory = (spinnerCategory.selectedItem as? Category)?.name
                ?: spinnerCategory.selectedItem?.toString() // Fallback to direct toString if cast fails
        
        if (selectedCategory == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }
        
        val note = editTextNotes.text.toString().trim()
        val type = if (radioIncome.isChecked) TxType.INCOME else TxType.EXPENSE
        
        // Create or update transaction
        val transactions = PrefsManager.loadTransactions().toMutableList()
        
        if (editingTransactionId != null) {
            // Update existing transaction
            val index = transactions.indexOfFirst { it.id == editingTransactionId }
            if (index >= 0) {
                transactions[index] = Transaction(
                    id = editingTransactionId!!,
                    title = title,
                    amount = amount,
                    category = selectedCategory,
                    type = type,
                    date = transactionDate,
                    note = note
                )
            }
        } else {
            // Add new transaction
            transactions.add(
                Transaction(
                    title = title,
                    amount = amount,
                    category = selectedCategory,
                    type = type,
                    date = transactionDate,
                    note = note
                )
            )
        }
        
        // Save to storage
        PrefsManager.saveTransactions(transactions)
        
        // Check budget status and send notification if needed
        checkBudgetStatus()
        
        // Show success message and finish activity
        Toast.makeText(this, "Transaction saved successfully", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    /**
     * Checks the current budget status and sends a notification if the budget is exceeded
     */
    private fun checkBudgetStatus() {
        // Only check budget for expense transactions
        if (radioExpense.isChecked) {
            val budget = PrefsManager.getBudget()
            
            // Skip if no budget is set
            if (budget <= 0) return
            
            // Calculate current month expenses only
            val transactions = PrefsManager.loadTransactions()
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            
            val monthlyExpenses = transactions
                .filter { transaction -> 
                    val transactionCal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                    transaction.type == TxType.EXPENSE &&
                    transactionCal.get(Calendar.MONTH) == currentMonth &&
                    transactionCal.get(Calendar.YEAR) == currentYear
                }
                .sumOf { it.amount }
            
            // Log to debug
            android.util.Log.d("AddEditTransaction", "Monthly expenses for budget check: $monthlyExpenses / $budget")
            
            // Check if budget is exceeded and send notification
            NotificationHelper.checkBudgetStatus(this, monthlyExpenses, budget)
        }
    }
} 