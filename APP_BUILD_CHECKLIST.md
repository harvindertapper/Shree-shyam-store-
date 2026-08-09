# Shree Shyam Store Build Checklist

## Product Target

Build a professional cloud-backed, offline-capable Android shop register app for small retail stores.

Default language: **English**  
Second language: **Hindi**

The app should help a shop owner:

- Manage products and stock.
- Create bills quickly.
- Track cash, UPI, and udhaar sales.
- Maintain customer udhaar ledger.
- Generate/share invoices.
- View useful daily/monthly reports.
- Back up important business data.
- Recover shop data after phone loss, clear storage, reinstall, or device change.

## Current App Inventory

- Firebase Auth/Firestore owner foundation is partially implemented. Local code/rules are in progress; final live Google Sign-In, shop-profile restore, App Check, and broader business-data sync acceptance are still pending.
- Final app identity is `com.harrylabs.shreeshyamstore` after the approved identity rename packet.
- Room schema version 5 is implemented. Syncable business entities use UUID primary keys, the old local `users` table has been removed from the runtime auth path, and a local `sync_outbox_operations` table tracks retryable cloud sync work.
- The approved early reset/migration path has already been used through Room v4 before real inventory entry. Room v4 to v5 uses an intentional migration for the sync outbox. Future v5 onward schema changes require intentional migrations or a new explicit owner-approved decision.
- First-launch shop setup.
- Room database for local storage.
- DataStore for settings/session.
- Product and category management.
- Opening stock and stock adjustment.
- Billing cart.
- Cash, UPI, and udhaar payment modes.
- Sale and sale-item records.
- Customer udhaar ledger.
- Udhaar payment entry.
- Basic reports.
- Shop settings with QR image selection.
- Debug APK build verified.
- Emulator launch verified.

## Immediate Fix Checklist

- [ ] Convert user-facing UI text to string resources.
- [ ] Add English default strings.
- [ ] Add Hindi translations in `values-hi`.
- [ ] Clean corrupted/mojibake Hindi text.
- [ ] Confirm app starts in English by default.
- [ ] Add language switch in settings.
- [ ] Save selected language preference.
- [ ] Apply language on app restart.

## Professional Delivery Feature Checklist

- [ ] Google owner sign-in works reliably on physical phone or stable emulator.
- [ ] Firebase Auth owner sign-in works reliably.
- [x] App identity rename to `com.harrylabs.shreeshyamstore` is complete before Firebase setup (`b7d92f2`).
- [ ] Owner UID maps to the active shop membership; UI exposes one active shop for now.
- [ ] Firestore security rules protect shop data by owner/member access.
- [ ] App Check and cost/budget guardrails are reviewed before product sync.
- [ ] Cloud-backed shop profile restores after reinstall/clear storage.
- [ ] Product/category/settings sync and restore pass before real inventory entry.
- [ ] Customer/udhaar sync and restore pass before real credit records are trusted.
- [ ] Sales, sale items, and stock adjustments sync with idempotency and no duplicate bills after retry.
- [ ] Restore gate blocks Home when cloud restore fails instead of showing an empty shop as success.
- [ ] Sync pending/error status is visible to the owner.
- [ ] Product add/edit supports piece, weight, and volume unit setup.
- [ ] Product add/edit preview calculator works.
- [ ] Product add/edit supports inline category creation with duplicate prevention.
- [ ] Product stock tracking can be enabled/disabled per product.
- [ ] Quick-added billing products are marked for later cleanup.
- [ ] Room/local cache strategy is documented and tested for offline use.
- [ ] First-launch setup stores shop details.
- [ ] Product add/edit works.
- [ ] Category add/rename works.
- [ ] Opening stock entry works.
- [ ] Stock adjustment history works.
- [ ] Billing cart quantity controls work.
- [ ] Cash/UPI sale can be saved without forced customer selection.
- [ ] Invoice generation is optional and user-triggered after sale.
- [ ] Billing supports amount-to-quantity loose item flow, for example `Rs. 50 sugar @ Rs. 47/kg`.
- [ ] Billing supports quantity-to-amount loose item flow.
- [ ] Billing supports per-line rate override without mutating product master price.
- [ ] Missing product can be quick-added during billing without stopping the bill.
- [ ] Billing prevents invalid/negative stock when tracking is enabled.
- [ ] Cash sale saves correctly.
- [ ] UPI sale saves correctly as a manual payment record.
- [ ] Udhaar sale creates customer credit transaction.
- [ ] Quick udhaar entry works from customer list/detail.
- [ ] WhatsApp/share reminder text is generated without requiring Contacts permission.
- [ ] Bill success screen shows correct invoice details.
- [ ] Invoice text copy works.
- [ ] Reports show correct daily/monthly totals.
- [ ] Private Owner Desk shows total stock value by selling price.
- [ ] Private Owner Desk shows category-wise stock value.
- [ ] Private Owner Desk shows today/month/custom profit using sale-item purchase-cost snapshots.
- [ ] Profit/stock value/purchase cost stay hidden from customer-facing billing screens.
- [ ] End-of-day "Aaj ka hisaab" summarizes cash, UPI, udhaar, total sale, low stock, top items, pending sync, and profit where allowed.
- [ ] Subtle shop-close routine joy is available without childish gamification.
- [ ] Low-stock dashboard is accurate.

