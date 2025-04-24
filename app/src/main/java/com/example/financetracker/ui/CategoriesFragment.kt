package com.example.financetracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.financetracker.R
import com.example.financetracker.data.Category
import com.example.financetracker.data.PrefsManager
import com.example.financetracker.data.TxType
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class CategoriesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategoryAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize UI components
        setupRecyclerView(view)
        setupFab(view)
    }
    
    override fun onResume() {
        super.onResume()
        loadCategories()
    }
    
    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewCategories)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        adapter = CategoryAdapter(emptyList()) { category ->
            showDeleteCategoryDialog(category)
        }
        recyclerView.adapter = adapter
    }
    
    private fun setupFab(view: View) {
        val fab: FloatingActionButton = view.findViewById(R.id.fabAddCategory)
        fab.setOnClickListener {
            showAddCategoryDialog()
        }
    }
    
    private fun loadCategories() {
        val categories = PrefsManager.loadCategories()
        adapter.updateItems(categories)
    }
    
    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_category, null)
            
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                // Will be overridden below
            }
            .setNegativeButton("Cancel", null)
            .create()
            
        alertDialog.show()
        
        // Get emoji input field (now a direct input field)
        val editTextEmoji = dialogView.findViewById<EditText>(R.id.editTextCategoryEmoji)
        
        // Override the positive button to validate input
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val editTextName = dialogView.findViewById<TextView>(R.id.editTextCategoryName)
            val radioIncome = dialogView.findViewById<RadioButton>(R.id.radioIncome)
            
            val name = editTextName.text.toString().trim()
            val emoji = editTextEmoji.text.toString()
            
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val type = if (radioIncome.isChecked) TxType.INCOME else TxType.EXPENSE
            val newCategory = Category(name, type, emoji)
            
            // Check for duplicates
            val categories = PrefsManager.loadCategories()
            if (categories.any { it.name.equals(name, ignoreCase = true) && it.type == type }) {
                Toast.makeText(requireContext(), "This category already exists", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Add the new category
            val updatedCategories = categories + newCategory
            PrefsManager.saveCategories(updatedCategories)
            
            // Update the UI
            adapter.updateItems(updatedCategories)
            
            // Dismiss the dialog
            alertDialog.dismiss()
        }
    }
    
    private fun showEditCategoryDialog(category: Category) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_category, null)
            
        // Populate the dialog with existing values
        val editTextName = dialogView.findViewById<TextView>(R.id.editTextCategoryName)
        val editTextEmoji = dialogView.findViewById<EditText>(R.id.editTextCategoryEmoji)
        val radioIncome = dialogView.findViewById<RadioButton>(R.id.radioIncome)
        val radioExpense = dialogView.findViewById<RadioButton>(R.id.radioExpense)
        
        editTextName.text = category.name
        editTextEmoji.setText(category.emoji)
        
        if (category.type == TxType.INCOME) {
            radioIncome.isChecked = true
        } else {
            radioExpense.isChecked = true
        }
        
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Category")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                // Will be overridden below
            }
            .setNegativeButton("Cancel", null)
            .create()
            
        alertDialog.show()
        
        // Override the positive button to validate input
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = editTextName.text.toString().trim()
            val emoji = editTextEmoji.text.toString()
            
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val type = if (radioIncome.isChecked) TxType.INCOME else TxType.EXPENSE
            
            // Check for duplicates (excluding the current category)
            val categories = PrefsManager.loadCategories()
            if (categories.any { 
                    it.name.equals(name, ignoreCase = true) && 
                    it.type == type && 
                    !(it.name == category.name && it.type == category.type) 
                }) {
                Toast.makeText(requireContext(), "This category already exists", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Update the category
            val updatedCategories = categories.map {
                if (it.name == category.name && it.type == category.type) {
                    Category(name, type, emoji)
                } else {
                    it
                }
            }
            
            PrefsManager.saveCategories(updatedCategories)
            
            // Update the UI
            adapter.updateItems(updatedCategories)
            
            // Dismiss the dialog
            alertDialog.dismiss()
            
            // Show confirmation
            Snackbar.make(requireView(), "Category updated", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun showDeleteCategoryDialog(category: Category) {
        // Check if the category is used in any transactions
        val transactions = PrefsManager.loadTransactions()
        val isUsed = transactions.any { it.category == category.name }
        
        if (isUsed) {
            AlertDialog.Builder(requireContext())
                .setTitle("Cannot Delete Category")
                .setMessage("This category is used in one or more transactions. Delete those transactions first.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteCategory(category: Category) {
        val categories = PrefsManager.loadCategories()
        val updatedCategories = categories.filter { 
            !(it.name == category.name && it.type == category.type) 
        }
        
        PrefsManager.saveCategories(updatedCategories)
        
        // Update UI
        adapter.updateItems(updatedCategories)
        
        // Show confirmation
        Snackbar.make(requireView(), "Category deleted", Snackbar.LENGTH_SHORT).show()
    }
    
    inner class CategoryAdapter(
        private var items: List<Category>,
        private val onLongClick: (Category) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
        
        fun updateItems(newItems: List<Category>) {
            // Group items by type, then sort alphabetically within each type
            this.items = newItems
                .sortedWith(compareBy({ it.type }, { it.name }))
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val category = items[position]
            holder.bind(category)
            
            // Add section header for the first item of each type
            if (position == 0 || items[position - 1].type != category.type) {
                holder.showHeader(category.type)
            } else {
                holder.hideHeader()
            }
        }
        
        override fun getItemCount() = items.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textName: TextView = itemView.findViewById(R.id.textCategoryName)
            private val textType: TextView = itemView.findViewById(R.id.textCategoryType)
            private val textEmoji: TextView = itemView.findViewById(R.id.textCategoryEmoji)
            
            fun bind(category: Category) {
                textName.text = category.name
                textEmoji.text = if (category.emoji != null && category.emoji.isNotEmpty()) category.emoji else "📂" // default emoji
                
                val typeText = when (category.type) {
                    TxType.INCOME -> "INCOME"
                    TxType.EXPENSE -> "EXPENSE"
                }
                textType.text = typeText
                
                // Set text color to white for better contrast against colored backgrounds
                textType.setTextColor(resources.getColor(android.R.color.white, null))
                
                // Set background drawable based on transaction type
                val backgroundDrawable = when (category.type) {
                    TxType.INCOME -> R.drawable.pill_background_income
                    TxType.EXPENSE -> R.drawable.pill_background
                }
                textType.setBackgroundResource(backgroundDrawable)
                
                // Set click listener for editing
                itemView.setOnClickListener {
                    showEditCategoryDialog(category)
                }
                
                // Set click listener for editing
                itemView.setOnClickListener {
                    showEditCategoryDialog(category)
                }
                
                // Set long click listener for deletion
                itemView.setOnLongClickListener {
                    onLongClick(category)
                    true
                }
            }
            
            fun showHeader(type: TxType) {
                // This is simplified - in a real app, you'd use a proper section header
                val typeText = when (type) {
                    TxType.INCOME -> "INCOME"
                    TxType.EXPENSE -> "EXPENSE"
                }
                textType.text = typeText
                
                // Set text color to white for better contrast
                textType.setTextColor(resources.getColor(android.R.color.white, null))
                
                // Set background drawable based on transaction type
                val backgroundDrawable = when (type) {
                    TxType.INCOME -> R.drawable.pill_background_income
                    TxType.EXPENSE -> R.drawable.pill_background
                }
                textType.setBackgroundResource(backgroundDrawable)
            }
            
            fun hideHeader() {
                // In a real app with proper section headers, you'd hide the header here
            }
        }
    }
} 