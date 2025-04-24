package com.example.financetracker.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.financetracker.R
import com.example.financetracker.data.PrefsManager
import com.example.financetracker.data.Transaction
import com.example.financetracker.data.TxType
import com.example.financetracker.util.CurrencyFormatter
import com.example.financetracker.util.DateUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.material.button.MaterialButton

class TransactionsFragment : Fragment() {

    private lateinit var recyclerViewTransactions: RecyclerView
    private lateinit var textViewEmpty: TextView
    private lateinit var transactionAdapter: TransactionAdapter
    
    // Tab views for filtering
    private lateinit var tabThisMonth: MaterialButton
    private lateinit var tabThisYear: MaterialButton
    private lateinit var tabAllTime: MaterialButton
    
    private var transactions = listOf<Transaction>()
    private var filteredTransactions = listOf<Transaction>()
    private var currentFilter = TransactionTimeFilter.CURRENT_MONTH

    enum class TransactionTimeFilter {
        CURRENT_MONTH,
        THIS_YEAR,
        ALL_TIME
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerViewTransactions = view.findViewById(R.id.recyclerViewAllTransactions)
        textViewEmpty = view.findViewById(R.id.textViewEmptyTransactions)
        
        // Initialize tab views
        tabThisMonth = view.findViewById(R.id.tabThisMonth)
        tabThisYear = view.findViewById(R.id.tabThisYear)
        tabAllTime = view.findViewById(R.id.tabAllTime)
        
        // Initialize the adapter with a LinearLayoutManager
        recyclerViewTransactions.layoutManager = LinearLayoutManager(requireContext())
        
        setupTransactionsList()
        setupTimeFilterTabs()
        setupFab(view)
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun setupTransactionsList() {
        transactionAdapter = TransactionAdapter(
            emptyList(),
            { transaction -> editTransaction(transaction) },
            { transaction -> showDeleteTransactionDialog(transaction); true }
        )
        recyclerViewTransactions.adapter = transactionAdapter
    }
    
    private fun setupTimeFilterTabs() {
        // Set up click listeners for tabs
        tabThisMonth.setOnClickListener { selectTab(TransactionTimeFilter.CURRENT_MONTH) }
        tabThisYear.setOnClickListener { selectTab(TransactionTimeFilter.THIS_YEAR) }
        tabAllTime.setOnClickListener { selectTab(TransactionTimeFilter.ALL_TIME) }
        
        // Set initial selection
        selectTab(currentFilter)
    }
    
    private fun selectTab(filter: TransactionTimeFilter) {
        // Reset all tabs
        val tabs = listOf(tabThisMonth, tabThisYear, tabAllTime)
        tabs.forEach { tab ->
            tab.setTextColor(resources.getColor(R.color.black, null))
            tab.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1EEFF"))
            tab.setStrokeColorResource(android.R.color.transparent)
            tab.strokeWidth = 0
            tab.icon = null
        }
        
        // Set selected tab based on filter
        val selectedTab = when (filter) {
            TransactionTimeFilter.CURRENT_MONTH -> tabThisMonth
            TransactionTimeFilter.THIS_YEAR -> tabThisYear
            TransactionTimeFilter.ALL_TIME -> tabAllTime
        }
        
        // Set selected tab properties
        selectedTab.setTextColor(resources.getColor(R.color.white, null))
        selectedTab.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.primary, null))
        
