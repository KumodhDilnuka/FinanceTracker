package com.example.financetracker.ui

import android.content.Intent
import android.content.res.ColorStateList
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    // ViewModel for handling data operations
    private val viewModel: HomeViewModel by viewModels()
    
    // Adapters for RecyclerViews
    private lateinit var transactionAdapter: GroupedTransactionAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    
    // Charts for displaying financial data
    private lateinit var expensePieChart: PieChart
    private lateinit var incomePieChart: PieChart
    
    private lateinit var recyclerViewTransactions: RecyclerView
    private lateinit var recyclerViewCategories: RecyclerView
    private lateinit var textTotalIncome: TextView
    private lateinit var textTotalExpense: TextView
    private lateinit var textBalance: TextView
    private lateinit var textBalanceDate: TextView
    private lateinit var textBudgetInfo: TextView
    private lateinit var textBudgetPercentage: TextView
    private lateinit var progressBudget: ProgressBar
    private lateinit var textViewEmptyTransactions: TextView
    private lateinit var textViewSeeAllTransactions: TextView
    
    // Time range selectors
    private lateinit var chipMonth: MaterialButton
    private lateinit var chipYear: MaterialButton
    private lateinit var chipAllTime: MaterialButton
    
    private var selectedTimeRange = TimeRange.MONTH
    
    private var transactions = listOf<Transaction>()
    
    // Enum for time range options
    enum class TimeRange {
        MONTH, YEAR, ALL_TIME
    }
    
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
        initUI()
        setupCharts()
        setupRecyclerViews()
        setupFAB()
        setupSeeAllButton()
        
        // For debugging only - long press on the balance card will add test transactions
        binding.root.findViewById<androidx.cardview.widget.CardView>(R.id.balanceCard)?.setOnLongClickListener {
            addTestTransactions()
            loadData() // Reload the data immediately
            android.util.Log.d("HomeFragment", "Added test transactions")
            true
        }
        
        // Observe ViewModel LiveData
        observeViewModel()
    }
    
    override fun onResume() {
        super.onResume()
        loadData() // Refresh data when returning to this fragment
    }
    
    private fun initUI() {
        // Get references to views
        recyclerViewTransactions = binding.recyclerViewTransactions
        recyclerViewCategories = binding.recyclerViewCategories
        textTotalIncome = binding.textTotalIncome
        textTotalExpense = binding.textTotalExpense
        textBalance = binding.textBalance
        textBalanceDate = binding.textBalanceDate
        textBudgetInfo = binding.textBudgetInfo
        textBudgetPercentage = binding.textBudgetPercentage
        progressBudget = binding.progressBudget
        textViewEmptyTransactions = binding.textViewEmptyTransactions
        textViewSeeAllTransactions = binding.textViewSeeAllTransactions
        
        // Init time range buttons
        try {
            chipMonth = binding.chipMonth
            chipYear = binding.chipYear
            chipAllTime = binding.chipAllTime
            
            // Set initial date in balance summary
            updateBalanceDateText()
            
            // Setup time range selector listeners
            setupTimeRangeSelectors()
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "Error initializing time range buttons: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun updateBalanceDateText() {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        textBalanceDate.text = "As of " + dateFormat.format(Date())
    }
    
    private fun setupTimeRangeSelectors() {
        // Set up click listeners for time range buttons
        chipMonth.setOnClickListener {
            selectTab(TimeRange.MONTH)
        }
        
        chipYear.setOnClickListener {
            selectTab(TimeRange.YEAR)
        }
        
        chipAllTime.setOnClickListener {
            selectTab(TimeRange.ALL_TIME)
        }
        
        // Set initial selection to Month
        selectTab(TimeRange.MONTH)
    }
    
    private fun selectTab(timeRange: TimeRange) {
        // Reset all tabs
        val tabs = listOf(chipMonth, chipYear, chipAllTime)
        tabs.forEach { tab ->
            tab.setTextColor(resources.getColor(R.color.black, null))
            tab.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1EEFF"))
            tab.setStrokeColorResource(android.R.color.transparent)
            tab.strokeWidth = 0
            tab.icon = null
        }
        
        // Set selected tab based on filter
        val selectedTab = when (timeRange) {
            TimeRange.MONTH -> chipMonth
            TimeRange.YEAR -> chipYear
            TimeRange.ALL_TIME -> chipAllTime
        }
        
        // Set selected tab properties
        selectedTab.setTextColor(resources.getColor(R.color.white, null))
        selectedTab.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.primary, null))
        
        // Update selected time range and refresh data
        if (selectedTimeRange != timeRange) {
            selectedTimeRange = timeRange
            updateDataForTimeRange()
        }
    }
    
    private fun updateDataForTimeRange() {
        android.util.Log.d("HomeFragment", "Updating data for time range: $selectedTimeRange")
        // Filter transactions based on selected time range
        val filteredTransactions = filterTransactionsByTimeRange(transactions, selectedTimeRange)
        
        // Update summary data with filtered transactions
        updateSummaryData(filteredTransactions)
        
        // Update UI elements
        if (filteredTransactions.isEmpty()) {
            textViewEmptyTransactions.visibility = View.VISIBLE
            recyclerViewTransactions.visibility = View.GONE
        } else {
            textViewEmptyTransactions.visibility = View.GONE
            recyclerViewTransactions.visibility = View.VISIBLE
            
            // Group transactions by date and take only the most recent 10
            val groupedTransactions = groupTransactionsByDate(filteredTransactions.take(10))
            transactionAdapter.submitList(groupedTransactions)
        }
        
        // Update category summaries and charts with filtered transactions
        updateCategorySummary(filteredTransactions)
        updatePieCharts(filteredTransactions)
        
        // Log the filtered transactions for debugging
        android.util.Log.d("HomeFragment", "Time range: $selectedTimeRange, Filtered transactions: ${filteredTransactions.size}")
    }
    
    private fun filterTransactionsByTimeRange(allTransactions: List<Transaction>, timeRange: TimeRange): List<Transaction> {
        // Get current date and time
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        
        // Debug log current date info
        android.util.Log.d("HomeFragment", "Current date: ${Date()}, Year: $currentYear, Month: $currentMonth")
        
        return when (timeRange) {
            TimeRange.MONTH -> {
                // Month: Current calendar month (1st to 31st)
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val filteredList = allTransactions.filter { transaction ->
                    val transactionCal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                    transactionCal.get(Calendar.MONTH) == currentMonth && 
                    transactionCal.get(Calendar.YEAR) == currentYear
                }
                
                android.util.Log.d("HomeFragment", "MONTH filter: Found ${filteredList.size} transactions in current month (${getMonthName(currentMonth)})")
                filteredList
            }
            
            TimeRange.YEAR -> {
                // Year: Current calendar year
                val startOfYear = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val filteredList = allTransactions.filter { transaction ->
                    val transactionCal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                    transactionCal.get(Calendar.YEAR) == currentYear
                }
                
                android.util.Log.d("HomeFragment", "YEAR filter: Found ${filteredList.size} transactions in year $currentYear")
                filteredList
            }
            
            TimeRange.ALL_TIME -> {
                // All time: no filtering
                android.util.Log.d("HomeFragment", "ALL_TIME filter: Returning all ${allTransactions.size} transactions")
                allTransactions
            }
        }
    }
    
    private fun getMonthName(month: Int): String {
        return when (month) {
            Calendar.JANUARY -> "January"
            Calendar.FEBRUARY -> "February"
            Calendar.MARCH -> "March"
            Calendar.APRIL -> "April"
            Calendar.MAY -> "May"
            Calendar.JUNE -> "June"
            Calendar.JULY -> "July"
            Calendar.AUGUST -> "August"
            Calendar.SEPTEMBER -> "September"
            Calendar.OCTOBER -> "October"
            Calendar.NOVEMBER -> "November"
            Calendar.DECEMBER -> "December"
            else -> "Unknown"
        }
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
    
    private fun setupRecyclerViews() {
        // Initialize transactions RecyclerView with a LinearLayoutManager
        recyclerViewTransactions.layoutManager = LinearLayoutManager(requireContext())
        
        // Create and set the transaction adapter
        transactionAdapter = GroupedTransactionAdapter(
            emptyList(),
            { transaction -> navigateToEditTransaction(transaction) },
            { transaction -> showDeleteTransactionDialog(transaction); true }
        )
        recyclerViewTransactions.adapter = transactionAdapter
        
        // Initialize categories RecyclerView
        recyclerViewCategories.layoutManager = LinearLayoutManager(requireContext())
        categoryAdapter = CategoryAdapter(emptyList())
        recyclerViewCategories.adapter = categoryAdapter
    }
    
    private fun setupFAB() {
        binding.fabAddTransaction.setOnClickListener {
            val intent = Intent(requireContext(), AddEditTransactionActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupSeeAllButton() {
        textViewSeeAllTransactions.setOnClickListener {
            // Navigate to Transactions tab
            (activity as? MainActivity)?.let {
                // Use navigation method or directly select the appropriate tab
                it.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.navigation_transactions
            }
        }
    }
    
    private fun observeViewModel() {
        // Relevant LiveData observations
    }
    
    private fun loadData() {
        android.util.Log.d("HomeFragment", "Loading data...")
        transactions = PrefsManager.loadTransactions().sortedByDescending { it.date }
        android.util.Log.d("HomeFragment", "Loaded ${transactions.size} transactions")
        
        // Update data based on the current time range selection
        updateDataForTimeRange()
        
        // Update other UI that doesn't depend on time range
        updateBudgetUI()
        
        // Check budget status - only for current month expenses
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
        val budget = PrefsManager.getBudget()
        
        android.util.Log.d("HomeFragment", "Checking budget notification with monthly expenses only: $monthlyExpenses / $budget")
        NotificationHelper.checkBudgetStatus(requireContext(), monthlyExpenses, budget)
    }
    
    private fun updateSummaryData(filteredTransactions: List<Transaction>) {
        val totalIncome = filteredTransactions
            .filter { it.type == TxType.INCOME }
            .sumOf { it.amount }
        
        val totalExpenses = filteredTransactions
            .filter { it.type == TxType.EXPENSE }
            .sumOf { it.amount }
        
        val balance = totalIncome - totalExpenses
        
        // Debug log to check values
        android.util.Log.d("HomeFragment", "SummaryData - Income: $totalIncome, Expenses: $totalExpenses, Balance: $balance")
        
        try {
            // Use formatCurrency with currency code
            val currency = PrefsManager.getCurrency().ifEmpty { "USD" } // Default to USD if not set
            textTotalIncome.text = CurrencyFormatter.formatCurrency(totalIncome, currency)
            textTotalExpense.text = CurrencyFormatter.formatCurrency(totalExpenses, currency)
            textBalance.text = CurrencyFormatter.formatCurrency(balance, currency)
            
            // Also log what we're setting to ensure UI is updated
            android.util.Log.d("HomeFragment", "Setting UI text - Income: ${textTotalIncome.text}, Expenses: ${textTotalExpense.text}, Balance: ${textBalance.text}")
        } catch (e: Exception) {
            // If formatter fails, use simple string concatenation
            android.util.Log.e("HomeFragment", "Error formatting currency: ${e.message}")
            textTotalIncome.text = "$${String.format("%.2f", totalIncome)}"
            textTotalExpense.text = "$${String.format("%.2f", totalExpenses)}"
            textBalance.text = "$${String.format("%.2f", balance)}"
        }
    }
    
    private fun updateBudgetUI() {
        val budget = PrefsManager.getBudget()
        if (budget <= 0) {
            textBudgetInfo.text = "No budget set"
            textBudgetPercentage.text = "0%"
            progressBudget.progress = 0
            return
        }
        
        // Calculate monthly expenses - only for current month
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
        
        // Debug log to check values
        android.util.Log.d("HomeFragment", "Budget: $budget, Monthly Expenses (current month only): $monthlyExpenses")
        
        // Use formatCurrency with currency code
        val currency = PrefsManager.getCurrency()
        val budgetInfoText = CurrencyFormatter.formatCurrency(monthlyExpenses, currency) + " / " + 
                             CurrencyFormatter.formatCurrency(budget, currency)
        textBudgetInfo.text = budgetInfoText
        
        // Calculate percentage with explicit floating-point division
        val actualPercentage = (monthlyExpenses * 100.0 / budget).toInt()
        
        // Cap progress bar at 100% but display actual percentage in text
        val progressValue = actualPercentage.coerceIn(0, 100)
        
        // Debug log the percentage calculation
        android.util.Log.d("HomeFragment", "Percentage calculation: ($monthlyExpenses * 100.0) / $budget = $actualPercentage%")
        
        // Update UI with appropriate colors for over-budget
        textBudgetPercentage.text = actualPercentage.toString() + "%"
        
        // Set the text color to red if over budget
        if (actualPercentage > 100) {
            textBudgetPercentage.setTextColor(resources.getColor(R.color.expense_red, null))
        } else {
            textBudgetPercentage.setTextColor(resources.getColor(R.color.primary, null))
        }
        
        // Progress bar is still capped at 100%
        progressBudget.progress = progressValue
    }
    
    private fun updateCategorySummary(filteredTransactions: List<Transaction> = transactions) {
        // Get categories with their total amounts
        val expensesByCategory = filteredTransactions
            .filter { it.type == TxType.EXPENSE }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
        
        // Get categories data
        val categories = PrefsManager.loadCategories()
        
        // Calculate total expenses for percentage
        val totalExpenses = expensesByCategory.sumOf { it.second }
        
        android.util.Log.d("HomeFragment", "Total expenses for time range: $totalExpenses, Categories: ${expensesByCategory.size}")
        
        // If no expenses, show empty
        if (totalExpenses <= 0) {
            categoryAdapter.submitList(emptyList())
            return
        }
        
        // Create spending items with percentage
        val spendingItems = expensesByCategory.map { (categoryName, amount) ->
            val emoji = categories.find { it.name == categoryName }?.emoji ?: ""
            // Calculate percentage correctly with floating-point math
            val percentage = ((amount * 100.0) / totalExpenses).toInt()
            
            android.util.Log.d("HomeFragment", "Category: $categoryName, Amount: $amount, Percentage: $percentage%")
            
            CategorySpendingItem(
                category = categoryName,
                emoji = emoji,
                amount = amount,
                percentage = percentage,
                currency = PrefsManager.getCurrency()
            )
        }
        
        // Submit to the adapter
        categoryAdapter.submitList(spendingItems)
    }
    
    private fun updatePieCharts(filteredTransactions: List<Transaction> = transactions) {
        // Update expense pie chart
        val expensesByCategory = filteredTransactions
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
        
        android.util.Log.d("HomeFragment", "Expense Categories: ${expenseCategoriesWithAmount.size}")
        updateExpensePieChart(expenseCategoriesWithAmount)
        
        // Update income pie chart
        val incomeByCategory = filteredTransactions
            .filter { it.type == TxType.INCOME }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
        
        val incomeCategoriesWithAmount = incomeByCategory.map { (categoryName, amount) ->
            val emoji = categories.find { it.name == categoryName }?.emoji ?: ""
            CategoryWithAmount(categoryName, amount, emoji)
        }
        
        android.util.Log.d("HomeFragment", "Income Categories: ${incomeCategoriesWithAmount.size}")
        
        // Debug logging for income data
        incomeCategoriesWithAmount.forEach { category ->
            android.util.Log.d("HomeFragment", "Income Category: ${category.name}, Amount: ${category.amount}")
        }
        
        updateIncomePieChart(incomeCategoriesWithAmount)
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
        android.util.Log.d("HomeFragment", "Updating income pie chart with ${categories.size} categories")
        
        if (categories.isEmpty()) {
            incomePieChart.setNoDataText("No income data available")
            incomePieChart.invalidate()
            android.util.Log.d("HomeFragment", "No income data available, cleared chart")
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
            android.util.Log.d("HomeFragment", "Added income entry: ${category.name} - ${category.amount}")
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
        
        // Set data to chart and refresh
        val data = PieData(dataSet)
        incomePieChart.data = data
        incomePieChart.highlightValues(null) // Clear any existing highlights
        incomePieChart.notifyDataSetChanged() // Notify chart that data has changed
        incomePieChart.invalidate() // Refresh the chart
        
        android.util.Log.d("HomeFragment", "Income pie chart updated with ${entries.size} entries")
    }
    
    // For transaction list grouping
    sealed class TransactionListItem {
        data class DateHeader(
            val date: LocalDate,
            val label: String,
            val dayTotal: Double
        ) : TransactionListItem()
        
        data class TransactionItem(val transaction: Transaction) : TransactionListItem()
    }
    
    // Adapter for transactions list with date headers
    inner class GroupedTransactionAdapter(
        private var items: List<TransactionListItem>,
        private val onClick: (Transaction) -> Unit,
        private val onLongClick: (Transaction) -> Boolean
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        
        private val VIEW_TYPE_HEADER = 0
        private val VIEW_TYPE_TRANSACTION = 1
        
        fun submitList(newItems: List<TransactionListItem>) {
            this.items = newItems
            notifyDataSetChanged()
        }
        
        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is TransactionListItem.DateHeader -> VIEW_TYPE_HEADER
                is TransactionListItem.TransactionItem -> VIEW_TYPE_TRANSACTION
            }
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_HEADER -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_date_header, parent, false)
                    DateHeaderViewHolder(view)
                }
                VIEW_TYPE_TRANSACTION -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_transaction, parent, false)
                    TransactionViewHolder(view)
                }
                else -> throw IllegalArgumentException("Unknown view type: " + viewType)
            }
        }
        
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is TransactionListItem.DateHeader -> {
                    (holder as DateHeaderViewHolder).bind(item)
                }
                is TransactionListItem.TransactionItem -> {
                    (holder as TransactionViewHolder).bind(item.transaction)
                }
            }
        }
        
        override fun getItemCount() = items.size
        
        inner class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textDate: TextView = itemView.findViewById(R.id.textDate)
            private val textDayTotal: TextView = itemView.findViewById(R.id.textDayTotal)
            
            fun bind(header: TransactionListItem.DateHeader) {
                textDate.text = header.label
                
                // Format day total with currency code
                val currency = PrefsManager.getCurrency()
                textDayTotal.text = CurrencyFormatter.formatCurrency(header.dayTotal, currency)
                
                // Set color based on amount (positive or negative)
                val textColor = if (header.dayTotal >= 0) {
                    resources.getColor(R.color.income_green, null)
                } else {
                    resources.getColor(R.color.expense_red, null)
                }
                textDayTotal.setTextColor(textColor)
            }
        }
        
        inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
                val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                textDate.text = dateFormat.format(Date(transaction.date))
                
                // Format amount using formatCurrency with currency code
                val currency = PrefsManager.getCurrency()
                val formattedAmount = CurrencyFormatter.formatCurrency(transaction.amount, currency)
                
                if (transaction.type == TxType.EXPENSE) {
                    textAmount.text = "-" + formattedAmount
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
                    textCategory.text = item.emoji + " " + item.category
                } else {
                    textCategory.text = item.category
                }
                val currency = PrefsManager.getCurrency()
                textAmount.text = CurrencyFormatter.formatCurrency(item.amount, currency)
                progressBar.progress = item.percentage
            }
        }
    }
    
    private fun navigateToEditTransaction(transaction: Transaction) {
        val intent = Intent(requireContext(), AddEditTransactionActivity::class.java)
        intent.putExtra("transaction_id", transaction.id)
        startActivity(intent)
    }
    
    private fun showDeleteTransactionDialog(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete '" + transaction.title + "'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTransaction(transaction)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteTransaction(transaction: Transaction) {
        // Get all transactions
        val allTransactions = PrefsManager.loadTransactions()
        
        // Filter out the transaction to delete
        val updatedTransactions = allTransactions.filter { it.id != transaction.id }
        
        // Save updated list
        PrefsManager.saveTransactions(updatedTransactions)
        
        // Refresh the data
        loadData()
        
        // Show confirmation
        Snackbar.make(requireView(), "Transaction deleted", Snackbar.LENGTH_SHORT).show()
    }
    
    private fun groupTransactionsByDate(transactions: List<Transaction>): List<TransactionListItem> {
        val result = mutableListOf<TransactionListItem>()
        val today = LocalDate.now()
        val yesterday = today.minus(1, ChronoUnit.DAYS)
        
        // Group by date
        val groupedByDate = transactions.groupBy { transaction ->
            val date = Date(transaction.date).toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            date
        }
        
        // Process each date group
        groupedByDate.entries.sortedByDescending { it.key }.forEach { (date, transactionsInDay) ->
            // Add header with appropriate label
            val headerLabel = when {
                date.isEqual(today) -> "Today"
                date.isEqual(yesterday) -> "Yesterday"
                date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMMM d"))
                else -> date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
            }
            
            // Calculate daily total
            val dailyTotal = transactionsInDay.sumOf { 
                if (it.type == TxType.EXPENSE) -it.amount else it.amount 
            }
            
            // Add header
            result.add(TransactionListItem.DateHeader(
                date = date,
                label = headerLabel,
                dayTotal = dailyTotal
            ))
            
            // Add transactions for the day
            transactionsInDay.forEach { transaction ->
                result.add(TransactionListItem.TransactionItem(transaction))
            }
        }
        
        return result
    }
    
    // For debugging purposes only
    private fun addTestTransactions() {
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        
        // Create a list of test transactions
        val testTransactions = mutableListOf<Transaction>()
        
        // Today
        testTransactions.add(
            Transaction(
                title = "Today Income",
                amount = 100.0,
                category = "Salary",
                type = TxType.INCOME,
                date = today,
                note = "Test transaction"
            )
        )
        
        testTransactions.add(
            Transaction(
                title = "Today Expense",
                amount = 50.0,
                category = "Food",
                type = TxType.EXPENSE,
                date = today,
                note = "Test transaction"
            )
        )
        
        // This week (3 days ago)
        calendar.add(Calendar.DAY_OF_YEAR, -3)
        val thisWeek = calendar.timeInMillis
        
        testTransactions.add(
            Transaction(
                title = "Week Income",
                amount = 200.0,
                category = "Salary",
                type = TxType.INCOME,
                date = thisWeek,
                note = "Test transaction"
            )
        )
        
        testTransactions.add(
            Transaction(
                title = "Week Expense",
                amount = 75.0,
                category = "Entertainment",
                type = TxType.EXPENSE,
                date = thisWeek,
                note = "Test transaction"
            )
        )
        
        // This month (15 days ago)
        calendar.setTimeInMillis(today)
        calendar.add(Calendar.DAY_OF_YEAR, -15)
        val thisMonth = calendar.timeInMillis
        
        testTransactions.add(
            Transaction(
                title = "Month Income",
                amount = 500.0,
                category = "Salary",
                type = TxType.INCOME,
                date = thisMonth,
                note = "Test transaction"
            )
        )
        
        testTransactions.add(
            Transaction(
                title = "Month Expense",
                amount = 150.0,
                category = "Housing",
                type = TxType.EXPENSE,
                date = thisMonth,
                note = "Test transaction"
            )
        )
        
        // This year (6 months ago)
        calendar.setTimeInMillis(today)
        calendar.add(Calendar.MONTH, -6)
        val thisYear = calendar.timeInMillis
        
        testTransactions.add(
            Transaction(
                title = "Year Income",
                amount = 1000.0,
                category = "Gifts",
                type = TxType.INCOME,
                date = thisYear,
                note = "Test transaction"
            )
        )
        
        testTransactions.add(
            Transaction(
                title = "Year Expense",
                amount = 300.0,
                category = "Healthcare",
                type = TxType.EXPENSE,
                date = thisYear,
                note = "Test transaction"
            )
        )
        
        // Save the test transactions
        val existingTransactions = PrefsManager.loadTransactions().toMutableList()
        existingTransactions.addAll(testTransactions)
        PrefsManager.saveTransactions(existingTransactions)
        
        // Show success toast
        android.widget.Toast.makeText(
            requireContext(),
            "Added ${testTransactions.size} test transactions",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 