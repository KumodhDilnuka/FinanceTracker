package com.example.financetracker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.financetracker.model.Budget
import com.example.financetracker.model.CategoryWithAmount
import com.example.financetracker.model.Transaction
import com.example.financetracker.model.TxType
import com.example.financetracker.util.PrefsManager

class HomeViewModel : ViewModel() {
    
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions
    
    private val _expenseCategories = MutableLiveData<List<CategoryWithAmount>>()
    val expenseCategories: LiveData<List<CategoryWithAmount>> = _expenseCategories
    
    private val _incomeCategories = MutableLiveData<List<CategoryWithAmount>>()
    val incomeCategories: LiveData<List<CategoryWithAmount>> = _incomeCategories
    
    private val _budget = MutableLiveData<Budget>()
    val budget: LiveData<Budget> = _budget
    
    fun loadData() {
        val allTransactions = PrefsManager.loadTransactions()
        _transactions.value = allTransactions
        
        // Update expense categories
        val expensesByCategory = allTransactions
            .filter { it.type == TxType.EXPENSE }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .map { CategoryWithAmount(it.key, it.value) }
            .sortedByDescending { it.amount }
            
        _expenseCategories.value = expensesByCategory
        
        // Update income categories
        val incomeByCategory = allTransactions
            .filter { it.type == TxType.INCOME }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .map { CategoryWithAmount(it.key, it.value) }
            .sortedByDescending { it.amount }
            
        _incomeCategories.value = incomeByCategory
        
        // Update budget
        _budget.value = PrefsManager.getBudget()
    }
} 