package com.example.financetracker.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.financetracker.R
import com.example.financetracker.databinding.FragmentHomeBinding
import com.example.financetracker.model.Budget
import com.example.financetracker.model.CategoryWithAmount
import com.example.financetracker.model.Transaction
import com.example.financetracker.model.TxType
import com.example.financetracker.util.CurrencyFormatter
import com.example.financetracker.util.DateUtils
import com.example.financetracker.util.NotificationHelper
import com.example.financetracker.util.PrefsManager
import com.example.financetracker.viewmodel.HomeViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.*
import java.text.SimpleDateFormat

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    // ViewModel for handling data operations
    private val viewModel: HomeViewModel by viewModels()
    
    // Adapters for RecyclerViews
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    
    // Charts for displaying financial data
    private lateinit var expensePieChart: PieChart
    private lateinit var incomePieChart: PieChart
    
    private lateinit var recyclerViewTransactions: RecyclerView
    private lateinit var recyclerViewCategories: RecyclerView
    private lateinit var textTotalIncome: TextView
    private lateinit var textTotalExpense: TextView
    private lateinit var textBalance: TextView
    private lateinit var textBudgetInfo: TextView
    private lateinit var textBudgetPercentage: TextView
    private lateinit var progressBudget: ProgressBar
    
    private var transactions = listOf<Transaction>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI components
        setupCharts()
        setupRecyclerViews()
        setupFAB()
        
        // Observe ViewModel LiveData
        observeViewModel()
    }
    
    override fun onResume() {
        super.onResume()
        loadData() // Refresh data when returning to this fragment
    }
    
    private fun setupCharts() {
        // Initialize expense pie chart
        expensePieChart = binding.pieChartExpenses
        setupPieChart(expensePieChart, "Expenses")
        
        // Initialize income pie chart
        incomePieChart = binding.pieChartIncome
        setupPieChart(incomePieChart, "Income")
    }
    
    private fun setupPieChart(chart: PieChart, label: String) {
        // Set general chart properties
        chart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            legend.isEnabled = true
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            isDrawHoleEnabled = true
            holeRadius = 58f
            setHoleColor(Color.WHITE)
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = label
            setCenterTextSize(16f)
            setCenterTextColor(Color.DKGRAY)
            animateY(1000, Easing.EaseInOutQuad)
        }
    }
    
    private fun updateExpensePieChart(categories: List<CategoryWithAmount>) {
        if (categories.isEmpty()) {
            expensePieChart.setNoDataText("No expense data available")
            expensePieChart.invalidate()
            return
        }
        
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        val colorScheme = getExpenseColorScheme(categories.size)
        
        // Create entries for each category
        for (i in categories.indices) {
            val category = categories[i]
            entries.add(PieEntry(category.amount.toFloat(), category.name))
            colors.add(colorScheme[i % colorScheme.size])
        }
        
        // Create dataset and set it to chart
        val dataSet = PieDataSet(entries, "Expense Categories")
        dataSet.apply {
            this.colors = colors
            valueTextSize = 14f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(expensePieChart)
            sliceSpace = 3f
            selectionShift = 5f
        }
        
        // Set data to chart
        val data = PieData(dataSet)
        expensePieChart.data = data
        expensePieChart.invalidate()
    }
    
    private fun updateIncomePieChart(categories: List<CategoryWithAmount>) {
        if (categories.isEmpty()) {
            incomePieChart.setNoDataText("No income data available")
            incomePieChart.invalidate()
            return
        }
        
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        val colorScheme = getIncomeColorScheme(categories.size)
        
        // Create entries for each category
        for (i in categories.indices) {
            val category = categories[i]
            entries.add(PieEntry(category.amount.toFloat(), category.name))
            colors.add(colorScheme[i % colorScheme.size])
        }
        
        // Create dataset and set it to chart
        val dataSet = PieDataSet(entries, "Income Categories")
        dataSet.apply {
            this.colors = colors
            valueTextSize = 14f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(incomePieChart)
            sliceSpace = 3f
            selectionShift = 5f
        }
        
        // Set data to chart
        val data = PieData(dataSet)
        incomePieChart.data = data
        incomePieChart.invalidate()
    }
    
    // Get a color scheme for expense categories
    private fun getExpenseColorScheme(size: Int): List<Int> {
        val baseColor = resources.getColor(R.color.expense_red, null)
        val baseColorInt = baseColor
        
        // Create variations of the base color
        val colorList = mutableListOf<Int>()
        colorList.add(baseColorInt)
        
        // Add darker and lighter variations
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColorInt, hsv)
        
        for (i in 1 until size) {
            // Alternate between lighter and darker
            if (i % 2 == 0) {
                hsv[1] = Math.max(0f, hsv[1] - 0.1f * (i / 2)) // Reduce saturation
                hsv[2] = Math.min(1f, hsv[2] + 0.1f * (i / 2)) // Increase brightness
            } else {
                hsv[1] = Math.min(1f, hsv[1] + 0.1f * ((i + 1) / 2)) // Increase saturation
                hsv[2] = Math.max(0f, hsv[2] - 0.1f * ((i + 1) / 2)) // Reduce brightness
            }
            
            colorList.add(Color.HSVToColor(hsv))
        }
        
        return colorList
    }
    
    // Get a color scheme for income categories with greener tones
    private fun getIncomeColorScheme(size: Int): List<Int> {
        val baseColor = resources.getColor(R.color.income_green, null)
        val baseColorInt = baseColor
        
        // Create variations of the base color
        val colorList = mutableListOf<Int>()
        colorList.add(baseColorInt)
        
        // Add darker and lighter variations
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColorInt, hsv)
        
        for (i in 1 until size) {
            // Alternate between lighter and darker
            if (i % 2 == 0) {
                hsv[1] = Math.max(0f, hsv[1] - 0.1f * (i / 2)) // Reduce saturation
                hsv[2] = Math.min(1f, hsv[2] + 0.1f * (i / 2)) // Increase brightness
            } else {
                hsv[1] = Math.min(1f, hsv[1] + 0.1f * ((i + 1) / 2)) // Increase saturation
                hsv[2] = Math.max(0f, hsv[2] - 0.1f * ((i + 1) / 2)) // Reduce brightness
            }
            
            colorList.add(Color.HSVToColor(hsv))
        }
        
        return colorList
    }

    private fun setupRecyclerViews() {
        // Initialize TextViews
        textTotalIncome = binding.textTotalIncome
        textTotalExpense = binding.textTotalExpense
        textBalance = binding.textBalance
        textBudgetInfo = binding.textBudgetInfo
        textBudgetPercentage = binding.textBudgetPercentage
        progressBudget = binding.progressBudget
        
        // Initialize RecyclerViews
        recyclerViewTransactions = binding.recyclerViewTransactions
        recyclerViewTransactions.layoutManager = LinearLayoutManager(requireContext())
        transactionAdapter = TransactionAdapter(
            emptyList(),
            // Click listener
            onClick = { transaction ->
                val intent = Intent(requireContext(), AddEditTransactionActivity::class.java)
                intent.putExtra("transaction_id", transaction.id)
                startActivity(intent)
            },
            // Long click listener for delete
            onLongClick = { transaction ->
                showDeleteTransactionDialog(transaction)
                true
            }
        )
        recyclerViewTransactions.adapter = transactionAdapter
        
        recyclerViewCategories = binding.recyclerViewCategories
        recyclerViewCategories.layoutManager = LinearLayoutManager(requireContext())
        categoryAdapter = CategoryAdapter(emptyList())
        recyclerViewCategories.adapter = categoryAdapter
    }
    
    private fun setupFAB() {
        val fab: FloatingActionButton = binding.fabAddTransaction
        fab.setOnClickListener {
            val intent = Intent(requireContext(), AddEditTransactionActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun observeViewModel() {
        // Observe transactions
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions)
            updateSummaryData(transactions)
        }
        
        // Observe expense categories
        viewModel.expenseCategories.observe(viewLifecycleOwner) { categories ->
            // Convert CategoryWithAmount to CategorySpendingItem
            val currency = PrefsManager.getCurrency()
            val totalExpense = categories.sumOf { it.amount }
            
            val categoryItems = categories.map { category ->
                val percentage = if (totalExpense > 0) ((category.amount / totalExpense) * 100).toInt() else 0
                // Use the emoji from CategoryWithAmount
                CategorySpendingItem(category.name, category.emoji, category.amount, percentage, currency)
            }
            
            categoryAdapter.submitList(categoryItems)
            updateExpensePieChart(categories)
        }
        
        // Observe income categories
        viewModel.incomeCategories.observe(viewLifecycleOwner) { categories ->
            updateIncomePieChart(categories)
        }
        
        // Observe budget info
        viewModel.budget.observe(viewLifecycleOwner) { budget ->
            updateBudgetUI(budget)
        }
    }
    
    private fun updateSummaryData(transactions: List<Transaction>) {
        val currency = PrefsManager.getCurrency()
        
        val totalIncome = transactions
            .filter { it.type == TxType.INCOME }
            .sumOf { it.amount }
            
        val totalExpense = transactions
            .filter { it.type == TxType.EXPENSE }
            .sumOf { it.amount }
            
        val balance = totalIncome - totalExpense
        
        textTotalIncome.text = CurrencyFormatter.formatCurrency(totalIncome, currency)
        textTotalExpense.text = CurrencyFormatter.formatCurrency(totalExpense, currency)
        textBalance.text = CurrencyFormatter.formatCurrency(balance, currency)
    }
    
    private fun updateBudgetUI(budget: Budget?) {
        val budget = PrefsManager.getBudget()
        val currency = PrefsManager.getCurrency()
        
        val totalExpense = transactions
            .filter { it.type == TxType.EXPENSE }
            .sumOf { it.amount }
            
        if (budget <= 0) {
            textBudgetInfo.text = "No monthly budget set"
            textBudgetPercentage.text = "N/A"
            progressBudget.progress = 0
            return
        }
        
        val percentage = ((totalExpense / budget) * 100).toInt()
        textBudgetInfo.text = "${CurrencyFormatter.formatCurrency(totalExpense, currency)} / ${CurrencyFormatter.formatCurrency(budget, currency)} (Monthly)"
        textBudgetPercentage.text = "$percentage%"
        progressBudget.progress = percentage.coerceAtMost(100)
    }
    
    private fun loadData() {
        transactions = PrefsManager.loadTransactions()
        
        updateSummaryData(transactions)
        updateBudgetUI(null)
        updateCategorySummary()
        updateTransactionsList()
        updatePieCharts()
        
        // Check budget status
        val totalExpenses = transactions
            .filter { it.type == TxType.EXPENSE }
            .sumOf { it.amount }
        val budget = PrefsManager.getBudget()
        
        NotificationHelper.checkBudgetStatus(requireContext(), totalExpenses, budget)
    }
    
    private fun updatePieCharts() {
        // Update expense pie chart
        val expensesByCategory = transactions
            .filter { it.type == TxType.EXPENSE }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
        
        val categories = PrefsManager.loadCategories()
        
        val expenseCategoriesWithAmount = expensesByCategory.map { (categoryName, amount) ->
            val emoji = categories.find { it.name == categoryName }?.emoji ?: ""
            CategoryWithAmount(categoryName, amount, emoji)
        }
        
        updateExpensePieChart(expenseCategoriesWithAmount)
        
        // Update income pie chart
        val incomeByCategory = transactions
            .filter { it.type == TxType.INCOME }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
        
        val incomeCategoriesWithAmount = incomeByCategory.map { (categoryName, amount) ->
            val emoji = categories.find { it.name == categoryName }?.emoji ?: ""
            CategoryWithAmount(categoryName, amount, emoji)
        }
        
        updateIncomePieChart(incomeCategoriesWithAmount)
    }
    
    private fun updateCategorySummary() {
        val expensesByCategory = transactions
            .filter { it.type == TxType.EXPENSE }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
        
        val totalExpense = expensesByCategory.sumOf { it.second }
        val currency = PrefsManager.getCurrency()
        val allCategories = PrefsManager.loadCategories()
        
        val categoryItems = expensesByCategory.map { (categoryName, amount) ->
            val percentage = if (totalExpense > 0) ((amount / totalExpense) * 100).toInt() else 0
            val emoji = allCategories.find { it.name == categoryName }?.emoji ?: ""
            CategorySpendingItem(categoryName, emoji, amount, percentage, currency)
        }
        
        categoryAdapter.submitList(categoryItems)
    }
    
    private fun updateTransactionsList() {
        // Show most recent transactions first
        val sortedTransactions = transactions.sortedByDescending { it.date }
        transactionAdapter.submitList(sortedTransactions)
    }
    
    private fun showDeleteTransactionDialog(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
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
        loadData()
        
        // Show confirmation
        Snackbar.make(requireView(), "Transaction deleted", Snackbar.LENGTH_SHORT).show()
    }
    
    // Adapter for transactions list
    inner class TransactionAdapter(
        private var items: List<Transaction>,
        private val onClick: (Transaction) -> Unit,
        private val onLongClick: (Transaction) -> Boolean
    ) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {
        
        fun submitList(newItems: List<Transaction>) {
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
            private val textDate: TextView = itemView.findViewById(R.id.textTransactionDate)
            private val textAmount: TextView = itemView.findViewById(R.id.textTransactionAmount)
            
            fun bind(transaction: Transaction) {
                textTitle.text = transaction.title
                
                // Find category to get emoji
                val categories = PrefsManager.loadCategories()
                val category = categories.find { it.name == transaction.category }
                
                // Display category with emoji if available
                if (category != null && category.emoji != null && category.emoji.isNotEmpty()) {
                    textCategory.text = "${category.emoji} ${transaction.category}"
                } else {
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
                
                itemView.setOnClickListener { onClick(transaction) }
                itemView.setOnLongClickListener { onLongClick(transaction) }
            }
        }
    }
    
    // Data class for category spending
    data class CategorySpendingItem(
        val category: String,
        val emoji: String,
        val amount: Double,
        val percentage: Int,
        val currency: String
    )
    
    // Adapter for category spending
    inner class CategoryAdapter(
        private var items: List<CategorySpendingItem>
    ) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
        
        fun submitList(newItems: List<CategorySpendingItem>) {
            this.items = newItems
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category_spending, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)
        }
        
        override fun getItemCount() = items.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textCategory: TextView = itemView.findViewById(R.id.textCategoryName)
            private val textAmount: TextView = itemView.findViewById(R.id.textCategoryAmount)
            private val progressBar: ProgressBar = itemView.findViewById(R.id.progressCategory)
            
            fun bind(item: CategorySpendingItem) {
                if (item.emoji != null && item.emoji.isNotEmpty()) {
                    textCategory.text = "${item.emoji} ${item.category}"
                } else {
                    textCategory.text = item.category
                }
                textAmount.text = CurrencyFormatter.formatCurrency(item.amount, item.currency)
                progressBar.progress = item.percentage
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 