## Invoice Sharing Checklist

- [ ] Define invoice data model used by receipt/PDF/share.
- [ ] Create clean invoice preview screen.
- [ ] Generate PDF invoice locally.
- [ ] Save PDF to app cache or user-selected location.
- [ ] Share invoice PDF through Android share sheet.
- [ ] Add direct WhatsApp share intent where available.
- [ ] Include shop name, phone, bill number, date, items, payment mode, and total.
- [ ] Support English and Hindi invoice labels.
- [ ] Handle missing WhatsApp gracefully.
- [ ] Add tests for invoice text/data formatting.

## Later Feature Checklist

- [ ] Manual backup/export/import if still required after cloud recovery.
- [ ] CSV export for products, sales, customers, and udhaar.
- [ ] Barcode/product-code support.
- [ ] Purchase entry and supplier module.
- [ ] Discounts.
- [ ] Returns/refunds.
- [ ] Optional GST/tax fields.
- [ ] App lock/PIN.
- [ ] Password reset/recovery.
- [ ] App icon and brand polish.
- [ ] Release signing setup.
- [ ] Versioning and changelog.

## Deferred

- [ ] Bluetooth thermal printer support.
- [ ] Multi-store support.
- [ ] Staff roles and permissions implementation.
- [ ] Real UPI payment confirmation.

## QA Checklist

- [ ] Fresh install opens correctly.
- [ ] Sign in with Google owner account.
- [ ] Restart and confirm Firebase session/profile restore.
- [ ] Complete first-launch setup.
- [ ] Add category.
- [ ] Add product.
- [ ] Add opening stock.
- [ ] Create cash bill.
- [ ] Create UPI bill.
- [ ] Create udhaar bill.
- [ ] Add udhaar payment.
- [ ] Verify stock reduced after sale.
- [ ] Verify reports totals.
- [ ] Restart app and confirm session/settings persist.
- [ ] Clear storage and confirm cloud restore path is available after sign-in.
- [ ] Reinstall and confirm cloud restore path is available after sign-in.
- [ ] Second device sign-in restores owner/shop profile according to approved sync rules.
- [ ] Inventory restore gate restores categories, products, settings, unit fields, and rates before real inventory entry.
- [ ] Offline mode shows cached data according to approved conflict rules.
- [ ] Switch language to Hindi.
- [ ] Switch language back to English.

## Required Verification

```powershell
.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon
```

For Android/emulator-facing work:

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --stacktrace --console=plain --no-daemon
```
