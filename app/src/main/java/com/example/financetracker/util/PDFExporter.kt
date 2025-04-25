package com.example.financetracker.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.financetracker.data.PrefsManager
import com.example.financetracker.data.Transaction
import com.example.financetracker.data.TxType
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for exporting financial data to PDF
 */
object PDFExporter {
    private const val TAG = "PDFExporter"

    /**
     * Export all transactions to a PDF file
     * @param context The application context
     * @param useInternalStorage Whether to use internal or external storage
     * @return The path to the generated PDF file, or null if export failed
     */
    fun exportTransactionsToPDF(context: Context, useInternalStorage: Boolean): String? {
        val transactions = PrefsManager.loadTransactions()
        
        if (transactions.isEmpty()) {
            Log.d(TAG, "No transactions to export")
            return null
        }
        
        return try {
            // Create a filename with current date and time
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "finance_tracker_${dateFormat.format(Date())}.pdf"
            
            // Determine the file path based on storage preference
            val file = if (useInternalStorage) {
                File(context.filesDir, fileName)
            } else {
                val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                if (!storageDir.exists()) {
                    storageDir.mkdirs()
                }
                File(storageDir, fileName)
            }
            
            // Generate the PDF
            createPDF(file, transactions)
            
            // Return the file path
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting to PDF: ${e.message}", e)
            null
        }
    }
    
    /**
     * Create the PDF file with transaction data
     */
    private fun createPDF(file: File, transactions: List<Transaction>) {
        val document = Document(PageSize.A4)
        PdfWriter.getInstance(document, FileOutputStream(file))
        
        document.open()
        
        // Add title
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, BaseColor.BLACK)
        val title = Paragraph("Finance Tracker - Transaction Report", titleFont)
        title.alignment = Element.ALIGN_CENTER
        title.spacingAfter = 20f
        document.add(title)
        
        // Add date
        val dateFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.DARK_GRAY)
        val date = Paragraph("Generated on: ${SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())}", dateFont)
        date.alignment = Element.ALIGN_CENTER
        date.spacingAfter = 30f
        document.add(date)
        
        // Add summary section
        addSummarySection(document, transactions)
        
        // Add transactions table
        addTransactionsTable(document, transactions)
        
        document.close()
    }
    
    /**
     * Add summary section with total income, expenses, balance, etc.
     */
    private fun addSummarySection(document: Document, transactions: List<Transaction>) {
        val sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, BaseColor.BLACK)
        val normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.BLACK)
        
        val summarySection = Paragraph("Financial Summary", sectionFont)
        summarySection.spacingAfter = 10f
        document.add(summarySection)
        
        // Calculate summary statistics
        val totalIncome = transactions
            .filter { it.type == TxType.INCOME }
            .sumOf { it.amount }
            
        val totalExpenses = transactions
            .filter { it.type == TxType.EXPENSE }
            .sumOf { it.amount }
            
        val netBalance = totalIncome - totalExpenses
        
        // Create a table for the summary
        val summaryTable = PdfPTable(2)
        summaryTable.widthPercentage = 60f
        summaryTable.setWidths(floatArrayOf(1.5f, 1f))
        
        // Add data to summary table
        addSummaryRow(summaryTable, "Total Income:", formatAmount(totalIncome), normalFont)
        addSummaryRow(summaryTable, "Total Expenses:", formatAmount(totalExpenses), normalFont)
        addSummaryRow(summaryTable, "Net Balance:", formatAmount(netBalance), sectionFont)
        
        document.add(summaryTable)
        document.add(Paragraph("\n"))
    }
    
    /**
     * Add a row to the summary table
     */
    private fun addSummaryRow(table: PdfPTable, label: String, value: String, font: Font) {
        val cell1 = PdfPCell(Paragraph(label, font))
        cell1.horizontalAlignment = Element.ALIGN_LEFT
        cell1.border = Rectangle.NO_BORDER
        cell1.paddingBottom = 5f
        
        val cell2 = PdfPCell(Paragraph(value, font))
        cell2.horizontalAlignment = Element.ALIGN_RIGHT
        cell2.border = Rectangle.NO_BORDER
        cell2.paddingBottom = 5f
        
        table.addCell(cell1)
        table.addCell(cell2)
    }
    
    /**
     * Add transactions table with all transaction details
     */
    private fun addTransactionsTable(document: Document, transactions: List<Transaction>) {
        val sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, BaseColor.BLACK)
        val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, BaseColor.WHITE)
        val normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10f, BaseColor.BLACK)
        
        val transactionSection = Paragraph("Transaction Details", sectionFont)
        transactionSection.spacingAfter = 10f
        document.add(transactionSection)
        
        // Sort transactions by date (newest first)
        val sortedTransactions = transactions.sortedByDescending { it.date }
        
        // Create transactions table
        val table = PdfPTable(5)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(1f, 2f, 1.5f, 1.5f, 2f))
        
        // Add table headers
        val headers = arrayOf("Date", "Category", "Amount", "Type", "Note")
        
        headers.forEach { header ->
            val cell = PdfPCell(Paragraph(header, headerFont))
            cell.backgroundColor = BaseColor(63, 81, 181) // Primary color
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.verticalAlignment = Element.ALIGN_MIDDLE
            cell.paddingTop = 5f
            cell.paddingBottom = 5f
            table.addCell(cell)
        }
        
        // Add transaction rows
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        
        sortedTransactions.forEach { transaction ->
            // Date
            val dateCell = PdfPCell(Paragraph(dateFormat.format(Date(transaction.date)), normalFont))
            dateCell.horizontalAlignment = Element.ALIGN_CENTER
            table.addCell(dateCell)
            
            // Category
            val categoryCell = PdfPCell(Paragraph(transaction.category, normalFont))
            categoryCell.horizontalAlignment = Element.ALIGN_LEFT
            table.addCell(categoryCell)
            
            // Amount
            val amountCell = PdfPCell(Paragraph(formatAmount(transaction.amount), normalFont))
            amountCell.horizontalAlignment = Element.ALIGN_RIGHT
            table.addCell(amountCell)
            
            // Type
            val typeText = if (transaction.type == TxType.INCOME) "Income" else "Expense"
            val typeCell = PdfPCell(Paragraph(typeText, normalFont))
            typeCell.horizontalAlignment = Element.ALIGN_CENTER
            table.addCell(typeCell)
            
            // Note
            val noteCell = PdfPCell(Paragraph(transaction.note, normalFont))
            noteCell.horizontalAlignment = Element.ALIGN_LEFT
            table.addCell(noteCell)
        }
        
        document.add(table)
    }
    
    /**
     * Format amount with currency
     */
    private fun formatAmount(amount: Double): String {
        val currency = PrefsManager.getCurrency()
        return CurrencyFormatter.formatCurrency(amount, currency)
    }
} 