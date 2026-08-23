package com.aistudio.shreeshyamstore.pqwzkb.utils

enum class AppLanguage {
    HINDI,
    ENGLISH
}

object LocaleHelper {
    fun getStrings(language: AppLanguage): AppStrings {
        return when (language) {
            AppLanguage.HINDI -> HindiStrings
            AppLanguage.ENGLISH -> EnglishStrings
        }
    }
}

interface AppStrings {
    // Auth & Welcome
    val godBlessing: String
    val devotionalHeader: String
    val shopSubtitle: String
    val feature1Title: String
    val feature1Desc: String
    val feature2Title: String
    val feature2Desc: String
    val feature3Title: String
    val feature3Desc: String
    val feature4Title: String
    val feature4Desc: String
    val continueWithGoogle: String
    val offlineBadge: String
    val skipForNow: String

    // Login & Lock
    val godChant: String
    val secureAppLock: String
    val enterSecurityPin: String
    val incorrectPin: String
    val pinAttemptsRemaining: (Int) -> String
    val pinLockedOut: (Long) -> String
    val biometricUnavailable: String
    val biometricNotRecognized: String
    val biometricFailed: String
    val sessionTimedOut: String
    val storeUnlocked: String
    val unlockStore: String
    val verifyBiometric: String
    val usePin: String
    val identityVerified: String
    val forgotPin: String
    val switchAccount: String
    val resetPin: String
    val resetLinkSent: String
    val registeredEmail: String
    val sendLink: String
    val cancel: String
    val actionSignInRequired: String
    val cloudAccountRequired: String
    val actionPermissionDenied: String
    val actionRoleInvalid: String

    // Setup
    val shopSetupTitle: String
    val shopSetupDesc: String
    val storeInfo: String
    val shopNameLabel: String
    val ownerNameLabel: String
    val ownerPhoneLabel: String
    val securityPinTitle: String
    val securityPinLabel: String
    val enableBiometricLabel: String
    val startShopButton: String

    // Navigation
    val navHome: String
    val navBilling: String
    val navProducts: String
    val navUdhaar: String
    val navReports: String
    val navSettings: String

    // Extra convenient alias
    val udhaar: String
    val upiPaytm: String
    val allTime: String

    // Home
    val defaultShopName: String
    val todaySales: String
    val cashSales: String
    val upiSales: String
    val udhaarSales: String
    val totalBills: String
    val quickActions: String
    val createNewBill: String
    val addNewProduct: String
    val customerUdhaar: String
    val dailyReports: String
    val lowStockAlert: String
    val itemsNeedRestock: String
    val viewStockList: String
    val recentSales: String
    val noSalesToday: String
    val greetingMorning: String
    val greetingAfternoon: String
    val greetingEvening: String
    val greetingNight: String

    // Billing & Payment
    val newBill: String
    val quickAddProduct: String
    val searchProduct: String
    val allCategories: String
    val billCart: String
    val totalAmount: String
    val checkoutBill: String
    val paymentTitle: String
    val selectPaymentMode: String
    val cash: String
    val upiQr: String
    val udhaarMode: String
    val customerName: String
    val customerPhone: String
    val scanQrInstruction: String
    val receivedAmount: String
    val returnChange: String
    val saveBill: String
    val billSavedSuccess: String
    val shareReceiptWhatsApp: String
    val viewReceipt: String
    val nextBill: String
    val emptyCart: String
    val stockAlert: String

    // Products & Stock
    val productsTitle: String
    val addProductTitle: String
    val openingStockTitle: String
    val wholesaleReorderList: String
    val stockAdjustmentTitle: String
    val productName: String
    val category: String
    val mrpPrice: String
    val sellingPrice: String
    val purchasePrice: String
    val currentStock: String
    val unit: String
    val trackStock: String
    val lowStockAlertQty: String
    val saveProduct: String
    val editProduct: String
    val deleteProduct: String
    val inStock: String
    val lowStock: String
    val outOfStock: String
    val searchProductPlaceholder: String

