package com.example.financetracker.ui

import android.content.Intent
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionsFragment : Fragment() {

    private lateinit var recyclerViewTransactions: RecyclerView
    private lateinit var textViewEmpty: TextView
    private lateinit var transactionAdapter: TransactionAdapter
    private var transactions = listOf<Transaction>()

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
        
        // Initialize the adapter with a LinearLayoutManager
        recyclerViewTransactions.layoutManager = LinearLayoutManager(requireContext())
        
        setupTransactionsList()
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

    private fun setupFab(view: View) {
        val fab: FloatingActionButton = view.findViewById(R.id.fabAddTransaction)
        fab.setOnClickListener {
            val intent = Intent(requireContext(), AddEditTransactionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadTransactions() {
        transactions = PrefsManager.loadTransactions().sortedByDescending { it.date }
        
        if (transactions.isEmpty()) {
            textViewEmpty.visibility = View.VISIBLE
            recyclerViewTransactions.visibility = View.GONE
        } else {
            textViewEmpty.visibility = View.GONE
            recyclerViewTransactions.visibility = View.VISIBLE
            transactionAdapter.updateItems(transactions)
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
            private val textDate: TextView = itemView.findViewById(R.id.textTransactionDate)
            private val textAmount: TextView = itemView.findViewById(R.id.textTransactionAmount)
            
            fun bind(transaction: Transaction) {
                textTitle.text = transaction.title
                textCategory.text = transaction.category
                
                // Format date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                textDate.text = dateFormat.format(Date(transaction.date))
                
                // Format amount
                val currency = PrefsManager.getCurrency()
                val formattedAmount = CurrencyFormatter.formatCurrency(transaction.amount, currency)
                
                if (transaction.type == TxType.EXPENSE) {
                    textAmount.text = "-$formattedAmount"
                    textAmount.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                } else {
                    textAmount.text = formattedAmount
                    textAmount.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                }
                
                itemView.setOnClickListener { onClick(transaction) }
                itemView.setOnLongClickListener { onLongClick(transaction) }
            }
        }
    }
} 