# Professional Delivery Implementation Plan - Shree Shyam Store

Date: 2026-07-02

Status: Planning document. This plan upgrades the remaining roadmap from a basic MVP sequence to a professional delivery route for a real kiryana/general-store counter app.

## Goal

Deliver Shree Shyam Store as a reliable daily-use shop application, not just a demo:

- Google owner sign-in must restore the correct shop after clear storage, reinstall, phone loss, or device change.
- Products, categories, stock, sales, udhaar, customers, and settings must not disappear silently.
- Billing must be fast at the counter and must not force customer selection or invoice creation for ordinary cash sales.
- Loose items must be first-class: the owner can enter quantity or amount, for example `Rs. 50 sugar @ Rs. 47/kg`, and the app calculates the other side.
- Private business insights such as stock value and profit must stay in an owner-only section, away from customer-facing billing screens.
- The app should reduce shop workload for the owner/family member, not create extra bookkeeping pressure.

## Current truth this plan is based on

- Final Android app id is `com.harrylabs.shreeshyamstore`.
- Room is already schema version 5. Syncable entities use UUID primary keys, the old local `users` table has been removed from the runtime path, and the local sync outbox exists for retryable cloud-sync work.
- Firebase Auth with Google Sign-In and Firestore profile foundation is implemented locally, but final live owner sign-in/shop-profile acceptance is still a gate.
- Existing product/category/customer/udhaar/sales cloud restore is not yet accepted as trustworthy business sync. User testing after clear storage found products and credit data missing, so the next professional priority is a real sync/restore path, not more surface polish.
- Existing feature packets remain useful, but the delivery route now needs stronger professional tracks for sync, billing speed, owner-private reports, and release hardening.

## Product principles

1. **Trust first:** if a sale, udhaar entry, or product is saved, it must either sync or clearly show pending/error status. Silent loss is unacceptable.
2. **Counter speed first:** cash sale should be one-tap save. Customer and invoice are optional unless udhaar or sharing is needed.
3. **No forced stock for every item:** important/high-value/fast-moving items can be tracked; small or rarely tracked items can remain stock-optional.
4. **Loose items are core, not an add-on:** amount-to-quantity and quantity-to-amount must be available during billing and product setup.
5. **Owner privacy:** profit, stock value, purchase cost, and business analytics live in a private owner section, not the public counter flow.
6. **Professional but simple:** daily summary, subtle sound/haptic, and a shop-close routine are okay; childish gamification is not.
7. **No false payment claims:** UPI remains a manual payment record unless a future approved integration verifies payment.
8. **One active shop in UI:** current product remains single-shop for the owner. The cloud model may keep memberships for future use, but multi-store UI is deferred.

## Delivery tracks and order

### PD-00 - Source-of-truth and checklist sync

**Goal:** Fix stale documentation before implementation starts so agents do not keep following old Room v2/Firebase-not-implemented text.

Scope:

- `APP_BUILD_CHECKLIST.md`
- `docs/IMPLEMENTATION_PLAN.md`
- This professional delivery plan

Implementation:

- Update current inventory to say Firebase Auth/Firestore foundation is partially implemented and pending final live QA.
- Update Room status to Room v4 and remove old Room v2 reset wording.
- Mark professional features as planned tracks instead of later vague ideas.
- Keep existing QA checklist, but label professional acceptance gates separately because the owner requested to skip QA execution for now.

Acceptance:

- Source docs agree on Room v4 and FR-G in-progress status.
- The next implementation task can be assigned from one clear route.

Required evidence:

- Docs diff.
- No build required if docs-only.

### PD-01 - Stabilization checkpoint and main-safety verification

**Goal:** Freeze the current unstable state before adding bigger features.

Scope:

- `AppDatabase.kt`
- `ShopRepository.kt`
- Current dirty FR-G/sync/billing files
- Gradle/test environment notes

Implementation:

- Verify the Android Studio main-thread database fix:
  - Repository suspend methods that touch Room use `withContext(Dispatchers.IO)`.
  - `clearAllTables()` never runs on the main thread.
  - Room callbacks do not run synchronous seed queries on the opening thread.
- Add/adjust tests for startup/session paths where possible.
- Document any remaining Gradle cache/environment issue separately from app logic.
- Create a clean checkpoint only after the user approves staging/commit scope.