    // Udhaar
    val udhaarTitle: String
    val addCustomer: String
    val searchCustomer: String
    val totalMarketCredit: String
    val sendWhatsAppReminder: String
    val receivePayment: String
    val giveCredit: String
    val customerLedger: String
    val balanceDue: String

    // Reports
    val reportsTitle: String
    val today: String
    val thisWeek: String
    val thisMonth: String
    val customRange: String
    val totalSalesRevenue: String
    val cashCollected: String
    val upiCollected: String
    val udhaarGiven: String
    val topSellingProducts: String
    val salesHistory: String

    // Settings
    val settingsTitle: String
    val languageSection: String
    val languageHindi: String
    val languageEnglish: String
    val shopProfile: String
    val paytmQrCode: String
    val uploadQr: String
    val securityPinSection: String
    val enableAppLock: String
    val changePin: String
    val biometricUnlock: String
    val welcomeChant: String
    val saveSettings: String
    val logout: String
    val activeOwner: String
}

object HindiStrings : AppStrings {
    override val godBlessing = "|| श्री गणेशाय नमः ||  जय श्री श्याम 🙏"
    override val devotionalHeader = "स्मार्ट डिजिटल स्टोर"
    override val shopSubtitle = "दुकानदार का डिजिटल बहीखाता और बिलिंग काउंटर"
    override val feature1Title = "तेज़ बिलिंग"
    override val feature1Desc = "नकद, UPI और उधार बिल 3 सेकंड में"
    override val feature2Title = "उधार बहीखाता"
    override val feature2Desc = "ग्राहक का खाता और WhatsApp पेमेंट तगादा"
    override val feature3Title = "स्टॉक प्रबंधन"
    override val feature3Desc = "कम सामान का अलर्ट और थोक खरीदारी लिस्ट"
    override val feature4Title = "सुरक्षित ऐप लॉक"
    override val feature4Desc = "4-अंकों का पिन और फिंगरप्रिंट सुरक्षा"
    override val continueWithGoogle = "Google के साथ शुरू करें"
    override val offlineBadge = "⚡ 100% ऑफ़लाइन चालू • बिना इंटरनेट के भी पूरा काम करेगा"
    override val skipForNow = "बाद में लॉगिन करें ➔"

    override val godChant = "|| श्री गणेशाय नमः ||"
    override val secureAppLock = "सुरक्षित ऐप लॉक"
    override val enterSecurityPin = "4-अंकों का सिक्योरिटी पिन दर्ज करें"
    override val incorrectPin = "गलत पिन! पुनः प्रयास करें"
    override val pinAttemptsRemaining: (Int) -> String = { attempts -> "शेष प्रयास: $attempts" }
    override val pinLockedOut: (Long) -> String = { seconds -> "बहुत अधिक गलत प्रयास। $seconds सेकंड बाद पुनः प्रयास करें या बायोमेट्रिक का उपयोग करें।" }
    override val biometricUnavailable = "बायोमेट्रिक उपलब्ध नहीं है। पिन का उपयोग करें।"
    override val biometricNotRecognized = "बायोमेट्रिक पहचान नहीं हुई। पिन का उपयोग करें।"
    override val biometricFailed = "बायोमेट्रिक अनलॉक विफल हुआ। पिन का उपयोग करें।"
    override val sessionTimedOut = "सुरक्षा के लिए सत्र लॉक हो गया। जारी रखने के लिए पिन दर्ज करें।"
    override val storeUnlocked = "दुकान अनलॉक हो गई! 🙏"
    override val unlockStore = "दुकान अनलॉक करें"
    override val verifyBiometric = "फिंगरप्रिंट या फेस से पहचान सत्यापित करें"
    override val usePin = "पिन का उपयोग करें"
    override val identityVerified = "पहचान सत्यापित! 🙏"
    override val forgotPin = "पिन भूल गए?"
    override val switchAccount = "खाता बदलें"
    override val resetPin = "पिन रीसेट करें"
    override val resetLinkSent = "रीसेट लिंक आपके ईमेल पर भेज दिया गया है!"
    override val registeredEmail = "रजिस्टर्ड ईमेल"
    override val sendLink = "लिंक भेजें"
    override val cancel = "रद्द करें"
    override val actionSignInRequired = "इस काम के लिए पहले साइन इन करें। डेटा डिवाइस पर सुरक्षित रहेगा।"
    override val cloudAccountRequired = "क्लाउड बैकअप या रिस्टोर के लिए Google खाते से साइन इन करें।"
    override val actionPermissionDenied = "इस खाते को यह काम करने की अनुमति नहीं है। मालिक से संपर्क करें।"
    override val actionRoleInvalid = "खाते की भूमिका मान्य नहीं है। मालिक से संपर्क करें।"

