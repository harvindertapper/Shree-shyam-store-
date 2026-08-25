package com.aistudio.shreeshyamstore.pqwzkb

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Issue56JourneyCoverageContractTest {
    @Test
    fun criticalJourneyControlsHaveStableTags() {
        val sources = criticalSources()
        assertTrue("Issue #56 source contract must find all critical Compose files", sources.size == requiredTags.keys.size)

        val missing = requiredTags.flatMap { (path, tags) ->
            val source = sources[path].orEmpty()
            tags.filterNot { tag -> source.contains("testTag(\"$tag\")") }
                .map { tag -> "$path is missing testTag(\"$tag\")" }
        }

        assertTrue(
            "Critical merchant journey controls must keep stable semantics tags: ${missing.joinToString()}",
            missing.isEmpty()
        )
    }

    @Test
    fun criticalMutationActionsKeepDuplicateAndTouchTargetGuards() {
        val sources = criticalSources()
        val billing = sources.getValue("ui/screens/BillingAndPaymentScreen.kt")
        val products = sources.getValue("ui/screens/ProductsAndStockScreen.kt")
        val udhaar = sources.getValue("ui/screens/UdhaarScreen.kt")
        val settings = sources.getValue("ui/screens/SettingsScreen.kt")
        val mutationCard = sources.getValue("ui/components/MutationStatusCard.kt")

        assertTrue(
            "Cash/UPI/Udhaar payment actions must be disabled while checkout is in flight",
            occurrences(billing, "enabled = !checkoutInFlight") >= 3
        )
        assertTrue(
            "Product/category actions must honor mutationInFlight",
            occurrences(products, "mutationInFlight") >= 8
        )
        assertTrue(
            "Udhaar payment and correction actions must honor mutationInFlight",
            occurrences(udhaar, "enabled = !mutationInFlight") >= 3
        )
        assertTrue(
            "Settings sync/backup/restore/save actions must honor mutationInFlight",
            occurrences(settings, "enabled = !mutationInFlight") >= 4
        )
        assertTrue(
            "Shared mutation retry and dismiss actions must keep a 48dp minimum target",
            occurrences(mutationCard, "heightIn(min = 48.dp)") >= 2
        )
        assertTrue(
            "Billing payment actions must remain at least 48dp high",
            billing.contains(".height(60.dp)")
        )
        assertTrue(
            "Product save must remain at least 48dp high",
            products.contains(".height(56.dp)") && products.contains("save_product_button")
        )
        assertTrue(
            "Settings restore and logout actions must remain at least 48dp high",
            settings.contains(".heightIn(min = 48.dp)") && settings.contains("settings_restore_button")
        )
    }

    private fun criticalSources(): Map<String, String> {
        val roots = listOf(
            File("src/main/java/com/aistudio/shreeshyamstore/pqwzkb"),
            File("app/src/main/java/com/aistudio/shreeshyamstore/pqwzkb"),
            File("../app/src/main/java/com/aistudio/shreeshyamstore/pqwzkb")
        )
        val root = roots.firstOrNull { it.isDirectory } ?: return emptyMap()
        return requiredTags.keys.mapNotNull { path ->
            val file = File(root, path)
            if (file.isFile) path to file.readText() else null
        }.toMap()
    }

    private fun occurrences(source: String, value: String): Int =
        source.windowed(value.length, 1).count { it == value }

    private val requiredTags = mapOf(
        "ui/screens/AuthScreens.kt" to listOf(
            "language_switcher_pill",
            "lang_en_button",
            "lang_hi_button",
            "google_sign_in_button",
            "skip_login_button",
            "pin_key_delete",
            "forgot_pin_button",
            "switch_account_button"
        ),
        "ui/screens/FirstLaunchSetupScreen.kt" to listOf(
            "shop_name_input",
            "owner_name_input",
            "owner_phone_input",
            "security_pin_input",
            "biometric_switch",
            "save_shop_setup_button"
        ),
        "ui/screens/ProductsAndStockScreen.kt" to listOf(
            "product_search_input",
            "product_scan_barcode_button",
            "manage_categories_button",
            "category_add_input",
            "category_add_button",
            "new_category_name_input",
            "confirm_new_category_button",
            "product_name_input",
            "product_category_input",
            "product_mrp_input",
            "product_sp_input",
            "product_track_stock_switch",
            "product_stock_input",
            "save_product_button"
        ),
        "ui/screens/BillingAndPaymentScreen.kt" to listOf(
            "billing_search_input",
            "billing_scan_barcode_button",
            "checkout_payment_button",
            "received_amount_field",
            "cash_pay_button",
            "upi_pay_button",
            "udhaar_pay_button",
            "udhaar_customer_search",
            "new_bill_confirm"
        ),
        "ui/screens/UdhaarScreen.kt" to listOf(
            "customer_search_input",
            "fab_add_customer",
            "add_customer_name_input",
            "add_customer_phone_input",
            "confirm_add_customer_button",
            "fab_receive_payment",
            "payment_amount_input",
            "payment_note_input",
            "confirm_payment_button",
            "ledger_correction_amount_input",
            "ledger_correction_reason_input",
            "cancel_payment_button",
            "cancel_ledger_correction_button",
            "reverse_ledger_entry_button",
            "correct_ledger_entry_button"
        ),
        "ui/screens/SettingsScreen.kt" to listOf(
            "logout_button",
            "settings_sign_in_button",
            "settings_shop_name_field",
            "automatic_sync_switch",
            "firebase_sync_card",
            "settings_sync_health_card",
            "sync_now_button",
            "settings_backup_now_button",
            "settings_restore_button",
            "settings_confirm_restore",
            "settings_cancel_restore",
            "save_settings_button"
        ),
        "ui/screens/ReportsScreen.kt" to listOf(
            "report_custom_range_selector",
            "report_select_start_date",
            "report_loading_state",
            "report_retry_button"
        ),
        "ui/components/MutationStatusCard.kt" to listOf(
            "mutation_status_card",
            "mutation_retry_button",
            "mutation_dismiss_button"
        ),
        "MainActivity.kt" to listOf(
            "bottom_nav",
            "nav_home",
            "nav_billing",
            "nav_products",
            "nav_udhaar",
            "nav_reports",
            "nav_settings"
        )
    )
}