Acceptance:

- Welcome screen no longer crashes with `Cannot access database on the main thread`.
- App launch reaches the expected login/home path on a debug build.
- No unrelated cleanup is mixed in.

Required evidence:

- `.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon`
- `.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon`
- If Gradle cache blocks tests, include the exact non-code blocker.

### PD-02 - Firebase owner trust gate

**Goal:** Make login and shop restore trustworthy before real data entry.

Scope:

- Firebase Auth flow
- `FirebaseOwnerRepository`
- `ShopViewModel`
- `SettingsDataStore`
- Firestore rules/tests
- Security/privacy checklist

Implementation:

- Finish live Google Sign-In acceptance on physical phone or stable emulator.
- Confirm provider enablement, SHA keys, package id, and `google-services.json` all match `com.harrylabs.shreeshyamstore`.
- On sign-in:
  - Load `users/{uid}`.
  - Resolve active shop membership.
  - Create owner profile/shop/membership through a transaction only when missing.
  - Never create duplicate shops for the same first-time owner path.
- On account switch:
  - Do not show old local data as if it belongs to the new account.
  - Show a restore/loading gate.
  - Clear local sensitive data only after successful restore decision.
- On logout:
  - Clear sensitive display/session data.
  - Preserve non-secret previous UID/shop markers only if needed for account-change detection.

Acceptance:

- Same Google account after clear storage restores the same owner/shop profile.
- Wrong/signed-out user cannot read/write another shop.
- App does not show an empty shop as "success" when restore failed.

Required evidence:

- Firestore rules tests.
- Manual sign-in and clear-storage proof.
- Security/privacy checklist update.

### PD-03 - Professional sync engine and restore gate

**Goal:** Stop product, credit, sale, and stock data from disappearing after clear storage.

Scope:

- Room entities/DAOs if new sync fields or outbox table are needed
- `ShopRepository`
- New sync coordinator/repository
- `FirebaseOwnerRepository` or a dedicated Firestore data repository
- Firestore rules/tests
- Restore UI/state in `ShopViewModel`

Implementation:

1. Add a local sync queue/outbox.
   - Each write gets `clientOperationId`, `localUuid`, `shopId`, `createdByUid`, `sourceDeviceId`, `syncStatus`, retry count, and last error.
   - Completed business records use stable document ids so retry cannot create duplicates.
   - Local save must complete first; cloud sync starts immediately after and retries in background.

2. Restore gate after login.
   - After clear storage/reinstall, the app must download the accepted domains before showing Home.
   - If restore fails, show a blocking retry screen instead of silently opening empty inventory.
   - The gate should show simple language: restoring shop data, pending retry, or no cloud data found.

3. Sync domains in safe order.
   - Phase A: shop settings, categories, products.
   - Phase B: customers and udhaar transactions.
   - Phase C: sales, sale items, stock adjustments, and invoice metadata after billing persistence is finalized.

4. Firestore structure.
   - `shops/{shopId}/settings/main`
   - `shops/{shopId}/categories/{categoryUuid}`
   - `shops/{shopId}/products/{productUuid}`
   - `shops/{shopId}/customers/{customerUuid}`
   - `shops/{shopId}/udhaarTransactions/{transactionUuid}`
   - `shops/{shopId}/sales/{saleUuid}`
   - `shops/{shopId}/sales/{saleUuid}/items/{saleItemUuid}` or a flat `saleItems` collection if report queries need it.
   - `shops/{shopId}/stockAdjustments/{adjustmentUuid}`

5. Conflict policy.
   - Products/categories/settings: last-write-wins only with `updatedAt`, `updatedByUid`, and device metadata.
   - Sales/sale items: append-only; no silent edits after completion.
   - Stock adjustments: append-only events.
   - Udhaar transactions: append-only ledger entries; corrections are new transactions, not destructive rewrites.

Acceptance:

- Add product -> force sync -> clear storage -> same Google login -> product restored.
- Add udhaar transaction -> clear storage -> same Google login -> customer balance restored.
- Save sale -> app restart/offline retry -> no duplicate sale after retry.
- Pending sync is visible; failure is visible; no silent data loss.

Required evidence:

- Unit tests for outbox/idempotency mapping.
- Firestore rules tests for every synced collection.
- Manual clear-storage restore proof for products, customers, udhaar, and later sales.

### PD-04 - Product and stock setup for real kiryana use

**Goal:** Make product entry fast, unit-aware, and forgiving.

Scope:

- `ProductsAndStockScreen.kt`
- `ShopViewModel.kt`
- Product entity/DAO/repository if missing fields need migration
- Strings in English/Hindi
- Calculator tests

Implementation:

- Product type:
  - Piece/count
  - Weight: kg/g with base grams
  - Volume: litre/ml with base ml
- Price model:
  - Selling price in paise per selected unit.
  - Purchase price in paise per selected unit when known.
  - Store both display unit and canonical base-unit denominator.
- Stock model:
  - Add `stockTrackingEnabled` or equivalent.
  - Default can be off for quick-added/low-risk items.
  - Tracked items use stock validation and low-stock alerts.
  - Untracked items can be sold without stock blocking.
- Inline category creation:
  - Add category while adding product.
  - Prevent case-insensitive duplicates.
  - Newly created category auto-selects.
- Quick cleanup:
  - Products created during billing can be marked `needsReview`.
  - Product screen shows a cleanup filter for missing category/purchase price/stock tracking.

Acceptance:

- Owner can create sugar priced per kg with stock in kg/g.
- Owner can create milk priced per litre with stock in litre/ml.
- Owner can quick-create a category from product add.
- Owner can leave stock untracked for items where stock counting is unnecessary.

Required evidence:

- Unit tests for product unit/rate conversions.
- English/Hindi string parity.
- Manual proof for product add/edit and inline category creation.

### PD-05 - Billing counter hardening

**Goal:** Make billing feel like a fast counter register.

Scope:

- `BillingAndPaymentScreen.kt`
- `ShopViewModel.kt`
- `ShopRepository`
- Sale/SaleItem entities if fields are missing
- Strings/resources
- Calculator tests

Implementation:

1. No forced customer/invoice.
   - Default cash sale flow: add items -> one-tap `Save Sale`.
   - Customer is optional for cash/UPI.
   - Customer is required only when payment mode is udhaar.
   - Invoice screen/share is shown only when owner asks for invoice/share.

2. Loose item calculator.
   - Quantity-to-amount: `1.25 kg sugar @ Rs. 47/kg = Rs. 58.75`.
   - Amount-to-quantity: `Rs. 50 sugar @ Rs. 47/kg = 1.064 kg`.
   - Volume equivalent: `Rs. 20 milk @ Rs. 60/litre = 333 ml`.
   - Piece products reject fractional pieces unless explicitly modeled as loose packets.

3. Per-line rate override.
   - Override does not mutate product master price.
   - Cart line stores original rate, effective rate, override flag, quantity base, display quantity, and line total.
   - UI clearly shows when a line was billed at a changed rate.

4. Smart product add during billing.
   - If item is missing, bill does not stop.
   - Quick add requires only name and selling price.
   - Category can be "Uncategorized" or selected later.
   - Quick-added product syncs like normal product and appears in cleanup list.

5. Stock validation.
   - Tracked products reduce stock in base units.
   - Untracked products do not block sale.
   - Oversell must be an explicit owner setting, not accidental.

Acceptance:

- Cash sale can be saved without customer and without invoice.
- Udhaar sale requires or quick-creates customer.
- Amount-to-quantity works during billing for weight/volume.
- Missing product can be quick-added without leaving the bill.
- Sale item snapshots keep old bill readable even if product price/name changes later.

Required evidence:

- Unit tests for amount-to-quantity, quantity-to-amount, rate override, line totals, and stock validation.
- Repository tests for sale persistence and stock reductions.
- Manual proof for cash, UPI, udhaar, loose item, and quick-add flows.

### PD-06 - Quick udhaar and WhatsApp reminder workflow

**Goal:** Make credit tracking faster than notebook entry.

Scope:

- `UdhaarScreen.kt`
- `ShopViewModel.kt`
- `ShopRepository`
- Customer/Udhaar DAO methods
- Android share intent helper
- Strings/resources

Implementation:

- Customer list row actions:
  - Tap customer -> detail.
  - Quick add credit/payment sheet from row or detail.
  - Amount, note, date default to now.
