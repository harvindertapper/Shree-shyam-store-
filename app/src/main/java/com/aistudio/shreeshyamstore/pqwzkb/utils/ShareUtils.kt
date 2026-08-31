package com.aistudio.shreeshyamstore.pqwzkb.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.aistudio.shreeshyamstore.pqwzkb.data.Customer
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PaymentState
import com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SalesExportResult {
    SHARED,
    NO_SALES,
    FAILED
}

object ShareUtils {

    private fun csvField(value: String): String =
        "\"${value.replace("\"", "\"\"")}\""

    /**
     * Share formatted text (bill, receipt, message) via Android Share Sheet or directly to WhatsApp
     */
    fun shareText(context: Context, text: String, title: String = "Share via", phoneNumber: String? = null) {
        try {
            if (!phoneNumber.isNullOrBlank()) {
                // Clean phone number (strip spaces, dashes, prepend 91 if 10 digits)
                var cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
                if (cleanPhone.length == 10) {
                    cleanPhone = "91$cleanPhone"
                } else if (cleanPhone.startsWith("+")) {
                    cleanPhone = cleanPhone.removePrefix("+")
                }

                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(text)}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.whatsapp")
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                }
            }

            // Fallback to generic share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(shareIntent, title))
        } catch (e: Exception) {
            // General fallback
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            try {
                context.startActivity(Intent.createChooser(fallbackIntent, title))
            } catch (ex: Exception) {
                Toast.makeText(context, "Sharing not supported on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Generate Udhaar reminder text with dynamic clickable UPI payment link
     */
    fun formatUdhaarReminder(
        shopName: String,
        customerName: String,
        balance: Long,
        ownerPhone: String? = null,
        ownerName: String? = null,
        upiId: String? = null
    ): String {
        val formattedAmount = CurrencyUtils.formatRupees(balance)
        val sb = StringBuilder()
        sb.append("नमस्ते $customerName जी 🙏\n\n")
        sb.append("यह *$shopName* की तरफ से विनम्र सूचना है।\n")
        sb.append("आपके खाते में कुल बकाया उधार: *$formattedAmount* है।\n\n")

        // Dynamic UPI Payment Link
        val effectiveUpiPa = when {
            !upiId.isNullOrBlank() -> upiId.trim()
            !ownerPhone.isNullOrBlank() -> {
                val cleanPhone = ownerPhone.replace(Regex("[^0-9]"), "")
                if (cleanPhone.isNotEmpty()) "$cleanPhone@upi" else null
            }
            else -> null
        }

        if (effectiveUpiPa != null && balance > 0L) {
            val encodedShop = Uri.encode(shopName.trim())
            val amountFormatted = MoneyUtils.toInputString(balance)
            val upiUri = "upi://pay?pa=$effectiveUpiPa&pn=$encodedShop&am=$amountFormatted&cu=INR"
            sb.append("कृपया नीचे दिए गए UPI लिंक पर क्लिक करके भुगतान करें:\n")
            sb.append("$upiUri\n\n")
        } else {
            sb.append("कृपया सुविधानुसार जल्द से जल्द भुगतान करने का कष्ट करें।\n\n")
        }

        if (!ownerPhone.isNullOrBlank()) {
            sb.append("संपर्क / UPI नंबर: $ownerPhone\n")
        }
        if (!ownerName.isNullOrBlank()) {
            sb.append("दुकानदार: $ownerName\n")
        }
        sb.append("\nधन्यवाद!\n")
        sb.append("— $shopName")
        return sb.toString()
    }

    /**
     * Legacy alias for formatUdhaarReminder
     */
    fun generateUdhaarReminderText(
        shopName: String,
        customerName: String,
        balance: Long,
        ownerPhone: String? = null,
        ownerName: String? = null,
        upiId: String? = null
    ): String {
        return formatUdhaarReminder(shopName, customerName, balance, ownerPhone, ownerName, upiId)
    }

    /**
     * Generate structured WhatsApp / SMS bill invoice receipt text
     */
    fun generateBillReceiptText(
        shopName: String,
        billNumber: String,
        dateFormatted: String,
        items: List<SaleItem>,
        totalAmount: Long,
        paymentMode: String,
        ownerPhone: String? = null,
        ownerName: String? = null,
        paymentState: String = PaymentState.RECEIVED.wireValue,
        receivedAmount: Long? = null
    ): String {
        val sb = StringBuilder()
        sb.append("🧾 *$shopName*\n")
        if (!ownerName.isNullOrBlank()) {
            sb.append("👤 दुकानदार / Store: $ownerName\n")
        }
        if (!ownerPhone.isNullOrBlank()) {
            sb.append("📞 संपर्क / Phone: $ownerPhone\n")
        }
        sb.append("📄 बिल नंबर / Bill No: $billNumber\n")
        sb.append("📅 दिनांक / Date: $dateFormatted\n")
        sb.append("----------------------------\n")
        for (itm in items) {
            sb.append("• ${itm.productNameSnapshot}\n")
            sb.append("   ${itm.quantity} ${itm.unit} x ${CurrencyUtils.formatRupees(itm.unitPrice)} = ${CurrencyUtils.formatRupees(itm.lineTotal)}\n")
        }
        sb.append("----------------------------\n")
            sb.append("*कुल राशि / Total: ${CurrencyUtils.formatRupees(totalAmount)}*\n")
        val modeText = when (paymentMode.uppercase()) {
            "UPI" -> "UPI / QR"
            "UDHAAR" -> "उधार / Udhaar"
            else -> "नकद / Cash"
        }
        sb.append("भुगतान माध्यम / Mode: $modeText\n")
        val stateText = runCatching { PaymentState.fromWireValue(paymentState).wireValue }
            .getOrDefault(PaymentState.RECEIVED.wireValue)
        sb.append("भुगतान स्थिति / Payment Status: $stateText\n")
        if (receivedAmount != null) {
            sb.append("प्राप्त राशि / Received: ${CurrencyUtils.formatRupees(receivedAmount)}\n")
            if (paymentMode.equals("CASH", ignoreCase = true) && receivedAmount > totalAmount) {
                sb.append("वापसी / Change: ${CurrencyUtils.formatRupees(receivedAmount - totalAmount)}\n")
            }
        }
        sb.append("----------------------------\n")
        sb.append("धन्यवाद! फिर पधारें 🙏\n")
        sb.append("— $shopName")
        return sb.toString()
    }

    /**
     * Generate Wholesale Re-order List text for WhatsApp/SMS
     */
    fun generateReorderListText(
        shopName: String,
        lowStockItems: List<Product>,
        categoryNameMap: Map<Long, String>
    ): String {
        val df = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
        val sb = StringBuilder()
        sb.append("📋 *सामान का नया आर्डर (Wholesale Re-order List)*\n")
        sb.append("🏪 दुकान: *$shopName*\n")
        sb.append("📅 दिनांक: ${df.format(Date())}\n")
        sb.append("----------------------------\n")
        sb.append("कृपया निम्नलिखित सामान भेजने का कष्ट करें:\n\n")

        lowStockItems.forEachIndexed { index, item ->
            val cat = categoryNameMap[item.categoryId]?.let { " ($it)" } ?: ""
            sb.append("${index + 1}. *${item.name}*$cat\n")
            val current = if (item.currentStock <= 0) "ख़त्म (0 stock)" else "बचा: ${item.currentStock} पीस"
            sb.append("   ▸ वर्तमान स्टॉक: $current | MRP: ${CurrencyUtils.formatRupees(item.mrp)}\n")
        }

        sb.append("\n----------------------------\n")
        sb.append("कुल आइटम: ${lowStockItems.size} सामान\n")
        sb.append("धन्यवाद! 🙏\n")
        sb.append("— $shopName")
        return sb.toString()
    }

    /**
     * Export Sales History to CSV and launch share sheet
     */
    fun exportSalesCsv(
        context: Context,
        sales: List<Sale>,
        shopName: String
    ): SalesExportResult {
        if (sales.isEmpty()) {
            return SalesExportResult.NO_SALES
        }

        return try {
            val fileName = "Sales_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            writer.append("Bill Number,Date,Payment Mode,Payment State,Total Amount (INR),Received Amount (INR),Change (INR),Customer ID,Created At\n")
            val df = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ENGLISH)
            for (s in sales) {
                writer.append(csvField(s.billNumber)).append(",")
                writer.append(csvField(df.format(Date(s.createdAt)))).append(",")
                writer.append(csvField(s.paymentMode)).append(",")
                writer.append(csvField(s.paymentState)).append(",")
                writer.append(MoneyUtils.toInputString(s.totalAmount)).append(",")
                writer.append(s.receivedAmount?.let { MoneyUtils.toInputString(it) }.orEmpty()).append(",")
                val change = if (s.paymentMode.equals("CASH", ignoreCase = true) &&
                    s.receivedAmount != null && s.receivedAmount > s.totalAmount
                ) s.receivedAmount - s.totalAmount else null
                writer.append(change?.let { MoneyUtils.toInputString(it) }.orEmpty()).append(",")
                writer.append(csvField(s.customerId?.toString().orEmpty())).append(",")
                writer.append("${s.createdAt}\n")
            }
            writer.flush()
            writer.close()

            if (shareCsvFile(context, file, "$shopName - Sales Report CSV", onFailure = {})) {
                SalesExportResult.SHARED
            } else {
                SalesExportResult.FAILED
            }
        } catch (e: Exception) {
            SalesExportResult.FAILED
        }
    }

    /**
     * Export Stock Inventory to CSV
     */
    fun exportStockCsv(
        context: Context,
        products: List<Product>,
        categoryNameMap: Map<Long, String>,
        shopName: String
    ) {
        if (products.isEmpty()) {
            Toast.makeText(context, "कोई प्रोडक्ट नहीं है (No products to export)", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "Stock_Inventory_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            writer.append("Product Name,Category,MRP (INR),Selling Price (INR),Current Stock,Stock Tracked,Status\n")
            for (p in products) {
                val catName = categoryNameMap[p.categoryId] ?: "General"
                val status = if (!p.trackStock) "Not Tracked" else if (p.currentStock <= 0) "Out of Stock" else if (p.currentStock <= 5) "Low Stock" else "In Stock"
                writer.append(csvField(p.name)).append(",")
                writer.append(csvField(catName)).append(",")
                writer.append(MoneyUtils.toInputString(p.mrp)).append(",")
                writer.append(MoneyUtils.toInputString(p.sellingPrice ?: p.mrp)).append(",")
                writer.append("${p.currentStock},")
                writer.append("${p.trackStock},")
                writer.append(csvField(status)).append("\n")
            }
            writer.flush()
            writer.close()

            shareCsvFile(context, file, "$shopName - Stock Inventory CSV")
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Export Udhaar Debtor Ledger to CSV
     */
    fun exportUdhaarCsv(
        context: Context,
        customers: List<Customer>,
        balances: Map<Long, Long>,
        shopName: String
    ) {
        if (customers.isEmpty()) {
            Toast.makeText(context, "कोई ग्राहक खाता नहीं है (No customer ledger)", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "Udhaar_Ledger_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            writer.append("Customer Name,Phone,Outstanding Due (INR),Status\n")
            for (c in customers) {
                val bal = balances[c.id] ?: 0L
                val status = if (bal > 0L) "Pending Due" else "Settled"
                writer.append(csvField(c.name)).append(",")
                writer.append(csvField(c.phone.orEmpty())).append(",")
                writer.append(MoneyUtils.toInputString(bal)).append(",")
                writer.append(csvField(status)).append("\n")
            }
            writer.flush()
            writer.close()

            shareCsvFile(context, file, "$shopName - Udhaar Ledger CSV")
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareCsvFile(
        context: Context,
        file: File,
        title: String,
        onFailure: () -> Unit = {
            Toast.makeText(context, "Error sharing report file", Toast.LENGTH_SHORT).show()
        }
    ): Boolean {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
            return true
        } catch (e: Exception) {
            // Fallback: Read file text and share directly as CSV text
            try {
                val content = file.readText()
                val textIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, content)
                }
                context.startActivity(Intent.createChooser(textIntent, title))
                return true
            } catch (ex: Exception) {
                onFailure()
                return false
            }
        }
    }
}