    override val shopSetupTitle = "दुकान प्रोफाइल सेटअप"
    override val shopSetupDesc = "अपनी दुकान का विवरण और 4-अंकों का सुरक्षित पिन सेट करें।"
    override val storeInfo = "दुकान की जानकारी"
    override val shopNameLabel = "दुकान का नाम *"
    override val ownerNameLabel = "दुकानदार का नाम"
    override val ownerPhoneLabel = "ओनर का मोबाइल नंबर"
    override val securityPinTitle = "सुरक्षा पिन सेटअप"
    override val securityPinLabel = "4-अंकों का ऐप पिन *"
    override val enableBiometricLabel = "फिंगरप्रिंट लॉक चालू करें"
    override val startShopButton = "दुकान शुरू करें"

    override val navHome = "होम"
    override val navBilling = "बिलिंग"
    override val navProducts = "सामान"
    override val navUdhaar = "उधार खाता"
    override val navReports = "रिपोर्ट्स"
    override val navSettings = "सेटिंग्स"

    override val udhaar = "उधार"
    override val upiPaytm = "Paytm / UPI"
    override val allTime = "सभी समय"

    override val defaultShopName = "स्मार्ट डिजिटल स्टोर"
    override val todaySales = "आज की कुल बिक्री"
    override val cashSales = "नकद बिक्री"
    override val upiSales = "UPI बिक्री"
    override val udhaarSales = "उधार बिक्री"
    override val totalBills = "कुल बिल"
    override val quickActions = "त्वरित कार्य"
    override val createNewBill = "नया बिल बनाएं"
    override val addNewProduct = "नया सामान जोड़ें"
    override val customerUdhaar = "उधार बहीखाता"
    override val dailyReports = "दैनिक रिपोर्ट्स"
    override val lowStockAlert = "कम स्टॉक चेतावनी"
    override val itemsNeedRestock = "सामान खत्म होने वाला है"
    override val viewStockList = "स्टॉक लिस्ट देखें"
    override val recentSales = "हाल के बिल"
    override val noSalesToday = "आज अभी तक कोई बिल नहीं बना है"
    override val greetingMorning = "शुभ प्रभात! ☀️"
    override val greetingAfternoon = "शुभ दोपहर! 🌤️"
    override val greetingEvening = "शुभ संध्या! 🌅"
    override val greetingNight = "शुभ रात्रि! 🌙"

    override val newBill = "नया बिल"
    override val quickAddProduct = "त्वरित सामान जोड़ें"
    override val searchProduct = "सामान का नाम खोजें..."
    override val allCategories = "सभी"
    override val billCart = "बिल कार्ट"
    override val totalAmount = "कुल रकम"
    override val checkoutBill = "बिल पक्का करें"
    override val paymentTitle = "भुगतान एवं बिल"
    override val selectPaymentMode = "भुगतान का माध्यम चुनें"
    override val cash = "नकद"
    override val upiQr = "Paytm QR"
    override val udhaarMode = "उधार खाता"
    override val customerName = "ग्राहक का नाम"
    override val customerPhone = "ग्राहक का मोबाइल नंबर"
    override val scanQrInstruction = "ग्राहक इस QR को स्कैन करके पेमेंट करें"
    override val receivedAmount = "प्राप्त रकम"
    override val returnChange = "वापस करने हैं"
    override val saveBill = "बिल सेव करें"
    override val billSavedSuccess = "बिल सफलतापूर्वक सेव हो गया!"
    override val shareReceiptWhatsApp = "WhatsApp पर रसीद भेजें"
    override val viewReceipt = "रसीद देखें"
    override val nextBill = "अगला बिल बनाएं"
    override val emptyCart = "कार्ट खाली है, सामान जोड़ें"
    override val stockAlert = "स्टॉक चेतावनी"

