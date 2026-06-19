# Product Spec

Status: Draft source of truth. Unknowns are marked `TBD - owner decision required`.

## App Purpose

Shree Shyam Store is a cloud-backed, offline-capable Kotlin Android shop-management app customized for a small Indian kiryana/general store. It should help a shop owner run daily counter operations with recoverable shop data, fast billing, product and stock control, customer udhaar tracking, practical reports, and invoice sharing.

Governance and future work must stay aligned to the Shree Shyam Store kiryana-shop product and native Kotlin Android stack.

The app is not a payment verification system. Cash, UPI, and udhaar entries are business records entered by the user unless a future approved integration verifies payment.

Owner decision on 2026-06-17: cloud sync/auth is mandatory MVP foundation. Firebase prerequisites, Auth/shop restore, rules/App Check/cost controls, and inventory restore gates must pass before Billing Phase 2 hardening.

Owner decision on 2026-06-17: final Android application id, namespace, Firebase app identity, and Play Store identity must be `com.harrylabs.shreeshyamstore`. Firebase Auth, Google Sign-In, SHA keys, and `google-services.json` must not be configured against the old random application id.

Owner decision on 2026-06-17: no real shop inventory has been entered yet. A Room v2 reset is allowed now so the foundation can become Firebase-ready, unit-ready, and billing-ready before real inventory entry.

Owner decision on 2026-06-17: owner will enter real shop inventory only after Firebase Auth/shop profile, Firestore rules/App Check/cost guardrails, Product Add/Edit unit/rate/stock UI, product/category/settings sync, and inventory restore QA pass.

M02F-A/DM-004 architecture decision: use a hybrid phased migration with Firestore as the canonical cloud source of truth over time and Room as the local working cache/offline transaction store. This is accepted for the foundation reset in `docs/governance/FIREBASE_CLOUD_SYNC_ARCHITECTURE.md` and `docs/governance/FOUNDATION_RESET_DM004.md`.

## Target Users

- Primary user: small retail shop owner or trusted family/operator using one Android phone.
- Business context: local kiryana/general-store workflows with quick counter billing, simple inventory, supplier/product growth later, and customer udhaar tracking.
- Language expectation: English by default, Hindi as a proper second app language.
- Data expectation: shop data must survive phone loss, clear storage, reinstall, and device change through an approved cloud ownership and restore model.

## Current Product Inventory From Repo

- Local owner registration and login.
- First-launch shop setup with shop name, owner phone, welcome chant toggle.
- Manual Compose navigation with bottom navigation after auth/setup.
- Local Room database for products, categories, stock, sales, customers, udhaar, and users.
- DataStore settings/session state including selected language.
- Products and stock screens with categories, product add/edit, opening stock, and stock adjustment.
- Billing cart, payment screen, bill success screen, invoice text copy.
- Cash, UPI, and udhaar payment modes.
- Customer udhaar ledger and payment entry.
- Reports screen with sales summaries and history.
- Settings screen with shop profile, language switch, static Paytm QR image URI, welcome chant, and logout.
- The hybrid Firestore/Room architecture is accepted. A Firebase BoM entry exists, but Firebase Auth, Google Sign-In, Firestore integration, cloud restore, rules deployment, and App Check are not implemented yet.

## Core Workflows

1. Owner opens app and reaches Welcome.
2. App routes to Login/Register, Setup, or Home based on local session/setup state.
3. Owner registers or logs in locally.
4. Owner completes first-launch shop setup.
5. Owner adds categories and products, with opening stock when needed.
6. Owner creates a bill from the cart.
7. Owner selects Cash, UPI, or Udhaar payment mode.
8. App saves sale, sale items, stock adjustment records, and udhaar credit when applicable.
9. Owner views/copies invoice text.
10. Owner reviews reports and udhaar balances.
11. Owner edits settings and changes app language.

## Screens and Routes Known From Repo

The current screen state is a sealed class, not a typed navigation graph:

- `Welcome`
- `Login`
- `Register`
- `Setup`
- `Home`
- `Billing`
- `Payment(invoiceTotal)`
- `BillSuccess`
- `Products`
- `AddEditProduct(productId)`
- `OpeningStock`
- `StockAdjustment(productId)`
- `Udhaar`
- `CustomerDetail(customerId)`
- `Reports`
- `Settings`

## Data Handled

- Shop profile: shop name, owner phone, QR image URI, welcome setting.
- Owner account: username, email, password hash.
- Session state: logged-in flags and owner identifiers.
- Product data: product names, categories, MRP, selling price, purchase price, stock, low-stock settings, active state.
- Sales data: bill number, total, payment mode, customer reference, notes, timestamps.
- Invoice items: product snapshots, quantities, unit prices, totals.
- Customer data: names, phone numbers.
- Udhaar ledger: credit/payment transactions and notes.
- Stock audit data: stock adjustment records and reasons.

Future unit/measurement data must support loose kiryana goods without forcing piece-only billing:

- Piece/count products.
- Weight products such as kg/gram.
- Volume products such as litre/ml.
- Packet/box/custom units if approved.
- Decimal quantities for measured goods.
- Sale item quantity/unit snapshots for old invoice readability.
- Per-line billing rate override that does not mutate product master price.

## Privacy and Security Expectations

- The app should be offline-capable but cloud-backed for ownership, recovery, and future multi-device operation.
- Firebase Auth/Firestore must not sync sensitive data until security rules, ownership, privacy, migration, and conflict controls are implemented and verified.
- Customer and sales data must be treated as sensitive business records.
- Password storage must be hardened before production release.
- Database migrations must preserve existing shop data.
- Backup/export/import must require explicit user action and must make data destination clear.
- QR image URI access must be tested across restarts and Android versions.
- Release builds must not require development `.env` secrets unless a documented feature uses them.

## Product Boundaries

In scope for the MVP:

- Stable local auth/setup.
- Firebase Auth owner sign-in, shop ownership, and Firestore-backed recovery foundation.
- Firestore security rules and privacy model.
- Local Room cache/offline strategy with explicit conflict handling.
- English/Hindi resources with no random Hinglish.
- Product/category/stock management.
- Billing with auditable invoice data.
- Cash, manual UPI, and udhaar records.
- Udhaar ledger and reports.
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
- Room v2 and the v1-only reset allowance are implemented.
- Money uses `Long` paise and quantity uses `Long` base units: count, grams, and ml.
- Per-line billing rate override is part of the accepted billing foundation and must not mutate product master pricing.

## Remaining Owner Decisions

- `TBD - owner decision required`: Firebase project id, dev/prod project policy, Firestore region, SHA-1/SHA-256 keys, `google-services.json` handling, budget/cost guardrails, and App Check strategy.
- `TBD - owner decision required`: Auth provider rollout details. Current implementation direction is Android Credential Manager Sign in with Google first; phone OTP remains later/optional.
- `TBD - owner decision required`: App Check enforcement timing, Firebase cost monitoring, backup/export policy, and retention expectations.
- `TBD - owner decision required`: production credential hardening approach for local login.
- `TBD - owner decision required`: whether to remove all AI Studio/Gemini scaffolding and unused network dependencies.
- `TBD - owner decision required`: Android backup policy for customer/sales data before release.
- `TBD - owner decision required`: exact invoice format, logo/QR placement, and required legal/tax fields.
- `TBD - owner decision required`: backup/export/import file format and restore conflict behavior.
- `TBD - owner decision required`: whether GST/tax support is needed for the target shop.
- `TBD - owner decision required`: whether purchase/supplier workflow is required before public release.
