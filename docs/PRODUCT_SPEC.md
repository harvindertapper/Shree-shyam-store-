# Product Spec

Status: Draft source of truth. Unknowns are marked `TBD - owner decision required`.

## App Purpose

Shree Shyam Store is a cloud-backed, offline-capable Kotlin Android shop-management app customized for a small Indian kiryana/general store. It should help a shop owner run daily counter operations with recoverable shop data, fast billing, product and stock control, customer udhaar tracking, owner-private business insights, practical daily close reports, and invoice sharing when needed.

Governance and future work must stay aligned to the Shree Shyam Store kiryana-shop product and native Kotlin Android stack.

The app is not a payment verification system. Cash, UPI, and udhaar entries are business records entered by the user unless a future approved integration verifies payment.

Owner decision on 2026-06-17: cloud sync/auth is mandatory professional foundation. Firebase prerequisites, Auth/shop restore, rules/App Check/cost controls, and inventory restore gates must pass before trusted real inventory and billing hardening.

Owner decision on 2026-06-17: final Android application id, namespace, Firebase app identity, and Play Store identity must be `com.harrylabs.shreeshyamstore`. Firebase Auth, Google Sign-In, SHA keys, and `google-services.json` must not be configured against the old random application id.

Owner decision on 2026-06-17: no real shop inventory has been entered yet. The approved early reset/migration path has been used to make the Room schema Firebase-ready before real inventory entry.

Owner decision on 2026-06-17: owner will enter real shop inventory only after Firebase Auth/shop profile, Firestore rules/App Check/cost guardrails, Product Add/Edit unit/rate/stock UI, product/category/settings sync, and inventory restore QA pass.

Owner direction on 2026-07-02: delivery should no longer be treated as a bare MVP. The app must be planned as a professional daily-use product with reliable cloud restore/sync, fast optional-customer billing, loose-item amount-to-quantity entry, quick udhaar, smart product quick-add, stock-by-exception, private stock/profit insights, end-of-day "Aaj ka hisaab", and optional invoice/share workflows.

M02F-A/DM-004 architecture decision: use a hybrid phased migration with Firestore as the canonical cloud source of truth over time and Room as the local working cache/offline transaction store. This is accepted for the foundation reset in `docs/governance/FIREBASE_CLOUD_SYNC_ARCHITECTURE.md` and `docs/governance/FOUNDATION_RESET_DM004.md`.

## Target Users

- Primary user: small retail shop owner or trusted family/operator using one Android phone.
- Business context: local kiryana/general-store workflows with quick counter billing, simple inventory, supplier/product growth later, and customer udhaar tracking.
- Language expectation: English by default, Hindi as a proper second app language.
- Data expectation: shop data must survive phone loss, clear storage, reinstall, and device change through an approved cloud ownership and restore model.

## Current Product Inventory From Repo

- Firebase owner login using Android Credential Manager Sign in with Google is implemented locally; final live account authorization QA is pending.
- First-launch shop setup with shop name, owner phone, welcome chant toggle.
- Manual Compose navigation with bottom navigation after auth/setup.
- Local Room database for products, categories, stock, sales, customers, udhaar, stock adjustments, and retryable sync outbox records. The runtime local `users` table has been removed since Room v4.
- DataStore settings/session markers including selected language, cached owner UID, and cached shop ID. No Firebase tokens are stored in DataStore.
- Products and stock screens with categories, product add/edit, opening stock, and stock adjustment.
- Billing cart, payment screen, bill success screen, invoice text copy.
- Cash, UPI, and udhaar payment modes.
- Customer udhaar ledger and payment entry.
- Reports screen with sales summaries and history.
- Settings screen with shop profile, language switch, static Paytm QR image URI, welcome chant, and logout.
- The hybrid Firestore/Room architecture is accepted. Firebase Auth, Google Sign-In client integration, owner/shop/membership profile code, and deployed Firestore rules are implemented; final manual Google account sign-in/shop-profile QA, App Check posture, and cost controls remain pending.

## Core Workflows

1. Owner opens app and reaches Welcome.
2. App routes to Owner Login, Setup, or Home based on Firebase Auth plus local setup markers.
3. Owner signs in with Google.
4. Owner creates or restores the cloud-backed shop profile, then completes first-launch shop setup when required.
5. Owner adds categories and products, with opening stock when needed.
6. Owner creates a bill from the cart. Missing products can be quick-added during billing without stopping the bill.
7. Owner can bill loose products by quantity or by amount, for example entering `Rs. 50 sugar @ Rs. 47/kg` and letting the app calculate quantity.
8. Owner selects Cash, UPI, or Udhaar payment mode.
9. Cash/UPI sale can be saved without forcing customer selection or invoice generation.
10. Udhaar sale requires/selects or quick-creates a customer.
11. App saves sale, sale items, stock adjustment records, and udhaar credit when applicable.
12. Owner generates/copies/shares invoice only when needed.
13. Owner reviews udhaar, quick-adds credit/payment, and can prepare a WhatsApp/share reminder message.
14. Owner reviews private stock value/profit reports away from customer-facing billing screens.
15. Owner reviews end-of-day "Aaj ka hisaab".
16. Owner edits settings and changes app language.

## Screens and Routes Known From Repo

The current screen state is a sealed class, not a typed navigation graph:

- `Welcome`
- `Login`
- `Setup`
- `Home`
- `Billing`
- `Payment(invoiceTotal)`
- `BillSuccess`
- `Products`
- `AddEditProduct(productUuid)`
- `OpeningStock`
- `StockAdjustment(productUuid)`
- `Udhaar`
- `CustomerDetail(customerUuid)`
- `Reports`
- `Settings`