    override val productsTitle = "दुकान का सामान"
    override val addProductTitle = "नया सामान जोड़ें"
    override val openingStockTitle = "ओपनिंग स्टॉक"
    override val wholesaleReorderList = "थोक खरीदारी लिस्ट"
    override val stockAdjustmentTitle = "स्टॉक सुधार"
    override val productName = "सामान का नाम"
    override val category = "कैटेगरी"
    override val mrpPrice = "MRP कीमत"
    override val sellingPrice = "बिक्री कीमत"
    override val purchasePrice = "खरीद कीमत"
    override val currentStock = "वर्तमान स्टॉक"
    override val unit = "इकाई"
    override val trackStock = "स्टॉक ट्रैक करें"
    override val lowStockAlertQty = "कम स्टॉक अलर्ट संख्या"
    override val saveProduct = "सामान सुरक्षित करें"
    override val editProduct = "सामान एडिट करें"
    override val deleteProduct = "सामान हटाएं"
    override val inStock = "स्टॉक में है"
    override val lowStock = "कम स्टॉक"
    override val outOfStock = "स्टॉक खत्म"
    override val searchProductPlaceholder = "सामान खोजें..."

    override val udhaarTitle = "उधार बहीखाता"
    override val addCustomer = "नया ग्राहक जोड़ें"
    override val searchCustomer = "ग्राहक खोजें..."
    override val totalMarketCredit = "मार्केट में कुल बकाया"
    override val sendWhatsAppReminder = "WhatsApp तगादा"
    override val receivePayment = "जमा करें"
    override val giveCredit = "उधार दें"
    override val customerLedger = "खाता विवरण"
    override val balanceDue = "बकाया रकम"

    override val reportsTitle = "दुकान रिपोर्ट्स"
    override val today = "आज"
    override val thisWeek = "इस सप्ताह"
    override val thisMonth = "इस महीने"
    override val customRange = "तारीख चुनें"
    override val totalSalesRevenue = "कुल बिक्री"
    override val cashCollected = "नकद संग्रह"
    override val upiCollected = "UPI संग्रह"
    override val udhaarGiven = "उधार दिया"
    override val topSellingProducts = "सबसे ज्यादा बिकने वाला सामान"
    override val salesHistory = "बिक्री इतिहास"

    override val settingsTitle = "ऐप सेटिंग्स"
    override val languageSection = "भाषा चुनें"
    override val languageHindi = "हिंदी (Hindi)"
    override val languageEnglish = "English"
    override val shopProfile = "दुकान की जानकारी"
    override val paytmQrCode = "दुकान का Paytm QR कोड"
    override val uploadQr = "QR फोटो चुनें"
    override val securityPinSection = "सुरक्षा एवं पिन लॉक"
    override val enableAppLock = "ऐप लॉक चालू रखें"
    override val changePin = "4-अंकों का पिन बदलें"
    override val biometricUnlock = "फिंगरप्रिंट अनलॉक"
    override val welcomeChant = "शुरुआती मंत्र व जयकारा"
    override val saveSettings = "सेटिंग्स सुरक्षित करें"
    override val logout = "लॉगआउट"
    override val activeOwner = "सक्रिय दुकानदार"
}

