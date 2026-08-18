package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.Customer
import com.example.data.UdhaarTransaction
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for generating professional PDF & CSV ledger statements for store customers.
 */
object PdfReportUtils {

    /**
     * Generates a clean A4 customer ledger statement PDF with shop header, customer metrics,
     * chronological debit/credit transaction table, and net outstanding balance.
     *
     * Returns a FileProvider content URI suitable for direct sharing via WhatsApp or system intents.
     */
    fun generateCustomerLedgerPdf(
        context: Context,
        customer: Customer,
        transactions: List<UdhaarTransaction>,
        shopName: String
    ): Uri? {
        val pdfDocument = PdfDocument()

        try {
            // Standard A4 dimensions: 595 x 842 points (72 dpi)
            val pageWidth = 595
            val pageHeight = 842
            var pageNumber = 1
            var page = pdfDocument.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            var canvas: Canvas = page.canvas

            // Paints
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(33, 33, 33)
                textSize = 10f
            }

            val boldPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(33, 33, 33)
                textSize = 11f
                isFakeBoldText = true
            }

            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(230, 81, 0) // Saffron primary tone
                textSize = 18f
                isFakeBoldText = true
            }

            val subtitlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(97, 97, 97)
                textSize = 11f
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(255, 243, 224) // Light saffron tint
                style = Paint.Style.FILL
            }

            val linePaint = Paint().apply {
                color = Color.rgb(224, 224, 224)
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }

            val greenPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(46, 125, 50) // Green (Jama / Payment)
                textSize = 10f
                isFakeBoldText = true
            }

            val redPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(198, 40, 40) // Red (Udhaar / Debit)
                textSize = 10f
                isFakeBoldText = true
            }

            val margin = 36f
            var y = margin + 20f

            // 1. Header Banner
            val bannerRect = RectF(margin, margin, pageWidth - margin, margin + 70f)
            canvas.drawRoundRect(bannerRect, 12f, 12f, headerBgPaint)

            canvas.drawText(shopName.ifBlank { "SHREE SHYAM STORE" }.uppercase(Locale.ENGLISH), margin + 16f, y + 10f, titlePaint)
            y += 28f
            canvas.drawText("CUSTOMER UDHAAR KHATA STATEMENT / खाता विवरण", margin + 16f, y + 8f, boldPaint)

            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
            val dateStr = "Date: ${dateFormat.format(Date())}"
            val dateWidth = subtitlePaint.measureText(dateStr)
            canvas.drawText(dateStr, pageWidth - margin - 16f - dateWidth, y + 8f, subtitlePaint)

            y = margin + 90f

            // 2. Customer & Account Summary Box
            var totalUdhaar = 0L
            var totalRepaid = 0L
            val sortedTxs = transactions.sortedBy { it.createdAt }
            sortedTxs.forEach { tx ->
                when (tx.type.uppercase(Locale.ENGLISH)) {
                    "CREDIT" -> totalUdhaar += tx.amount
                    "PAYMENT" -> totalRepaid += tx.amount
                }
            }
            val netBalance = (totalUdhaar - totalRepaid).coerceAtLeast(0L)

            val custBoxRect = RectF(margin, y, pageWidth - margin, y + 74f)
            canvas.drawRoundRect(custBoxRect, 8f, 8f, linePaint)

            canvas.drawText("Customer Name: ${customer.name}", margin + 14f, y + 20f, boldPaint)
            canvas.drawText("Phone: ${customer.phone?.ifBlank { "N/A" } ?: "N/A"}", margin + 14f, y + 40f, textPaint)
            canvas.drawText("Credit Limit: ${CurrencyUtils.formatRupees(customer.creditLimit)}", margin + 14f, y + 60f, subtitlePaint)

            // Right side summary
            val summaryX = pageWidth - margin - 200f
            canvas.drawText("Total Udhaar Given: ${CurrencyUtils.formatRupees(totalUdhaar)}", summaryX, y + 20f, redPaint)
            canvas.drawText("Total Repaid (Jama): ${CurrencyUtils.formatRupees(totalRepaid)}", summaryX, y + 40f, greenPaint)

            val netBalPaint = Paint().apply {
                isAntiAlias = true
                color = if (netBalance > 0L) Color.rgb(198, 40, 40) else Color.rgb(46, 125, 50)
                textSize = 13f
                isFakeBoldText = true
            }
            canvas.drawText("Net Due: ${CurrencyUtils.formatRupees(netBalance)}", summaryX, y + 62f, netBalPaint)

            y += 94f

            // 3. Table Header
            val tableHeaderRect = RectF(margin, y, pageWidth - margin, y + 24f)
            val tableHeaderBg = Paint().apply {
                color = Color.rgb(245, 245, 245)
                style = Paint.Style.FILL
            }
            canvas.drawRect(tableHeaderRect, tableHeaderBg)
            canvas.drawRect(tableHeaderRect, linePaint)

            val colDate = margin + 8f
            val colDesc = margin + 120f
            val colType = margin + 300f
            val colAmount = margin + 390f
            val colBalance = margin + 465f

            canvas.drawText("Date & Time", colDate, y + 16f, boldPaint)
            canvas.drawText("Description / Note", colDesc, y + 16f, boldPaint)
            canvas.drawText("Type", colType, y + 16f, boldPaint)
            canvas.drawText("Amount", colAmount, y + 16f, boldPaint)
            canvas.drawText("Balance", colBalance, y + 16f, boldPaint)

            y += 24f

            // 4. Table Rows
            val rowDateFormat = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.ENGLISH)
            var runningBalance = 0L

            fun startContinuationPage() {
                pdfDocument.finishPage(page)
                pageNumber += 1
                page = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = page.canvas
                y = margin + 32f
                canvas.drawText(
                    "${shopName.ifBlank { "SHREE SHYAM STORE" }.uppercase(Locale.ENGLISH)} — ${customer.name} (continued)",
                    margin,
                    y,
                    boldPaint
                )
                y += 20f
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 12f
                canvas.drawRect(
                    RectF(margin, y, pageWidth - margin, y + 24f),
                    tableHeaderBg
                )
                canvas.drawRect(RectF(margin, y, pageWidth - margin, y + 24f), linePaint)
                canvas.drawText("Date & Time", colDate, y + 16f, boldPaint)
                canvas.drawText("Description / Note", colDesc, y + 16f, boldPaint)
                canvas.drawText("Type", colType, y + 16f, boldPaint)
                canvas.drawText("Amount", colAmount, y + 16f, boldPaint)
                canvas.drawText("Balance", colBalance, y + 16f, boldPaint)
                y += 24f
            }

            if (sortedTxs.isEmpty()) {
                y += 20f
                canvas.drawText("No transactions recorded for this customer.", margin + 14f, y, subtitlePaint)
                y += 20f
            } else {
                for (tx in sortedTxs) {
                    if (y > pageHeight - 60f) {
                        startContinuationPage()
                    }

                    val isCredit = tx.type.equals("CREDIT", ignoreCase = true) || tx.type.equals("UDHAAR", ignoreCase = true)
                    if (isCredit) {
                        runningBalance += tx.amount
                    } else {
                        runningBalance = (runningBalance - tx.amount).coerceAtLeast(0L)
                    }

                    // Row underline
                    canvas.drawLine(margin, y + 20f, pageWidth - margin, y + 20f, linePaint)

                    val dateText = rowDateFormat.format(Date(tx.createdAt))
                    val descText = (tx.note ?: if (isCredit) "Store Bill Udhaar" else "Payment Received").take(24)
                    val typeText = if (isCredit) "UDHAAR (+)" else "JAMA (-)"
                    val amountText = CurrencyUtils.formatRupees(tx.amount)
                    val balText = CurrencyUtils.formatRupees(runningBalance)

                    canvas.drawText(dateText, colDate, y + 14f, textPaint)
                    canvas.drawText(descText, colDesc, y + 14f, textPaint)
                    canvas.drawText(typeText, colType, y + 14f, if (isCredit) redPaint else greenPaint)
                    canvas.drawText(amountText, colAmount, y + 14f, if (isCredit) redPaint else greenPaint)
                    canvas.drawText(balText, colBalance, y + 14f, boldPaint)

                    y += 22f
                }
            }

            // 5. Footer Note
            val footerY = pageHeight - margin
            canvas.drawLine(margin, footerY - 20f, pageWidth - margin, footerY - 20f, linePaint)
            val footerText = "Thank you for your business with $shopName 🙏 • Shree Shyam POS"
            canvas.drawText(footerText, margin, footerY - 6f, subtitlePaint)

            pdfDocument.finishPage(page)

            // Save to application cache directory
            val cleanName = customer.name.replace(Regex("[^a-zA-Z0-9]"), "_")
            val fileName = "Ledger_${cleanName}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e("PdfReportUtils", "Error generating customer ledger PDF", e)
            return null
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Fallback CSV statement generator for customer ledger.
     */
    fun generateCustomerLedgerCsv(
        context: Context,
        customer: Customer,
        transactions: List<UdhaarTransaction>
    ): Uri? {
        try {
            val cleanName = customer.name.replace(Regex("[^a-zA-Z0-9]"), "_")
            val fileName = "Ledger_${cleanName}_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            writer.append("Transaction ID,Date,Type,Amount (INR),Running Balance (INR),Note\n")
            val df = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ENGLISH)
            var runningBalance = 0L

            val sorted = transactions.sortedBy { it.createdAt }
            for (tx in sorted) {
                val isCredit = tx.type.equals("CREDIT", ignoreCase = true) || tx.type.equals("UDHAAR", ignoreCase = true)
                if (isCredit) {
                    runningBalance += tx.amount
                } else {
                    runningBalance = (runningBalance - tx.amount).coerceAtLeast(0L)
                }

                writer.append("${tx.id},")
                writer.append("\"${df.format(Date(tx.createdAt))}\",")
                writer.append("\"${if (isCredit) "UDHAAR" else "JAMA"}\",")
                writer.append(MoneyUtils.toInputString(tx.amount)).append(",")
                writer.append(MoneyUtils.toInputString(runningBalance)).append(",")
                writer.append("\"${(tx.note ?: "").replace("\"", "\"\"")}\"\n")
            }
            writer.flush()
            writer.close()

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e("PdfReportUtils", "Error generating customer ledger CSV", e)
            return null
        }
    }
}
