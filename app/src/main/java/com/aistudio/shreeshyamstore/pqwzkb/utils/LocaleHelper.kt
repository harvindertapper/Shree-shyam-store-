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

    // Durable mutation feedback
    val statusValidating: String
    val statusSavingLocally: String
    val statusSavedLocally: String
    val statusSavedLocallyDetail: String
    val statusSyncing: String
    val statusSuccess: String
    val statusValidationError: String
    val statusAuthError: String
    val statusRetryableError: String
    val statusConflict: String
    val statusFailure: String
    val retryAction: String
    val dismissAction: String
    val checkoutCreditLimitError: String
    val checkoutStockError: String
    val checkoutCustomerError: String
    val checkoutValidationError: String
    val checkoutSaveError: String
    val paymentUpdateError: String
    val backupSuccess: String
    val restoreSuccess: String

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
    val addNewCategory: String
    val noCategories: String
    val noReasonsAvailable: String
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

    // Product and stock form validation
    val productFormBack: String
    val productFormBarcodeLabel: String
    val productFormBarcodePlaceholder: String
    val productFormScanBarcode: String
    val productFormAddCategoryTitle: String
    val productFormCategoryName: String
    val productFormAddCategory: String
    val productFormUnitLabel: String
    val productFormUnitHint: String
    val productFormTrackStockHint: String
    val productFormActiveLabel: String
    val productFormActiveHint: String
    val productFormMoneyHint: String
    val productFormQuantityHint: String
    val productFormRequiredField: (String) -> String
    val productFormInvalidNumber: (String) -> String
    val productFormInvalidField: (String) -> String
    val productFormNegativeNumber: (String) -> String
    val productFormNonPositiveMrp: String
    val productFormFractionNotAllowed: (String) -> String
    val productFormCategoryRequired: String
    val productFormBarcodeInvalid: String
    val productFormBarcodeSet: (String) -> String
    val productFormChooseCategory: String
    val productFormFastAddPrefix: String
    val productFormAddItem: String
    val productFormRecentlyAdded: String
    val productFormTotal: String
    val productFormNoItemsInCategory: String
    val productFormPrice: String
    val productFormStock: String
    val productFormUntracked: String
    val productFormNoProducts: String
    val productFormInactive: String
    val productFormLowStock: (String) -> String
    val productFormStockAvailable: (String) -> String
    val productFormStockUntracked: String
    val productFormAdjustStock: String
    val productFormSaveAndAdd: String
    val productFormCategoryManager: String
    val productFormNewCategoryPlaceholder: String
    val productFormRenameCategory: String
    val productFormUpdateCategory: String
    val productFormDone: String
    val productFormNoLowStock: String
    val productFormStockHealthy: String
    val productFormAddStock: String
    val productFormLeft: (String) -> String
    val productFormClose: String
    val productFormPlusOneStock: String
    val productFormCopy: String
    val productFormBarcodeScanned: (String) -> String
    val productFormLowStockBanner: (Int) -> String
    val productFormReorderListHint: String
    val productFormOrderList: String
    val stockAdjustmentCurrentStock: (String) -> String
    val stockAdjustmentEnterPhysicalCount: String
    val stockAdjustmentReason: String
    val stockAdjustmentSave: String
    val stockAdjustmentHistory: String
    val stockAdjustmentStockTransition: (String, String) -> String
    val stockAdjustmentProductNotFound: String
    val stockReasonCountCorrection: String
    val stockReasonPurchaseAdded: String
    val stockReasonDamaged: String
    val stockReasonOpening: String
    val stockReasonOther: String

    // Settings control center
    val settingsAccountSection: String
    val settingsAccountProvider: String
    val settingsProviderFirebase: String
    val settingsProviderLocal: String
    val settingsNotAssigned: String
    val settingsStoreIdentity: String
    val settingsTenantIdentity: String
    val settingsSignedIn: String
    val settingsSignedOut: String
    val settingsCloudSignInRequired: String
    val settingsShopProfileHint: String
    val settingsSecurityHint: String
    val settingsAppLockOnHint: String
    val settingsAppLockOffHint: String
    val settingsSetPinHint: String
    val settingsPinConfirmLabel: String
    val settingsPinMismatch: String
    val settingsBiometricAvailable: String
    val settingsBiometricUnavailable: String
    val settingsLanguageHint: String
    val settingsAppearanceSection: String
    val settingsAppearanceHint: String
    val settingsAutomaticSync: String
    val settingsAutomaticSyncHint: String
    val settingsManualSync: String
    val settingsCloudBackupTitle: String
    val settingsCloudBackupHint: String
    val settingsBackupNow: String
    val settingsRestore: String
    val settingsRestoreWarningTitle: String
    val settingsRestoreWarningMessage: String
    val settingsRestoreConfirmAction: String
    val settingsBillingSection: String
    val settingsManualUpiSettlement: String
    val settingsManualUpiSettlementHint: String
    val settingsInventorySection: String
    val settingsInventoryHint: String
    val settingsCustomersSection: String
    val settingsCustomersHint: String
    val settingsSyncSection: String
    val settingsSyncHealth: String
    val settingsLastAttempt: String
    val settingsLastSuccess: String
    val settingsLastAttemptUnavailable: String
    val settingsHealthHealthy: String
    val settingsHealthNever: String
    val settingsHealthPending: String
    val settingsHealthRetrying: String
    val settingsHealthBlocked: String
    val settingsPending: String
    val settingsRetryable: String
    val settingsDeadLetter: String
    val settingsConflicts: String
    val settingsDataPrivacySection: String
    val settingsDataPrivacyHint: String
    val settingsSupportSection: String
    val settingsVersion: String
    val settingsSupportHint: String
    val settingsSaveHint: String
    val settingsSavedLocally: String
    val settingsSaveValidationShopName: String
    val settingsSaveValidationPin: String
    val settingsSaveValidationPinConfirm: String
    val settingsDisableLockTitle: String
    val settingsDisableLockMessage: String
    val settingsConfirmDisable: String
    val settingsKeepLock: String
    val settingsLockDraftNotice: String
    val settingsOwnerPhonePlaceholder: String
    val settingsPinPlaceholder: String
    val settingsBack: String
    val settingsUserAvatar: String
    val settingsSignIn: String
    val settingsWelcomeChantHint: String
    val settingsQrPreview: String
    val settingsChangeQr: String
    val settingsNoQrSelected: String
    val settingsSyncNote: String
    val settingsLastSyncLabel: String
    val settingsSyncStatusSuccess: String
    val settingsSyncStatusNoChanges: String
    val settingsSyncStatusFailed: String
    val settingsSyncStatusUnavailable: String

    // Issue #54 localization completeness: shared and representative journey copy
    val commonLanguage: String
    val commonBack: String
    val commonClose: String
    val commonOkay: String
    val commonDelete: String
    val commonBiometric: String
    val commonSuccess: String
    val commonScanBarcode: String
    val commonQuickAdd: String
    val commonReorderList: String
    val commonExportStock: String
    val commonManageCategories: String
    val commonExportSales: String
    val commonExportUdhaar: String
    val commonSeeItems: String
    val commonAddToBasket: String
    val commonRemoveFromBasket: String
    val commonAdd: String
    val commonRemove: String
    val commonLedgerActions: String
    val commonStaticPaytmQr: String
    val commonNoQrConfigured: String
    val commonWhatsApp: String
    val commonCustomer: String
    val scannerTitle: String
    val scannerSubtitle: String
    val scannerCameraPermissionTitle: String
    val scannerCameraPermissionMessage: String
    val scannerAllow: String
    val scannerToggleTorch: String
    val scannerCancel: String

    val languageEnglishCode: String
    val languageHindiCode: String
    val authLoginSuccess: (String) -> String
    val authGoogleSignInFailed: String
    val authLocalPinTitle: String
    val authLocalPinHelp: String
    val authGooglePinTitle: String
    val authGooglePinHelp: String
    val authSwitchAccountHelp: String
    val authUsernameOrEmail: String
    val authLocalPassword: String
    val authAccount: (String) -> String
    val authGoogleAccount: String
    val authNewPin: String
    val authPinChanged: String
    val authChangePin: String
    val authVerifyGoogle: String
    val authSignIn: String

    val billingProductNotFound: String
    val billingLowStock: (String) -> String
    val billingStockAvailable: (String) -> String
    val billingStockUntracked: String
    val billingCartSummary: (String) -> String
    val billingInsufficientStockTitle: String
    val billingInsufficientStockMessage: (String, String) -> String
    val billingProductAdded: (String) -> String
    val billingBarcodeNotFound: (String) -> String
    val billingPayableAmount: String
    val billingReceivedCashUpi: String
    val billingQrPrompt: String
    val billingNoQrTitle: String
    val billingNoQrMessage: String
    val billingUpiPaid: String
    val billingSelectUdhaarCustomer: String
    val billingNoCustomerFound: String
    val billingAddCustomer: (String) -> String
    val billingConfirmUdhaar: String
    val billingBillNumber: String
    val billingPaymentMode: String
    val billingPaymentStatus: String
    val billingReceived: String
    val billingChange: String
    val billingCopyInvoice: String
    val billingHistory: String

    val homeCloudSyncTriggered: String
    val homeCloudBackupActive: String
    val homeBillsCreated: (Int) -> String
    val homeRestockNeeded: (Int) -> String
    val homeStockSafe: String
    val homeStockSufficient: String
    val homeBillingSubtitle: String
    val homeInventorySubtitle: String
    val homeUdhaarSubtitle: String
    val homeReportsSubtitle: String

    val reportsTotalSalesTitle: String
    val reportsBillsGenerated: (Int) -> String
    val reportsHistoryTitle: String
    val reportsNoRecords: String
    val reportsUdhaarCustomer: String
    val reportsBillNumber: String
    val reportsCustomer: String
    val reportsPaymentMode: String
    val reportsCopy: String
    val reportsClose: String
    val reportsNoChartData: String
    val reportsPaymentModeSplit: String
    val reportsTotal: String
    val reportsWeeklyTrend: String

    val udhaarDebtorsOnly: String
    val udhaarNoCustomerFound: String
    val udhaarSaveCustomer: String
    val udhaarCustomerNameRequired: String
    val udhaarCustomerAdded: (String) -> String
    val udhaarPhoneLabel: String
    val udhaarLedgerHistory: String
    val udhaarNoTransactions: String
    val udhaarCreditGiven: String
    val udhaarPaymentReceived: String
    val udhaarCorrectionReversal: String
    val udhaarInvalidEntry: String
    val udhaarReceivePaymentTitle: String
    val udhaarReceivedAmountLabel: String
    val udhaarNoteModeLabel: String
    val udhaarNoteModePlaceholder: String
    val udhaarSavePayment: String
    val udhaarCorrectEntry: String
    val udhaarCorrectAmount: String
    val udhaarReasonRequired: String
    val udhaarReverse: String
    val udhaarCorrect: String

    val setupShopNameRequired: String
    val setupShopNameHint: String
    val setupPhoneInvalid: String
    val setupPinInvalid: String
    val setupCompleted: String

    val productFormSellingPriceShort: String
    val productFormLowStockCount: (Int) -> String
    val productFormWhatsApp: String
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
    override val statusValidating = "जाँच हो रही है"
    override val statusSavingLocally = "डिवाइस पर सुरक्षित हो रहा है"
    override val statusSavedLocally = "डिवाइस पर सुरक्षित हो गया"
    override val statusSavedLocallyDetail = "डेटा इस डिवाइस पर सुरक्षित है। इंटरनेट मिलने पर सिंक अपने आप जारी रहेगा।"
    override val statusSyncing = "क्लाउड सिंक हो रहा है"
    override val statusSuccess = "काम सफल रहा"
    override val statusValidationError = "जानकारी जाँचें"
    override val statusAuthError = "पहचान सत्यापित करें"
    override val statusRetryableError = "अभी पूरा नहीं हुआ"
    override val statusConflict = "डेटा में टकराव है"
    override val statusFailure = "काम पूरा नहीं हो सका"
    override val retryAction = "फिर प्रयास करें"
    override val dismissAction = "बंद करें"
    override val checkoutCreditLimitError = "उधार की सीमा पूरी हो गई। बिल सुरक्षित नहीं हुआ।"
    override val checkoutStockError = "स्टॉक पर्याप्त नहीं है। बिल सुरक्षित नहीं हुआ।"
    override val checkoutCustomerError = "ग्राहक की सही जानकारी जरूरी है। बिल सुरक्षित नहीं हुआ।"
    override val checkoutValidationError = "बिल की जानकारी जाँचें। बिल सुरक्षित नहीं हुआ।"
    override val checkoutSaveError = "बिल सुरक्षित नहीं हो सका। फिर प्रयास करें।"
    override val paymentUpdateError = "भुगतान की स्थिति अपडेट नहीं हो सकी। फिर प्रयास करें।"
    override val backupSuccess = "क्लाउड बैकअप सफल रहा।"
    override val restoreSuccess = "क्लाउड से डेटा सफलतापूर्वक रिस्टोर हो गया।"

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
    override val addNewCategory = "＋ नई कैटेगरी"
    override val noCategories = "कोई कैटेगरी नहीं"
    override val noReasonsAvailable = "कोई कारण उपलब्ध नहीं"
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
    override val productFormBack = "वापस"
    override val productFormBarcodeLabel = "बारकोड (वैकल्पिक)"
    override val productFormBarcodePlaceholder = "EAN / UPC / QR कोड"
    override val productFormScanBarcode = "बारकोड स्कैन करें"
    override val productFormAddCategoryTitle = "नई कैटेगरी जोड़ें"
    override val productFormCategoryName = "कैटेगरी का नाम"
    override val productFormAddCategory = "जोड़ें"
    override val productFormUnitLabel = "इकाई"
    override val productFormUnitHint = "जैसे pcs, kg, g या litre"
    override val productFormTrackStockHint = "बिल बनने पर स्टॉक अपने आप घटेगा"
    override val productFormActiveLabel = "सामान सक्रिय है"
    override val productFormActiveHint = "बंद करने पर यह सामान बिलिंग में नहीं दिखेगा"
    override val productFormMoneyHint = "रकम रुपये में लिखें; 10.50 या 10,50 दोनों चलेंगे।"
    override val productFormQuantityHint = "मात्रा खाली या गलत नहीं हो सकती।"
    override val productFormRequiredField: (String) -> String = { field -> "$field आवश्यक है" }
    override val productFormInvalidNumber: (String) -> String = { field -> "$field की संख्या सही नहीं है" }
    override val productFormInvalidField: (String) -> String = { field -> "$field सही नहीं है" }
    override val productFormNegativeNumber: (String) -> String = { field -> "$field ऋणात्मक नहीं हो सकता" }
    override val productFormNonPositiveMrp = "MRP शून्य से अधिक होना चाहिए"
    override val productFormFractionNotAllowed: (String) -> String = { field -> "$field में पूरी संख्या लिखें" }
    override val productFormCategoryRequired = "सक्रिय कैटेगरी चुनें"
    override val productFormBarcodeInvalid = "बारकोड में स्पेस नहीं होना चाहिए और वह 128 अक्षरों से छोटा हो"
    override val productFormBarcodeSet: (String) -> String = { code -> "बारकोड दर्ज हुआ: $code" }
    override val productFormChooseCategory = "कैटेगरी चुनें"
    override val productFormFastAddPrefix = "त्वरित जोड़ें"
    override val productFormAddItem = "सामान जोड़ें"
    override val productFormRecentlyAdded = "हाल में जोड़ा गया सामान"
    override val productFormTotal = "कुल"
    override val productFormNoItemsInCategory = "इस कैटेगरी में अभी कोई सामान नहीं है।"
    override val productFormPrice = "कीमत"
    override val productFormStock = "स्टॉक"
    override val productFormUntracked = "ट्रैक नहीं हो रहा"
    override val productFormNoProducts = "अभी कोई सामान उपलब्ध नहीं है।"
    override val productFormInactive = "निष्क्रिय"
    override val productFormLowStock: (String) -> String = { stock -> "कम स्टॉक: $stock बचा है" }
    override val productFormStockAvailable: (String) -> String = { stock -> "स्टॉक: $stock उपलब्ध" }
    override val productFormStockUntracked = "स्टॉक ट्रैक नहीं हो रहा"
    override val productFormAdjustStock = "स्टॉक सुधारें"
    override val productFormSaveAndAdd = "सेव करें और बिल में जोड़ें"
    override val productFormCategoryManager = "कैटेगरी प्रबंधन"
    override val productFormNewCategoryPlaceholder = "नई कैटेगरी का नाम"
    override val productFormRenameCategory = "कैटेगरी का नाम बदलें"
    override val productFormUpdateCategory = "बदलें"
    override val productFormDone = "पूर्ण"
    override val productFormNoLowStock = "कोई सामान कम नहीं है"
    override val productFormStockHealthy = "दुकान में पर्याप्त स्टॉक मौजूद है।"
    override val productFormAddStock = "माल जोड़ें"
    override val productFormLeft: (String) -> String = { stock -> "$stock बचा है" }
    override val productFormClose = "बंद करें"
    override val productFormPlusOneStock = "स्टॉक में 1 जोड़ें"
    override val productFormCopy = "कॉपी"
    override val productFormBarcodeScanned: (String) -> String = { code -> "बारकोड स्कैन हुआ: $code" }
    override val productFormLowStockBanner: (Int) -> String = { count -> "कम स्टॉक चेतावनी: $count सामान" }
    override val productFormReorderListHint = "थोक खरीदारी लिस्ट देखें"
    override val productFormOrderList = "ऑर्डर लिस्ट"
    override val stockAdjustmentCurrentStock: (String) -> String = { stock -> "वर्तमान स्टॉक: $stock" }
    override val stockAdjustmentEnterPhysicalCount = "दुकान में मौजूद वास्तविक स्टॉक संख्या दर्ज करें"
    override val stockAdjustmentReason = "कारण"
    override val stockAdjustmentSave = "स्टॉक सुधार सुरक्षित करें"
    override val stockAdjustmentHistory = "स्टॉक सुधार इतिहास"
    override val stockAdjustmentStockTransition: (String, String) -> String = { old, new -> "स्टॉक: $old ➔ $new" }
    override val stockAdjustmentProductNotFound = "यह सामान नहीं मिला या अब सक्रिय नहीं है। वापस जाकर दूसरा सामान चुनें।"
    override val stockReasonCountCorrection = "स्टॉक मिलान"
    override val stockReasonPurchaseAdded = "नया माल आया"
    override val stockReasonDamaged = "खराब या एक्सपायर सामान"
    override val stockReasonOpening = "ओपनिंग स्टॉक प्रविष्टि"
    override val stockReasonOther = "अन्य"

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

    override val settingsAccountSection = "खाता और स्टोर पहचान"
    override val settingsAccountProvider = "खाता प्रदाता"
    override val settingsProviderFirebase = "Google / Firebase"
    override val settingsProviderLocal = "स्थानीय डिवाइस खाता"
    override val settingsNotAssigned = "अभी निर्धारित नहीं"
    override val settingsStoreIdentity = "स्टोर पहचान"
    override val settingsTenantIdentity = "स्टोर कार्यक्षेत्र"
    override val settingsSignedIn = "साइन इन है"
    override val settingsSignedOut = "साइन इन नहीं है"
    override val settingsCloudSignInRequired = "सुरक्षित क्लाउड सिंक और बैकअप के लिए Google से साइन इन करें।"
    override val settingsShopProfileHint = "यह जानकारी बिल, रसीद और स्टोर पहचान में उपयोग होती है।"
    override val settingsSecurityHint = "ऐप लॉक डिवाइस पर ही सुरक्षित रहता है। PIN या बायोमेट्रिक जानकारी क्लाउड पर नहीं भेजी जाती।"
    override val settingsAppLockOnHint = "ऐप बंद होने या कुछ समय निष्क्रिय रहने पर PIN माँगा जाएगा।"
    override val settingsAppLockOffHint = "ऐप लॉक बंद है। दुकान साझा डिवाइस पर हो तो इसे चालू रखें।"
    override val settingsSetPinHint = "PIN बदलने के लिए नया सुरक्षित 4-अंकों का PIN और पुष्टि दर्ज करें।"
    override val settingsPinConfirmLabel = "PIN की पुष्टि करें"
    override val settingsPinMismatch = "दोनों PIN एक जैसे होने चाहिए।"
    override val settingsBiometricAvailable = "इस डिवाइस पर बायोमेट्रिक उपलब्ध है।"
    override val settingsBiometricUnavailable = "इस डिवाइस पर बायोमेट्रिक उपलब्ध नहीं है; PIN का उपयोग करें।"
    override val settingsLanguageHint = "बिल, स्टॉक, उधार और रिपोर्ट की भाषा चुनें।"
    override val settingsAppearanceSection = "दिखावट और पहुँच"
    override val settingsAppearanceHint = "भाषा नियंत्रण उपलब्ध है। अलग theme, font-size या accessibility presets अभी उपलब्ध नहीं हैं।"
    override val settingsAutomaticSync = "स्वचालित सिंक और बैकअप"
    override val settingsAutomaticSyncHint = "इंटरनेट मिलते ही लोकल बदलाव सिंक होंगे और सुरक्षित snapshot बैकअप बनेगा।"
    override val settingsManualSync = "अभी सिंक करें"
    override val settingsCloudBackupTitle = "क्लाउड बैकअप और रिस्टोर"
    override val settingsCloudBackupHint = "साइन इन खाते के साथ सुरक्षित डेटा कॉपी और रिकवरी।"
    override val settingsBackupNow = "अभी बैकअप लें"
    override val settingsRestore = "रिस्टोर करें"
    override val settingsRestoreWarningTitle = "डेटा बदलने की चेतावनी"
    override val settingsRestoreWarningMessage = "क्लाउड snapshot आपके डिवाइस के cloud-owned डेटा को बदलेगा। पहले snapshot जाँचा जाएगा और recovery point बनाया जाएगा।"
    override val settingsRestoreConfirmAction = "हाँ, रिस्टोर करें"
    override val settingsBillingSection = "बिलिंग और भुगतान"
    override val settingsManualUpiSettlement = "UPI भुगतान की पुष्टि"
    override val settingsManualUpiSettlementHint = "UPI भुगतान को बिल में दर्ज करें और वास्तविक settlement अपने बैंक/UPI ऐप में मैन्युअली जाँचें।"
    override val settingsInventorySection = "स्टॉक और सामान"
    override val settingsInventoryHint = "स्टॉक ट्रैकिंग, कम-स्टॉक सीमा और इकाई अभी सामान के स्तर पर सेट होती है। ग्लोबल डिफॉल्ट उपलब्ध नहीं है।"
    override val settingsCustomersSection = "ग्राहक और उधार"
    override val settingsCustomersHint = "उधार सीमा और भुगतान ग्राहक खाते के अनुसार लागू होती है। ग्लोबल रिमाइंडर डिफॉल्ट अभी उपलब्ध नहीं है।"
    override val settingsSyncSection = "सिंक और बैकअप"
    override val settingsSyncHealth = "सिंक स्वास्थ्य"
    override val settingsLastAttempt = "अंतिम प्रयास"
    override val settingsLastSuccess = "अंतिम सफल पुष्टि"
    override val settingsLastAttemptUnavailable = "अलग अंतिम प्रयास समय अभी दर्ज नहीं है।"
    override val settingsHealthHealthy = "सिंक ठीक है"
    override val settingsHealthNever = "हाल की सफल सिंक उपलब्ध नहीं"
    override val settingsHealthPending = "स्थानीय बदलाव सिंक की प्रतीक्षा में हैं"
    override val settingsHealthRetrying = "सिंक फिर प्रयास कर रहा है"
    override val settingsHealthBlocked = "कुछ बदलावों की समीक्षा जरूरी है"
    override val settingsPending = "बाकी बदलाव"
    override val settingsRetryable = "फिर प्रयास योग्य"
    override val settingsDeadLetter = "समीक्षा जरूरी"
    override val settingsConflicts = "टकराव"
    override val settingsDataPrivacySection = "डेटा और गोपनीयता"
    override val settingsDataPrivacyHint = "पासवर्ड, PIN verifier और session secrets डिवाइस से बाहर नहीं जाते। Restore से पहले snapshot जाँच और recovery point बनाया जाता है।"
    override val settingsSupportSection = "सहायता और जानकारी"
    override val settingsVersion = "ऐप संस्करण"
    override val settingsSupportHint = "समस्या होने पर ऐप संस्करण और बिना संवेदनशील डेटा के स्थिति साझा करें।"
    override val settingsSaveHint = "स्थानीय सेटिंग्स पहले डिवाइस पर सुरक्षित होंगी। क्लाउड बैकअप अलग कार्रवाई है।"
    override val settingsSavedLocally = "सेटिंग्स इस डिवाइस पर सुरक्षित हो गईं।"
    override val settingsSaveValidationShopName = "दुकान का नाम आवश्यक है।"
    override val settingsSaveValidationPin = "नया PIN सुरक्षित 4-अंकों का होना चाहिए।"
    override val settingsSaveValidationPinConfirm = "PIN की पुष्टि करें।"
    override val settingsDisableLockTitle = "ऐप लॉक बंद करें?"
    override val settingsDisableLockMessage = "ऐप लॉक बंद करने पर इस डिवाइस पर दुकान खोलने के लिए PIN नहीं माँगा जाएगा। क्या आप जारी रखना चाहते हैं?"
    override val settingsConfirmDisable = "लॉक बंद करें"
    override val settingsKeepLock = "लॉक चालू रखें"
    override val settingsLockDraftNotice = "ऐप लॉक बंद करने का विकल्प चुना गया है। बदलाव लागू करने के लिए नीचे 'सेटिंग्स सेव करें' दबाएं।"
    override val settingsOwnerPhonePlaceholder = "9876543210"
    override val settingsPinPlaceholder = "4 अंकों का PIN"
    override val settingsBack = "वापस"
    override val settingsUserAvatar = "यूज़र प्रोफ़ाइल"
    override val settingsSignIn = "साइन इन करें"
    override val settingsWelcomeChantHint = "ऐप शुरू होने पर 'जय श्री श्याम' भजन बजाएं।"
    override val settingsQrPreview = "अपलोड किए गए QR का प्रीव्यू"
    override val settingsChangeQr = "QR फोटो बदलें"
    override val settingsNoQrSelected = "कोई QR नहीं चुना गया"
    override val settingsSyncNote = "क्लाउड बैकअप आपके स्टोर खाते के साथ सुरक्षित डेटा कॉपी रखता है।"
    override val settingsLastSyncLabel = "अंतिम सिंक"
    override val settingsSyncStatusSuccess = "स्थिति: सिंक सफल"
    override val settingsSyncStatusNoChanges = "स्थिति: कोई नया बदलाव नहीं"
    override val settingsSyncStatusFailed = "स्थिति: सिंक अधूरा है; फिर प्रयास होगा"
    override val settingsSyncStatusUnavailable = "स्थिति: अभी उपलब्ध नहीं"

    override val commonLanguage = "भाषा"
    override val commonBack = "वापस"
    override val commonClose = "बंद करें"
    override val commonOkay = "ठीक है"
    override val commonDelete = "हटाएँ"
    override val commonBiometric = "बायोमेट्रिक"
    override val commonSuccess = "सफल"
    override val commonScanBarcode = "बारकोड स्कैन करें"
    override val commonQuickAdd = "त्वरित जोड़ें"
    override val commonReorderList = "री-ऑर्डर लिस्ट"
    override val commonExportStock = "स्टॉक CSV निर्यात करें"
    override val commonManageCategories = "कैटेगरी प्रबंधित करें"
    override val commonExportSales = "बिक्री CSV निर्यात करें"
    override val commonExportUdhaar = "उधार CSV निर्यात करें"
    override val commonSeeItems = "सामान देखें"
    override val commonAddToBasket = "कार्ट में जोड़ें"
    override val commonRemoveFromBasket = "कार्ट से हटाएँ"
    override val commonAdd = "जोड़ें"
    override val commonRemove = "हटाएँ"
    override val commonLedgerActions = "खाता विकल्प"
    override val commonStaticPaytmQr = "दुकान का स्थिर Paytm QR"
    override val commonNoQrConfigured = "QR सेट नहीं है"
    override val commonWhatsApp = "WhatsApp"
    override val commonCustomer = "ग्राहक"
    override val scannerTitle = "बारकोड / QR कोड स्कैन करें"
    override val scannerSubtitle = "कैमरे को बारकोड के सामने रखें"
    override val scannerCameraPermissionTitle = "कैमरा अनुमति आवश्यक है"
    override val scannerCameraPermissionMessage = "बारकोड स्कैन करने के लिए कृपया कैमरा एक्सेस की अनुमति दें।"
    override val scannerAllow = "अनुमति दें"
    override val scannerToggleTorch = "फ्लैशलाइट चालू या बंद करें"
    override val scannerCancel = "रद्द करें"

    override val languageEnglishCode = "EN"
    override val languageHindiCode = "हिंदी"
    override val authLoginSuccess: (String) -> String = { name -> "लॉगिन सफल! $name".trim() }
    override val authGoogleSignInFailed = "Google साइन इन विफल हुआ"
    override val authLocalPinTitle = "लोकल अकाउंट से पिन बदलें"
    override val authLocalPinHelp = "Firebase ईमेल रीसेट लोकल अकाउंट के डिवाइस PIN को नहीं बदलता। अपने लोकल अकाउंट का पासवर्ड और नया PIN दर्ज करें।"
    override val authGooglePinTitle = "Google से PIN सत्यापित करें"
    override val authGooglePinHelp = "आपका ऐप लॉक PIN डिवाइस पर रहता है। Google account को दोबारा verify करने के बाद नया PIN सेट होगा; कोई password-reset email नहीं भेजी जाएगी।"
    override val authSwitchAccountHelp = "कोई active account उपलब्ध नहीं है। पहले सही account से sign in करें।"
    override val authUsernameOrEmail = "Username या Email"
    override val authLocalPassword = "लोकल पासवर्ड"
    override val authAccount: (String) -> String = { account -> "Account: $account" }
    override val authGoogleAccount = "Google account"
    override val authNewPin = "नया 4-अंकों का PIN"
    override val authPinChanged = "PIN बदल गया।"
    override val authChangePin = "PIN बदलें"
    override val authVerifyGoogle = "Google से सत्यापित करें"
    override val authSignIn = "Sign in करें"

    override val billingProductNotFound = "कोई सामान नहीं मिला!"
    override val billingLowStock: (String) -> String = { stock -> "कम स्टॉक: $stock बचा है" }
    override val billingStockAvailable: (String) -> String = { stock -> "स्टॉक: $stock उपलब्ध" }
    override val billingStockUntracked = "स्टॉक अनट्रैक्ड"
    override val billingCartSummary: (String) -> String = { quantity -> "थैला: $quantity सामान" }
    override val billingInsufficientStockTitle = "पर्याप्त स्टॉक नहीं है"
    override val billingInsufficientStockMessage: (String, String) -> String = { name, stock -> "प्रोडक्ट '$name' का उपलब्ध स्टॉक $stock है। इससे अधिक मात्रा का बिल नहीं बनाया जा सकता।" }
    override val billingProductAdded: (String) -> String = { name -> "✓ $name बिल में जोड़ा गया" }
    override val billingBarcodeNotFound: (String) -> String = { code -> "बारकोड: $code (उत्पाद नहीं मिला - नया उत्पाद जोड़ें)" }
    override val billingPayableAmount = "कुल चुकाने योग्य राशि"
    override val billingReceivedCashUpi = "प्राप्त राशि (Cash / UPI)"
    override val billingQrPrompt = "ग्राहक से कहें: 'QR स्कैन करके राशि दर्ज करें।'"
    override val billingNoQrTitle = "UPI QR सेट नहीं है"
    override val billingNoQrMessage = "सेटिंग्स में जाकर दुकान का Paytm Business QR लगाएं।"
    override val billingUpiPaid = "UPI भुगतान"
    override val billingSelectUdhaarCustomer = "उधार ग्राहक चुनें"
    override val billingNoCustomerFound = "कोई ग्राहक नहीं मिला।"
    override val billingAddCustomer: (String) -> String = { name -> "+ नया जोड़ें: '$name'" }
    override val billingConfirmUdhaar = "खाता खोलें और उधार लिखें"
    override val billingBillNumber = "बिल नंबर:"
    override val billingPaymentMode = "भुगतान माध्यम:"
    override val billingPaymentStatus = "भुगतान स्थिति:"
    override val billingReceived = "प्राप्त राशि:"
    override val billingChange = "वापसी:"
    override val billingCopyInvoice = "बिल कॉपी करें"
    override val billingHistory = "इतिहास"

    override val homeCloudSyncTriggered = "⚡ क्लाउड सिंक शुरू हुआ"
    override val homeCloudBackupActive = "क्लाउड बैकअप सक्रिय ⚡"
    override val homeBillsCreated: (Int) -> String = { count -> "$count बिल बने" }
    override val homeRestockNeeded: (Int) -> String = { count -> "$count सामान जल्द ख़त्म होने वाले हैं।" }
    override val homeStockSafe = "दुकान का स्टॉक सुरक्षित है"
    override val homeStockSufficient = "सभी जरूरी सामान पर्याप्त मात्रा में उपलब्ध हैं।"
    override val homeBillingSubtitle = "नकद / UPI / उधार"
    override val homeInventorySubtitle = "स्टॉक एंट्री"
    override val homeUdhaarSubtitle = "खाता व WhatsApp तगादा"
    override val homeReportsSubtitle = "दैनिक व मासिक बिक्री"

    override val reportsTotalSalesTitle = "कुल बिक्री"
    override val reportsBillsGenerated: (Int) -> String = { count -> "$count बिल बनाए गए" }
    override val reportsHistoryTitle = "बिक्री का इतिहास:"
    override val reportsNoRecords = "कोई रिकॉर्ड नहीं मिला!"
    override val reportsUdhaarCustomer = "उधार ग्राहक"
    override val reportsBillNumber = "बिल नं:"
    override val reportsCustomer = "ग्राहक:"
    override val reportsPaymentMode = "भुगतान माध्यम:"
    override val reportsCopy = "कॉपी"
    override val reportsClose = "बंद करें"
    override val reportsNoChartData = "वितरण चार्ट के लिए कोई बिक्री डेटा उपलब्ध नहीं है।"
    override val reportsPaymentModeSplit = "भुगतान माध्यम वितरण"
    override val reportsTotal = "कुल बिक्री"
    override val reportsWeeklyTrend = "पिछले 7 दिनों की बिक्री"

    override val udhaarDebtorsOnly = "बकाया वाले"
    override val udhaarNoCustomerFound = "कोई ग्राहक नहीं मिला!"
    override val udhaarSaveCustomer = "खाता खोलें"
    override val udhaarCustomerNameRequired = "ग्राहक का नाम आवश्यक है!"
    override val udhaarCustomerAdded: (String) -> String = { name -> "$name खाता खुल गया!" }
    override val udhaarPhoneLabel = "मोबाइल:"
    override val udhaarLedgerHistory = "लेन-देन इतिहास:"
    override val udhaarNoTransactions = "इस खाते में कोई लेन-देन इतिहास नहीं है।"
    override val udhaarCreditGiven = "उधार दिया"
    override val udhaarPaymentReceived = "रकम प्राप्त हुई"
    override val udhaarCorrectionReversal = "सुधार / रिवर्सल"
    override val udhaarInvalidEntry = "अमान्य प्रविष्टि"
    override val udhaarReceivePaymentTitle = "जमा राशि दर्ज करें"
    override val udhaarReceivedAmountLabel = "प्राप्त रकम *"
    override val udhaarNoteModeLabel = "टिप्पणी / माध्यम (वैकल्पिक)"
    override val udhaarNoteModePlaceholder = "उदा. नकद, UPI, Paytm..."
    override val udhaarSavePayment = "जमा सुरक्षित करें"
    override val udhaarCorrectEntry = "लेजर सुधार"
    override val udhaarCorrectAmount = "सही रकम *"
    override val udhaarReasonRequired = "कारण *"
    override val udhaarReverse = "रिवर्स"
    override val udhaarCorrect = "सुधार"

    override val setupShopNameRequired = "दुकान का नाम आवश्यक है!"
    override val setupShopNameHint = "यह नाम ग्राहकों के बिल पर दिखेगा।"
    override val setupPhoneInvalid = "कृपया सही 10 अंकों का मोबाइल नंबर दर्ज करें"
    override val setupPinInvalid = "कृपया सुरक्षित 4-अंकों का PIN दर्ज करें"
    override val setupCompleted = "दुकान सेटअप पूरा हुआ!"

    override val productFormSellingPriceShort = "बिक्री"
    override val productFormLowStockCount: (Int) -> String = { count -> "कुल $count सामान कम हैं" }
    override val productFormWhatsApp = "WhatsApp"
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
    override val statusValidating = "Checking details"
    override val statusSavingLocally = "Saving on this device"
    override val statusSavedLocally = "Saved on this device"
    override val statusSavedLocallyDetail = "Your data is safe on this device. Sync will continue automatically when the internet is available."
    override val statusSyncing = "Syncing to cloud"
    override val statusSuccess = "Completed successfully"
    override val statusValidationError = "Check the details"
    override val statusAuthError = "Sign in to continue"
    override val statusRetryableError = "Not completed yet"
    override val statusConflict = "Data conflict needs attention"
    override val statusFailure = "Could not complete"
    override val retryAction = "Try again"
    override val dismissAction = "Dismiss"
    override val checkoutCreditLimitError = "Udhaar credit limit exceeded. Bill was not saved."
    override val checkoutStockError = "Insufficient stock. Bill was not saved."
    override val checkoutCustomerError = "Valid customer details are required. Bill was not saved."
    override val checkoutValidationError = "Check the bill details. Bill was not saved."
    override val checkoutSaveError = "Bill could not be saved. Please try again."
    override val paymentUpdateError = "Payment state could not be updated. Please try again."
    override val backupSuccess = "Cloud backup completed successfully."
    override val restoreSuccess = "Data was restored from cloud successfully."

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
    override val addNewCategory = "＋ New Category"
    override val noCategories = "No categories yet"
    override val noReasonsAvailable = "No reasons available"
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
    override val productFormBack = "Back"
    override val productFormBarcodeLabel = "Barcode (optional)"
    override val productFormBarcodePlaceholder = "EAN / UPC / QR code"
    override val productFormScanBarcode = "Scan barcode"
    override val productFormAddCategoryTitle = "Add new category"
    override val productFormCategoryName = "Category name"
    override val productFormAddCategory = "Add"
    override val productFormUnitLabel = "Unit"
    override val productFormUnitHint = "For example, pcs, kg, g, or litre"
    override val productFormTrackStockHint = "Stock reduces automatically when billing"
    override val productFormActiveLabel = "Product is active"
    override val productFormActiveHint = "Inactive products are hidden from billing"
    override val productFormMoneyHint = "Enter rupee amounts; both 10.50 and 10,50 are accepted."
    override val productFormQuantityHint = "Quantity cannot be blank or malformed."
    override val productFormRequiredField: (String) -> String = { field -> "$field is required" }
    override val productFormInvalidNumber: (String) -> String = { field -> "$field is not a valid number" }
    override val productFormInvalidField: (String) -> String = { field -> "$field is not valid" }
    override val productFormNegativeNumber: (String) -> String = { field -> "$field cannot be negative" }
    override val productFormNonPositiveMrp = "MRP must be greater than zero"
    override val productFormFractionNotAllowed: (String) -> String = { field -> "Enter a whole number for $field" }
    override val productFormCategoryRequired = "Choose an active category"
    override val productFormBarcodeInvalid = "Barcode must contain no spaces and be 128 characters or shorter"
    override val productFormBarcodeSet: (String) -> String = { code -> "Barcode set: $code" }
    override val productFormChooseCategory = "Choose category"
    override val productFormFastAddPrefix = "Fast add"
    override val productFormAddItem = "Add product"
    override val productFormRecentlyAdded = "Recently added items"
    override val productFormTotal = "Total"
    override val productFormNoItemsInCategory = "No products in this category yet."
    override val productFormPrice = "Price"
    override val productFormStock = "Stock"
    override val productFormUntracked = "Not tracked"
    override val productFormNoProducts = "No products are available yet."
    override val productFormInactive = "Inactive"
    override val productFormLowStock: (String) -> String = { stock -> "Low stock: $stock left" }
    override val productFormStockAvailable: (String) -> String = { stock -> "Stock: $stock available" }
    override val productFormStockUntracked = "Stock is not tracked"
    override val productFormAdjustStock = "Adjust stock"
    override val productFormSaveAndAdd = "Save and add to bill"
    override val productFormCategoryManager = "Category management"
    override val productFormNewCategoryPlaceholder = "New category name"
    override val productFormRenameCategory = "Rename category"
    override val productFormUpdateCategory = "Update"
    override val productFormDone = "Done"
    override val productFormNoLowStock = "No low-stock products"
    override val productFormStockHealthy = "Store inventory is well stocked."
    override val productFormAddStock = "Add stock"
    override val productFormLeft: (String) -> String = { stock -> "$stock left" }
    override val productFormClose = "Close"
    override val productFormPlusOneStock = "Add 1 to stock"
    override val productFormCopy = "Copy"
    override val productFormBarcodeScanned: (String) -> String = { code -> "Barcode scanned: $code" }
    override val productFormLowStockBanner: (Int) -> String = { count -> "Low-stock warning: $count items" }
    override val productFormReorderListHint = "View wholesale reorder list"
    override val productFormOrderList = "Order list"
    override val stockAdjustmentCurrentStock: (String) -> String = { stock -> "Current stock: $stock" }
    override val stockAdjustmentEnterPhysicalCount = "Enter the actual physical stock count"
    override val stockAdjustmentReason = "Reason"
    override val stockAdjustmentSave = "Save stock adjustment"
    override val stockAdjustmentHistory = "Stock adjustment history"
    override val stockAdjustmentStockTransition: (String, String) -> String = { old, new -> "Stock: $old ➔ $new" }
    override val stockAdjustmentProductNotFound = "This product was not found or is no longer active. Go back and choose another product."
    override val stockReasonCountCorrection = "Stock count correction"
    override val stockReasonPurchaseAdded = "Purchase added"
    override val stockReasonDamaged = "Damaged or expired"
    override val stockReasonOpening = "Opening stock entry"
    override val stockReasonOther = "Other"

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

    override val settingsAccountSection = "Account & store identity"
    override val settingsAccountProvider = "Account provider"
    override val settingsProviderFirebase = "Google / Firebase"
    override val settingsProviderLocal = "Local device account"
    override val settingsNotAssigned = "Not assigned yet"
    override val settingsStoreIdentity = "Store identity"
    override val settingsTenantIdentity = "Store workspace"
    override val settingsSignedIn = "Signed in"
    override val settingsSignedOut = "Not signed in"
    override val settingsCloudSignInRequired = "Sign in with Google for secure cloud sync and backup."
    override val settingsShopProfileHint = "This information is used on bills, receipts, and store identity."
    override val settingsSecurityHint = "App-lock data stays on this device. PIN and biometric information are never sent to the cloud."
    override val settingsAppLockOnHint = "The app will ask for your PIN after it is closed or inactive."
    override val settingsAppLockOffHint = "App lock is off. Keep it on when the shop uses a shared device."
    override val settingsSetPinHint = "To change the PIN, enter a new safe 4-digit PIN and confirm it."
    override val settingsPinConfirmLabel = "Confirm PIN"
    override val settingsPinMismatch = "Both PIN entries must match."
    override val settingsBiometricAvailable = "Biometric unlock is available on this device."
    override val settingsBiometricUnavailable = "Biometric unlock is unavailable on this device; use the PIN."
    override val settingsLanguageHint = "Choose the language for billing, stock, udhaar, and reports."
    override val settingsAppearanceSection = "Appearance & accessibility"
    override val settingsAppearanceHint = "Language control is available. Separate theme, font-size, or accessibility presets are not available yet."
    override val settingsAutomaticSync = "Automatic sync & backup"
    override val settingsAutomaticSyncHint = "When online, local changes sync and a protected snapshot backup is created."
    override val settingsManualSync = "Sync now"
    override val settingsCloudBackupTitle = "Cloud backup & restore"
    override val settingsCloudBackupHint = "Secure data copy and recovery with the signed-in account."
    override val settingsBackupNow = "Back up now"
    override val settingsRestore = "Restore"
    override val settingsRestoreWarningTitle = "Data replacement warning"
    override val settingsRestoreWarningMessage = "The cloud snapshot will replace cloud-owned data on this device. The snapshot will be validated and a recovery point will be created first."
    override val settingsRestoreConfirmAction = "Yes, restore"
    override val settingsBillingSection = "Billing & payments"
    override val settingsManualUpiSettlement = "UPI payment confirmation"
    override val settingsManualUpiSettlementHint = "Record UPI payment in the bill and verify actual settlement manually in your bank or UPI app."
    override val settingsInventorySection = "Stock & products"
    override val settingsInventoryHint = "Stock tracking, low-stock limits, and units are set per product. Global defaults are not available yet."
    override val settingsCustomersSection = "Customers & udhaar"
    override val settingsCustomersHint = "Credit limits and payments are enforced per customer. A global reminder default is not available yet."
    override val settingsSyncSection = "Sync & backup"
    override val settingsSyncHealth = "Sync health"
    override val settingsLastAttempt = "Last attempt"
    override val settingsLastSuccess = "Last successful acknowledgement"
    override val settingsLastAttemptUnavailable = "A separate last-attempt time is not recorded yet."
    override val settingsHealthHealthy = "Sync is healthy"
    override val settingsHealthNever = "No recent successful sync"
    override val settingsHealthPending = "Local changes are waiting to sync"
    override val settingsHealthRetrying = "Sync is retrying"
    override val settingsHealthBlocked = "Some changes need review"
    override val settingsPending = "Pending changes"
    override val settingsRetryable = "Retryable"
    override val settingsDeadLetter = "Needs review"
    override val settingsConflicts = "Conflicts"
    override val settingsDataPrivacySection = "Data & privacy"
    override val settingsDataPrivacyHint = "Passwords, PIN verifiers, and session secrets stay on the device. Restore validates the snapshot and creates a recovery point first."
    override val settingsSupportSection = "Support & information"
    override val settingsVersion = "App version"
    override val settingsSupportHint = "When reporting a problem, share the app version and redacted status only."
    override val settingsSaveHint = "Settings are saved on this device first. Cloud backup is a separate action."
    override val settingsSavedLocally = "Settings saved on this device."
    override val settingsSaveValidationShopName = "Shop name is required."
    override val settingsSaveValidationPin = "The new PIN must be a safe 4-digit PIN."
    override val settingsSaveValidationPinConfirm = "Confirm the new PIN."
    override val settingsDisableLockTitle = "Turn off app lock?"
    override val settingsDisableLockMessage = "With app lock off, this device will not ask for a PIN before opening the shop. Continue?"
    override val settingsConfirmDisable = "Turn off lock"
    override val settingsKeepLock = "Keep lock on"
    override val settingsLockDraftNotice = "Turning off app lock is selected. Tap 'Save settings' below to apply this change."
    override val settingsOwnerPhonePlaceholder = "9876543210"
    override val settingsPinPlaceholder = "4-digit PIN"
    override val settingsBack = "Back"
    override val settingsUserAvatar = "User profile"
    override val settingsSignIn = "Sign in"
    override val settingsWelcomeChantHint = "Play devotional greeting on app launch."
    override val settingsQrPreview = "Uploaded QR preview"
    override val settingsChangeQr = "Change QR photo"
    override val settingsNoQrSelected = "No QR selected"
    override val settingsSyncNote = "Cloud backup keeps a protected copy with your store account."
    override val settingsLastSyncLabel = "Last sync"
    override val settingsSyncStatusSuccess = "Status: Sync completed"
    override val settingsSyncStatusNoChanges = "Status: No new changes"
    override val settingsSyncStatusFailed = "Status: Sync incomplete; retry will continue"
    override val settingsSyncStatusUnavailable = "Status: Not available yet"

    override val commonLanguage = "Language"
    override val commonBack = "Back"
    override val commonClose = "Close"
    override val commonOkay = "OK"
    override val commonDelete = "Delete"
    override val commonBiometric = "Biometric"
    override val commonSuccess = "Success"
    override val commonScanBarcode = "Scan barcode"
    override val commonQuickAdd = "Quick add"
    override val commonReorderList = "Reorder list"
    override val commonExportStock = "Export stock CSV"
    override val commonManageCategories = "Manage categories"
    override val commonExportSales = "Export sales CSV"
    override val commonExportUdhaar = "Export Udhaar CSV"
    override val commonSeeItems = "See items"
    override val commonAddToBasket = "Add to cart"
    override val commonRemoveFromBasket = "Remove from cart"
    override val commonAdd = "Add"
    override val commonRemove = "Remove"
    override val commonLedgerActions = "Ledger actions"
    override val commonStaticPaytmQr = "Static shop Paytm QR"
    override val commonNoQrConfigured = "No QR configured"
    override val commonWhatsApp = "WhatsApp"
    override val commonCustomer = "Customer"
    override val scannerTitle = "Scan barcode / QR code"
    override val scannerSubtitle = "Place the camera in front of the barcode"
    override val scannerCameraPermissionTitle = "Camera permission required"
    override val scannerCameraPermissionMessage = "Allow camera access to scan a barcode."
    override val scannerAllow = "Allow camera"
    override val scannerToggleTorch = "Toggle flashlight"
    override val scannerCancel = "Cancel"

    override val languageEnglishCode = "EN"
    override val languageHindiCode = "Hindi"
    override val authLoginSuccess: (String) -> String = { name -> "Login successful! $name".trim() }
    override val authGoogleSignInFailed = "Google sign-in failed"
    override val authLocalPinTitle = "Reset PIN with local account"
    override val authLocalPinHelp = "A Firebase email reset cannot change a local account's device PIN. Verify the local account password and choose a new PIN."
    override val authGooglePinTitle = "Verify with Google to reset PIN"
    override val authGooglePinHelp = "Your app-lock PIN stays on this device. Re-verify the same Google account, then choose a new PIN; no password-reset email will be sent."
    override val authSwitchAccountHelp = "No active account is available. Sign in with the correct account first."
    override val authUsernameOrEmail = "Username or email"
    override val authLocalPassword = "Local account password"
    override val authAccount: (String) -> String = { account -> "Account: $account" }
    override val authGoogleAccount = "Google account"
    override val authNewPin = "New 4-digit app PIN"
    override val authPinChanged = "PIN changed successfully."
    override val authChangePin = "Change PIN"
    override val authVerifyGoogle = "Verify with Google"
    override val authSignIn = "Sign in"

    override val billingProductNotFound = "No product found!"
    override val billingLowStock: (String) -> String = { stock -> "Low stock: $stock left" }
    override val billingStockAvailable: (String) -> String = { stock -> "Stock: $stock available" }
    override val billingStockUntracked = "Stock untracked"
    override val billingCartSummary: (String) -> String = { quantity -> "Cart: $quantity items" }
    override val billingInsufficientStockTitle = "Insufficient stock"
    override val billingInsufficientStockMessage: (String, String) -> String = { name, stock -> "Product '$name' has only $stock available. A bill cannot include more than the available stock." }
    override val billingProductAdded: (String) -> String = { name -> "✓ $name added to cart" }
    override val billingBarcodeNotFound: (String) -> String = { code -> "Barcode: $code (Product not found in inventory)" }
    override val billingPayableAmount = "Total payable amount"
    override val billingReceivedCashUpi = "Received amount (Cash / UPI)"
    override val billingQrPrompt = "Ask customer: 'Scan QR and enter exact amount.'"
    override val billingNoQrTitle = "UPI QR not set"
    override val billingNoQrMessage = "Go to Settings to configure the shop Paytm QR."
    override val billingUpiPaid = "UPI paid"
    override val billingSelectUdhaarCustomer = "Select Udhaar customer"
    override val billingNoCustomerFound = "No customer found."
    override val billingAddCustomer: (String) -> String = { name -> "+ Add new: '$name'" }
    override val billingConfirmUdhaar = "Open ledger & confirm Udhaar"
    override val billingBillNumber = "Bill no:"
    override val billingPaymentMode = "Payment mode:"
    override val billingPaymentStatus = "Payment status:"
    override val billingReceived = "Received:"
    override val billingChange = "Change:"
    override val billingCopyInvoice = "Copy invoice"
    override val billingHistory = "History"

    override val homeCloudSyncTriggered = "⚡ Cloud sync triggered"
    override val homeCloudBackupActive = "Cloud backup active ⚡"
    override val homeBillsCreated: (Int) -> String = { count -> "$count bills created" }
    override val homeRestockNeeded: (Int) -> String = { count -> "$count items need restock." }
    override val homeStockSafe = "Store stock is optimal"
    override val homeStockSufficient = "All key products are well stocked."
    override val homeBillingSubtitle = "Cash / UPI / Udhaar"
    override val homeInventorySubtitle = "Stock inventory"
    override val homeUdhaarSubtitle = "Ledger & WhatsApp"
    override val homeReportsSubtitle = "Sales & profit"

    override val reportsTotalSalesTitle = "Total sales"
    override val reportsBillsGenerated: (Int) -> String = { count -> "$count bills generated" }
    override val reportsHistoryTitle = "Sales history:"
    override val reportsNoRecords = "No records found!"
    override val reportsUdhaarCustomer = "Udhaar customer"
    override val reportsBillNumber = "Bill no:"
    override val reportsCustomer = "Customer:"
    override val reportsPaymentMode = "Payment mode:"
    override val reportsCopy = "Copy"
    override val reportsClose = "Close"
    override val reportsNoChartData = "No sales data available for the distribution chart."
    override val reportsPaymentModeSplit = "Payment mode split"
    override val reportsTotal = "Total"
    override val reportsWeeklyTrend = "Weekly sales trend"

    override val udhaarDebtorsOnly = "Debtors only"
    override val udhaarNoCustomerFound = "No customer found!"
    override val udhaarSaveCustomer = "Save customer"
    override val udhaarCustomerNameRequired = "Customer name required!"
    override val udhaarCustomerAdded: (String) -> String = { name -> "$name added!" }
    override val udhaarPhoneLabel = "Phone:"
    override val udhaarLedgerHistory = "Transaction ledger history:"
    override val udhaarNoTransactions = "No transactions in this account yet."
    override val udhaarCreditGiven = "Credit given"
    override val udhaarPaymentReceived = "Payment received"
    override val udhaarCorrectionReversal = "Correction / reversal"
    override val udhaarInvalidEntry = "Invalid ledger entry"
    override val udhaarReceivePaymentTitle = "Receive payment"
    override val udhaarReceivedAmountLabel = "Received amount *"
    override val udhaarNoteModeLabel = "Note / mode (optional)"
    override val udhaarNoteModePlaceholder = "e.g. Cash, UPI, Paytm..."
    override val udhaarSavePayment = "Save payment"
    override val udhaarCorrectEntry = "Correct ledger entry"
    override val udhaarCorrectAmount = "Correct amount *"
    override val udhaarReasonRequired = "Reason *"
    override val udhaarReverse = "Reverse"
    override val udhaarCorrect = "Correct"

    override val setupShopNameRequired = "Shop name is required!"
    override val setupShopNameHint = "This name will appear on customer bills."
    override val setupPhoneInvalid = "Please enter a valid 10-digit mobile number"
    override val setupPinInvalid = "Please enter a safe 4-digit PIN"
    override val setupCompleted = "Shop setup completed!"

    override val productFormSellingPriceShort = "Sales"
    override val productFormLowStockCount: (Int) -> String = { count -> "Total $count items low in stock" }
    override val productFormWhatsApp = "WhatsApp"
}