object EnglishStrings : AppStrings {
    override val godBlessing = "|| Shree Ganeshay Namah ||  Jai Shree Shyam 🙏"
    override val devotionalHeader = "Smart Digital Store"
    override val shopSubtitle = "Smart Digital Ledger & POS Billing Counter"
    override val feature1Title = "Fast POS Billing"
    override val feature1Desc = "Cash, UPI & Credit bills in under 3 seconds"
    override val feature2Title = "Udhaar Ledger"
    override val feature2Desc = "Track customer credit & send WhatsApp reminders"
    override val feature3Title = "Stock Manager"
    override val feature3Desc = "Low stock alerts & wholesale re-order list"
    override val feature4Title = "Secure App Lock"
    override val feature4Desc = "4-digit PIN & Biometric security"
    override val continueWithGoogle = "Continue with Google"
    override val offlineBadge = "⚡ 100% Offline Ready • Works seamlessly without internet"
    override val skipForNow = "Skip for now ➔"

    override val godChant = "|| Shree Ganeshay Namah ||"
    override val secureAppLock = "Secure App Lock"
    override val enterSecurityPin = "Enter 4-Digit Security PIN"
    override val incorrectPin = "Incorrect PIN! Please try again"
    override val pinAttemptsRemaining: (Int) -> String = { attempts -> "Attempts remaining: $attempts" }
    override val pinLockedOut: (Long) -> String = { seconds -> "Too many failed attempts. Try again in ${seconds}s or use biometric unlock." }
    override val biometricUnavailable = "Biometric unlock is unavailable. Use your PIN."
    override val biometricNotRecognized = "Biometric not recognized. Use your PIN."
    override val biometricFailed = "Biometric unlock failed. Use your PIN."
    override val sessionTimedOut = "The session was locked for your security. Enter your PIN to continue."
    override val storeUnlocked = "Store Unlocked! 🙏"
    override val unlockStore = "Unlock Store"
    override val verifyBiometric = "Verify identity with fingerprint or face"
    override val usePin = "Use PIN"
    override val identityVerified = "Identity Verified! 🙏"
    override val forgotPin = "Forgot PIN?"
    override val switchAccount = "Switch Account"
    override val resetPin = "Reset PIN"
    override val resetLinkSent = "Reset link has been sent to your email!"
    override val registeredEmail = "Registered Email"
    override val sendLink = "Send Link"
    override val cancel = "Cancel"
    override val actionSignInRequired = "Sign in before continuing. Your data remains safe on this device."
    override val cloudAccountRequired = "Sign in with Google for cloud backup or restore."
    override val actionPermissionDenied = "This account is not allowed to perform this action. Contact the owner."
    override val actionRoleInvalid = "This account has an invalid role. Contact the owner."

    override val shopSetupTitle = "Shop Profile Setup"
    override val shopSetupDesc = "Enter your store details and set a 4-digit security PIN."
    override val storeInfo = "Store Information"
    override val shopNameLabel = "Shop Name *"
    override val ownerNameLabel = "Owner Name"
    override val ownerPhoneLabel = "Owner Phone Number"
    override val securityPinTitle = "Security PIN Setup"
    override val securityPinLabel = "4-Digit App PIN *"
    override val enableBiometricLabel = "Enable Biometric Lock"
    override val startShopButton = "Start My Shop"

    override val navHome = "Home"
    override val navBilling = "Billing"
    override val navProducts = "Products"
    override val navUdhaar = "Udhaar"
    override val navReports = "Reports"
    override val navSettings = "Settings"

    override val udhaar = "Udhaar"
    override val upiPaytm = "Paytm / UPI"
    override val allTime = "All Time"

    override val defaultShopName = "Smart Digital Store"
    override val todaySales = "Today's Total Sales"
    override val cashSales = "Cash Sales"
    override val upiSales = "UPI Sales"
    override val udhaarSales = "Udhaar Sales"
    override val totalBills = "Total Bills"
    override val quickActions = "Quick Actions"
    override val createNewBill = "Create New Bill"
    override val addNewProduct = "Add New Product"
    override val customerUdhaar = "Customer Udhaar"
    override val dailyReports = "Daily Reports"
    override val lowStockAlert = "Low Stock Alert"
    override val itemsNeedRestock = "Items need restocking"
    override val viewStockList = "View Stock List"
    override val recentSales = "Recent Bills"
    override val noSalesToday = "No bills generated today yet"
    override val greetingMorning = "Good Morning! ☀️"
    override val greetingAfternoon = "Good Afternoon! 🌤️"
    override val greetingEvening = "Good Evening! 🌅"
    override val greetingNight = "Good Night! 🌙"