        // Apply filter if changed
        if (currentFilter != filter) {
            currentFilter = filter
            applyFilter()
        }
    }

    private fun setupFab(view: View) {
        val fab: FloatingActionButton = view.findViewById(R.id.fabAddTransaction)
        fab.setOnClickListener {
            val intent = Intent(requireContext(), AddEditTransactionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadTransactions() {
        transactions = PrefsManager.loadTransactions().sortedByDescending { it.date }
        applyFilter()
    }
    
    private fun applyFilter() {
        filteredTransactions = when (currentFilter) {
            TransactionTimeFilter.CURRENT_MONTH -> {
                transactions.filter { DateUtils.isCurrentMonth(it.date) }
            }
            TransactionTimeFilter.THIS_YEAR -> {
                transactions.filter { DateUtils.isCurrentYear(it.date) }
            }
            TransactionTimeFilter.ALL_TIME -> {
                transactions
            }
        }
        
        updateEmptyState()
    }
    
    private fun updateEmptyState() {
        if (filteredTransactions.isEmpty()) {
            val noTransactionsText = when (currentFilter) {
                TransactionTimeFilter.CURRENT_MONTH -> "No transactions this month"
                TransactionTimeFilter.THIS_YEAR -> "No transactions this year"
                TransactionTimeFilter.ALL_TIME -> "No transactions yet"
            }
            
            textViewEmpty.text = "$noTransactionsText\nTap + to add a new transaction"
            textViewEmpty.visibility = View.VISIBLE
            recyclerViewTransactions.visibility = View.GONE
        } else {
            textViewEmpty.visibility = View.GONE
            recyclerViewTransactions.visibility = View.VISIBLE
            transactionAdapter.updateItems(filteredTransactions)
        }
    }

    private fun editTransaction(transaction: Transaction) {
        val intent = Intent(requireContext(), AddEditTransactionActivity::class.java)
        intent.putExtra("transaction_id", transaction.id)
        startActivity(intent)
    }

    private fun showDeleteTransactionDialog(transaction: Transaction) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete '${transaction.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTransaction(transaction)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction(transaction: Transaction) {
        val updatedTransactions = transactions.filter { it.id != transaction.id }
        PrefsManager.saveTransactions(updatedTransactions)
        
        // Refresh the data
        loadTransactions()
        
        // Show confirmation
        Snackbar.make(requireView(), "Transaction deleted", Snackbar.LENGTH_SHORT).show()
    }

    inner class TransactionAdapter(
        private var items: List<Transaction>,
        private val onClick: (Transaction) -> Unit,
        private val onLongClick: (Transaction) -> Boolean
    ) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {
        
        fun updateItems(newItems: List<Transaction>) {
            this.items = newItems
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val transaction = items[position]
            holder.bind(transaction)
        }
        
        override fun getItemCount() = items.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textTitle: TextView = itemView.findViewById(R.id.textTransactionTitle)
            private val textCategory: TextView = itemView.findViewById(R.id.textTransactionCategory)
            private val textCategoryIcon: TextView = itemView.findViewById(R.id.textCategoryIcon)
            private val textDate: TextView = itemView.findViewById(R.id.textTransactionDate)
            private val textAmount: TextView = itemView.findViewById(R.id.textTransactionAmount)
            
            fun bind(transaction: Transaction) {
                textTitle.text = transaction.title
                
                // Find category to get emoji
                val categories = PrefsManager.loadCategories()
                val category = categories.find { it.name == transaction.category }
                
                // Display category emoji in the circle icon
                if (category != null && category.emoji != null && category.emoji.isNotEmpty()) {
                    textCategoryIcon.text = category.emoji
                    textCategory.text = category.name
                } else {
                    // Default icon if no emoji available
                    textCategoryIcon.text = "💰"
                    textCategory.text = transaction.category
                }
                
                // Format date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                textDate.text = dateFormat.format(Date(transaction.date))
                
                // Format amount
                val currency = PrefsManager.getCurrency()
                val formattedAmount = CurrencyFormatter.formatCurrency(transaction.amount, currency)
                
                if (transaction.type == TxType.EXPENSE) {
                    textAmount.text = "-$formattedAmount"
                    textAmount.setTextColor(resources.getColor(R.color.expense_red, null))
                } else {
                    textAmount.text = formattedAmount
                    textAmount.setTextColor(resources.getColor(R.color.income_green, null))
                }
                
                // Add ripple effect on item click
                itemView.setOnClickListener { 
                    // Apply visual feedback
                    itemView.isPressed = true
                    itemView.postDelayed({
                        itemView.isPressed = false
                        onClick(transaction)
                    }, 50)
                }
                
                itemView.setOnLongClickListener { onLongClick(transaction) }
            }
        }
    }
} 