- From billing:
  - Udhaar payment mode asks for customer.
  - If customer missing, quick-create name and optional phone.
- WhatsApp/share reminder:
  - Generate a polite reminder message with shop name, customer name, balance, and payment note.
  - Use Android share sheet / WhatsApp intent where available.
  - Do not request Contacts permission.
  - If WhatsApp missing, fall back to normal share sheet/copy.
- Ledger rules:
  - Credit and payment are append-only transactions.
  - Balance is calculated from transactions.
  - Corrections are new adjustment transactions, not silent edits.

Acceptance:

- Owner can add udhaar amount in two taps from customer.
- Reminder/share message is ready without manually typing the full text.
- Clear storage after sync restore keeps customer balance correct.

Required evidence:

- Balance calculation tests.
- Share intent fallback proof.
- Firestore sync/restore proof after PD-03 Phase B.

### PD-07 - Owner Private Desk: stock value and profit

**Goal:** Add private business insights without exposing them at the counter.

Scope:

- New private Owner Desk screen or private tab under Reports
- `ReportsScreen.kt` if reused
- Calculation utilities
- Repository queries
- App lock/PIN/biometric packet if approved
- Strings/resources

Implementation:

1. Private access.
   - Add an Owner Desk section separate from Billing.
   - Hide purchase price, profit, and stock value from normal billing screens.
   - Optional local app lock: AndroidX Biometric where available, with a PIN fallback only if hashed safely. Do not store raw PIN.

2. Stock value.
   - Show total stock value by selling price.
   - Show category-wise stock value by selling price.
   - Also show purchase-cost stock value and potential margin where purchase price exists.
   - Products without stock tracking or without purchase price appear in a separate "missing data" group.

3. Profit.
   - Profit must use sale item purchase-cost snapshots captured at sale time.
   - Do not calculate historical profit from today's product purchase price.
   - Show:
     - Today profit
     - This month profit
     - Custom date range
     - Product-wise profit
     - Unknown-cost sales total

4. Formula rules.
   - Selling stock value:
     - `stockQuantityBase * sellingPricePaise / sellingUnitBaseQuantity`
   - Purchase stock value:
     - `stockQuantityBase * purchasePricePaise / purchaseUnitBaseQuantity`
   - Sale line profit:
     - `lineTotalPaise - purchaseCostSnapshotPaise`
   - Use integer paise/base units and safe rounding; do not use floating-point for stored business totals.

Acceptance:

- Owner can see total stock value and category-wise stock value.
- Owner can see today/month profit only inside private section.
- Profit handles missing purchase price honestly instead of showing false profit.
- Customer-facing billing screen does not expose private cost/profit.

Required evidence:

- Unit tests for stock value and profit calculators.
- Report query tests with piece, weight, and volume items.
- Privacy review for where purchase price/profit is displayed.

### PD-08 - End-of-day "Aaj ka hisaab"

**Goal:** Give the owner a useful daily close summary and a small feeling of completion.

Scope:

- Home screen or new day-close screen
- Reports/calculation utilities
- DataStore or Room day-close record if needed
- Strings/resources
- Sound/haptic setting if added

Implementation:

- End-of-day summary card:
  - Total sale
  - Cash sale
  - Manual UPI sale
  - Udhaar added
  - Udhaar payment received
  - Net cash expected
  - Profit if Owner Desk unlocked/allowed
  - Low stock items
  - Top sold items
  - Pending sync count
- Shop closed action:
  - Owner taps `Close shop for today`.
  - App stores close timestamp and summary snapshot.
  - Optional haptic/success sound if enabled.
- Routine joy:
  - Subtle daily streak for completed shop-close summaries.
  - No childish badges or distracting game UI.

Acceptance:

- Owner can understand today's business in one screen.
- Pending sync is visible before shop close.
- Shop close does not block normal next-day use.

Required evidence:

- Calculation tests for daily summary.
- Manual proof with seeded sales/udhaar data.

### PD-09 - Invoice PDF and share workflow

**Goal:** Generate professional receipts only when needed.

Scope:

- Invoice data formatter
- PDF generator/helper
- Bill success/invoice preview screen
- Android share intent/FileProvider config if needed
- English/Hindi invoice labels

Implementation:

- Invoice is optional after sale.
- Invoice uses saved sale and sale-item snapshots, not live product data.
- PDF includes:
  - Shop name
  - Phone
  - Bill number
  - Date/time
  - Items, quantity display, rate, line total
  - Payment mode
  - Customer name only if selected
  - Total
  - QR image if available and safe to access
- Share:
  - Save to app cache or user-selected destination.
  - Share through Android share sheet.
  - WhatsApp direct target optional and failure-safe.
  - No broad storage permission unless explicitly approved.

Acceptance:

- Sale can be saved without invoice.
- Owner can generate/share invoice after sale.
- Missing WhatsApp falls back gracefully.
- English/Hindi invoice labels are correct.

Required evidence:

- Invoice formatting tests.
- PDF generation proof.
- Android share proof.

### PD-10 - Professional release hardening

**Goal:** Prepare for a trustworthy family-shop release.

Scope:

- AndroidManifest backup policy
- Security/privacy checklist
- Firebase App Check/cost controls
- Release signing docs
- UI polish
- Localization cleanup
- Crash/diagnostic policy if added later

Implementation:

- App Check:
  - Debug provider only for development.
  - Play Integrity direction for production.
  - Enforcement timing decided before broad real data use.
- Cost controls:
  - Budget alerts.
  - Conservative query/write design.
  - Firestore delete protection and PITR posture decision.
- Backup/privacy:
  - Review `android:allowBackup` before release.
  - Decide whether customer/sales data should be excluded from Android backup because cloud restore is the intended path.
- App lock:
  - If approved, add owner-private lock before profit/stock values are considered release-ready.
- Visual polish:
  - Clean English/Hindi resources.
  - Remove mojibake.
  - Keep billing UI fast and uncluttered.
- Release:
  - Release signing steps documented without secrets in repo.
  - Version name/code and changelog documented.

Acceptance:

- Release build can be produced without committing secrets.
- Backup/privacy posture is documented.
- No high-risk debug/development behavior remains in production path.

Required evidence:

- Build/test results.
- Security/privacy checklist update.
- Manual release checklist.

## Recommended next coding sequence

Because the owner has already seen product/credit loss after clear storage, the next implementation order should be:

1. **PD-00 docs sync** - complete immediately.
2. **PD-01 stabilization checkpoint** - verify main-safety fix and create clean baseline.
3. **PD-02 Firebase owner trust gate** - live sign-in and shop restore acceptance.
4. **PD-03 Phase A/B sync** - settings, categories, products, customers, and udhaar restore.
5. **PD-04 product/stock setup** - finish unit-aware product entry, stock by exception, inline category creation, cleanup flags.
6. **PD-05 billing hardening** - no forced customer/invoice, amount-to-quantity loose flow, quick product add, rate override, stock validation.
7. **PD-03 Phase C sync** - sales, sale items, stock adjustments, and invoice metadata sync.
8. **PD-06 quick udhaar** - fast udhaar and WhatsApp/share reminder.
9. **PD-07 Owner Private Desk** - stock value, profit, private reports.
10. **PD-08 Aaj ka hisaab** - daily close summary and subtle routine joy.
11. **PD-09 invoice PDF/share** - professional invoice only when needed.
12. **PD-10 release hardening** - App Check/cost/backup/privacy/signing/polish.

## What must not be done yet

- Do not enter real long-term inventory until sync/restore gates are trustworthy.
- Do not claim UPI payment success; record it as manual UPI only.
- Do not add Bluetooth printer in this route.
- Do not add multi-store UI or staff permission UI yet.
- Do not hide sync failures from the owner.
- Do not calculate profit from current product cost for old sales.
- Do not force every product into stock tracking.

## Definition of professional acceptance

This application is professionally acceptable for shop use only when:

- A clear-storage/reinstall test restores owner profile, products, customers, udhaar, sales, and stock history.
- Offline sale/udhaar writes are queued and visibly sync later without duplicates.
- Cash sale can be saved quickly without customer or invoice.
- Loose item amount-to-quantity and quantity-to-amount flows work in billing.
- Stock tracking can be optional per product.
- Profit and stock value are available only in a private owner area.
- Daily close summary is understandable and useful.
- Invoice PDF/share works from saved records.
- Firebase rules, App Check posture, cost controls, Android backup policy, and release signing are documented and accepted.