    override val newBill = "New Bill"
    override val quickAddProduct = "Quick Add Product"
    override val searchProduct = "Search product name..."
    override val allCategories = "All"
    override val billCart = "Bill Cart"
    override val totalAmount = "Total Amount"
    override val checkoutBill = "Checkout Bill"
    override val paymentTitle = "Payment & Bill"
    override val selectPaymentMode = "Select Payment Mode"
    override val cash = "Cash"
    override val upiQr = "Paytm QR"
    override val udhaarMode = "Udhaar Ledger"
    override val customerName = "Customer Name"
    override val customerPhone = "Customer Phone Number"
    override val scanQrInstruction = "Ask customer to scan and pay via QR"
    override val receivedAmount = "Received Amount"
    override val returnChange = "Return Change"
    override val saveBill = "Save Bill"
    override val billSavedSuccess = "Bill saved successfully!"
    override val shareReceiptWhatsApp = "Share Receipt on WhatsApp"
    override val viewReceipt = "View Receipt"
    override val nextBill = "Create Next Bill"
    override val emptyCart = "Cart is empty, please add items"
    override val stockAlert = "Stock Warning"

    override val productsTitle = "Store Inventory"
    override val addProductTitle = "Add New Product"
    override val openingStockTitle = "Opening Stock"
    override val wholesaleReorderList = "Wholesale Re-order List"
    override val stockAdjustmentTitle = "Stock Adjustment"
    override val productName = "Product Name"
    override val category = "Category"
    override val mrpPrice = "MRP Price"
    override val sellingPrice = "Selling Price"
    override val purchasePrice = "Purchase Price"
    override val currentStock = "Current Stock"
    override val unit = "Unit"
    override val trackStock = "Track Stock"
    override val lowStockAlertQty = "Low Stock Alert Qty"
    override val saveProduct = "Save Product"
    override val editProduct = "Edit Product"
    override val deleteProduct = "Delete Product"
    override val inStock = "In Stock"
    override val lowStock = "Low Stock"
    override val outOfStock = "Out of Stock"
    override val searchProductPlaceholder = "Search products..."

    override val udhaarTitle = "Udhaar Ledger"
    override val addCustomer = "Add New Customer"
    override val searchCustomer = "Search customer..."
    override val totalMarketCredit = "Total Market Credit"
    override val sendWhatsAppReminder = "WhatsApp Reminder"
    override val receivePayment = "Receive Payment"
    override val giveCredit = "Give Credit"
    override val customerLedger = "Customer Ledger"
    override val balanceDue = "Balance Due"

    override val reportsTitle = "Shop Reports"
    override val today = "Today"
    override val thisWeek = "This Week"
    override val thisMonth = "This Month"
    override val customRange = "Custom Range"
    override val totalSalesRevenue = "Total Sales"
    override val cashCollected = "Cash Collected"
    override val upiCollected = "UPI Collected"
    override val udhaarGiven = "Credit Given"
    override val topSellingProducts = "Top Selling Products"
    override val salesHistory = "Sales History"

    override val settingsTitle = "Settings"
    override val languageSection = "Language"
    override val languageHindi = "हिंदी (Hindi)"
    override val languageEnglish = "English"
    override val shopProfile = "Shop Profile"
    override val paytmQrCode = "Shop Paytm QR Code"
    override val uploadQr = "Upload QR Image"
    override val securityPinSection = "Security & PIN Lock"
    override val enableAppLock = "Enable App Lock"
    override val changePin = "Change 4-Digit PIN"
    override val biometricUnlock = "Biometric Unlock"
    override val welcomeChant = "Welcome Devotional Chant"
    override val saveSettings = "Save Settings"
    override val logout = "Logout"
    override val activeOwner = "Active Owner"
}