## Data Handled

- Shop profile: shop name, owner phone, QR image URI, welcome setting.
- Owner identity: Firebase Auth UID, email, display name, active shop ID, and owner membership. Local password hashes are no longer part of the runtime auth path.
- Session markers: cached owner UID, cached shop ID, non-secret display/email fields, and setup markers. Firebase Auth remains the source of truth for login state.
- Product data: product names, categories, MRP, selling price, purchase price, stock, stock-tracking preference, low-stock settings, quick-add cleanup status, active state.
- Sales data: bill number, total, payment mode, customer reference, notes, timestamps.
- Invoice items: product snapshots, quantities, unit prices, totals.
- Customer data: names, phone numbers.
- Udhaar ledger: credit/payment transactions and notes.
- Stock audit data: stock adjustment records and reasons.

Professional unit/measurement behavior must support loose kiryana goods without forcing piece-only billing:

- Piece/count products.
- Weight products such as kg/gram.
- Volume products such as litre/ml.
- Packet/box/custom units if approved.
- Decimal quantities for measured goods.
- Amount-to-quantity entry for loose items, for example `Rs. 50 sugar @ Rs. 47/kg`.
- Quantity-to-amount entry for loose items.
- Sale item quantity/unit snapshots for old invoice readability.
- Per-line billing rate override that does not mutate product master price.
- Stock tracking must be optional per product so the owner tracks important/high-value/fast-moving items without being forced to count every small item.

## Privacy and Security Expectations

- The app should be offline-capable but cloud-backed for ownership, recovery, and future multi-device operation.
- Firebase Auth/Firestore must not sync sensitive data until security rules, ownership, privacy, migration, and conflict controls are implemented and verified.
- Customer and sales data must be treated as sensitive business records.
- Firebase Auth is the runtime owner identity provider. Any future PIN/app-lock or credential hardening needs an owner-approved packet.
- Database migrations must preserve existing shop data.
- Backup/export/import must require explicit user action and must make data destination clear.
- QR image URI access must be tested across restarts and Android versions.
- Release builds must not require development `.env` secrets unless a documented feature uses them.

## Product Boundaries

In scope for professional delivery:

- Firebase owner auth/setup with recoverable shop profile.
- Firebase Auth owner sign-in, shop ownership, and Firestore-backed recovery foundation.
- Firestore security rules and privacy model.
- Local Room cache/offline strategy with explicit conflict handling.
- English/Hindi resources with no random Hinglish.
- Product/category/stock management.
- Stock-by-exception: per-product stock tracking can be enabled or left untracked.
- Billing with auditable invoice data.
- Fast cash/UPI sale without forced customer or invoice.
- Loose item billing with amount-to-quantity and quantity-to-amount calculation.
- Smart product quick-add during billing, with later cleanup.
- Cash, manual UPI, and udhaar records.
- Quick udhaar ledger, payment entry, and WhatsApp/share reminder text.
- Owner-private stock value and profit reports.
- End-of-day "Aaj ka hisaab" daily close summary.
- PDF invoice generation and share workflow after billing correctness is stable.
- Backup/export/import after invoice workflow.
- Manual backup/export/import only if still required after cloud recovery decisions.

Out of scope until explicitly approved:

- Bluetooth thermal printer support.
- Multi-store or multi-branch support.
- Staff roles and permissions implementation. Modeling future membership/roles is allowed inside Firebase foundation planning.
- Real UPI confirmation.
- Loan/credit scoring.
- GST/tax filing or legal compliance claims beyond owner-approved user-entered invoice fields.
- AI-generated business decisions.

## Accepted Foundation Decisions

- Final Android application id and namespace are `com.harrylabs.shreeshyamstore`.
- Firestore becomes canonical over time while Room remains the local working cache/offline store.
- Room v5 is implemented: syncable rows use UUID primary keys, the legacy local `users` table is dropped by migration from v3 to v4, and v4 to v5 adds the retryable local sync outbox without deleting business data.
- Money uses `Long` paise and quantity uses `Long` base units: count, grams, and ml.
- Per-line billing rate override is part of the accepted billing foundation and must not mutate product master pricing.
- Cash and manual UPI sales should not force customer selection.
- Invoice generation is optional and user-triggered after sale.
- One active shop is exposed in the UI for now; multi-store UI is deferred even though the cloud membership model can support future expansion.
- Profit must use sale-item purchase-cost snapshots, not the current product purchase price for old sales.

## Remaining Owner Decisions

- Firebase project/config status: project `shreeshyamstore`, Android app `com.harrylabs.shreeshyamstore`, `google-services.json`, Firestore `(default)` in `asia-south1`, and debug SHA registration are configured. Remaining owner decisions: dev/prod separation, delete protection/PITR posture, budget/cost guardrails, and App Check enforcement timing.
- `TBD - owner decision required`: confirm/complete live Firebase Authentication Google provider enablement and manual owner-account sign-in QA. Phone OTP remains later/optional.
- `TBD - owner decision required`: App Check enforcement timing, Firebase cost monitoring, backup/export policy, and retention expectations.
- `TBD - owner decision required`: whether to remove all AI Studio/Gemini scaffolding and unused network dependencies.
- `TBD - owner decision required`: Android backup policy for customer/sales data before release.
- `TBD - owner decision required`: exact invoice format, logo/QR placement, and required legal/tax fields.
- `TBD - owner decision required`: backup/export/import file format and restore conflict behavior.
- `TBD - owner decision required`: whether GST/tax support is needed for the target shop.
- `TBD - owner decision required`: whether purchase/supplier workflow is required before public release.
