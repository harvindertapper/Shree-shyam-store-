# Data Model

## Storage

The app uses a local Room database named:

```text
shree_shyam_store_db
```

Settings/session use Android DataStore.

Owner decision on 2026-06-17: Firebase Auth and Firestore/cloud sync are mandatory MVP foundation. The final source-of-truth split between Firestore and local Room cache is TBD and must be decided before Billing Phase 2 implementation.

M02F-A/DM-004 architecture decision: use a hybrid phased migration with Firestore as the canonical cloud source of truth over time and Room as the local working cache/offline transaction store. See `docs/governance/FIREBASE_CLOUD_SYNC_ARCHITECTURE.md` and `docs/governance/FOUNDATION_RESET_DM004.md`.

DM-004 foundation reset decision: no real shop inventory has been entered yet, so Room v2 reset is allowed before real inventory entry. New v2 model work must use `Long` paise for money, `Long` base units for quantities, cloud sync fields, soft delete where appropriate, and final app identity `com.harrylabs.shreeshyamstore`.

## Entities

## Category

Table: `categories`

Purpose:

- Product grouping such as Grocery, Dairy, Snacks, Household.

Fields:

- `id`
- `name`
- `createdAt`
- `updatedAt`

## Product

Table: `products`

Purpose:

- Sellable shop item.

Fields:

- `id`
- `name`
- `categoryId`
- `mrp`
- `sellingPrice`
- `purchasePrice`
- `currentStock`
- `trackStock`
- `lowStockAlertQty`
- `isActive`
- `createdAt`
- `updatedAt`

Future cloud/unit readiness, not implemented in Room yet:

- `unitType`
- `displayUnit`
- `baseUnit`
- `allowsDecimalQuantity`
- `quantityScale`
- `stockQuantityBase`
- `lowStockAlertBase`

DM-004 v2 target fields:

- `localUuid`
- `remoteId`
- `shopId`
- `syncStatus`
- `deletedAt`
- `lastSyncedAt`
- `createdByUid`
- `updatedByUid`
- `sourceDeviceId`
- `pricePerUnitPaise`
- `priceUnitBaseQty`
- `purchasePricePerUnitPaise`
- `purchasePriceUnitBaseQty`
- `stockQuantityBase`
- `lowStockAlertBase`

Business rules:

- Effective sale price is `sellingPrice` when positive, otherwise `mrp`.
- If `trackStock` is true, billing must reduce stock.
- Future billing should prevent negative tracked stock.
- `purchasePrice` should be used for profit reports.

## Sale

Table: `sales`

Purpose:

- One completed bill/invoice.

Fields:

- `id`
- `billNumber`
- `totalAmount`
- `paymentMode`
- `customerId`
- `note`
- `createdAt`

DM-004 v2 target fields:

- `localUuid`
- `remoteId`
- `shopId`
- `deviceId`
- `billSequence`
- `idempotencyKey`
- `createdByUid`
- `updatedByUid`
- `syncStatus`
- `deletedAt`
- `lastSyncedAt`
- `totalAmountPaise`
- `saleStatus`

Business rules:

- `paymentMode` values currently used: `CASH`, `UPI`, `UDHAAR`.
- UPI currently means manually recorded UPI sale, not verified payment.
- Udhaar sale should reference a customer.
- UPI remains a manual payment record, not verified payment.
- Bill idempotency must prevent duplicate bills after retry or sync.
- Sale status should be planned for future cancel/return/refund support, without implementing that UI now.

## SaleItem

Table: `sale_items`

Purpose:

- Line items belonging to a sale.

Fields:

- `id`
- `saleId`
- `productId`
- `productNameSnapshot`
- `quantity`
- `unitPrice`
- `lineTotal`

Business rules:

- Store product name and price snapshots so old invoices remain readable after product edits.
- Future sale-item snapshots must also store quantity unit, display text, product unit, and decimal precision so measured goods invoices remain readable after product unit edits.

DM-004 v2 sale item snapshot fields:

- `localUuid`
- `remoteId`
- `shopId`
- `syncStatus`
- `deletedAt`
- `lastSyncedAt`
- `createdByUid`
- `updatedByUid`
- `sourceDeviceId`
- `unitTypeSnapshot`
- `displayUnitSnapshot`
- `baseUnitSnapshot`
- `enteredQuantityText`
- `quantityBase`
- `originalPricePerUnitPaise`
- `originalPriceUnitBaseQty`
- `effectivePricePerUnitPaise`
- `effectivePriceUnitBaseQty`
- `rateOverridden`
- `lineTotalPaise`
- `purchasePricePerUnitPaiseSnapshot`
- `purchasePriceUnitBaseQtySnapshot`

Per-line rate override rules:

- Override is stored on the cart/sale line only.
- Override must not change the product master rate.
- Calculator and line total use the effective billed rate.
- Invoice shows the effective billed rate.

## Customer

Table: `customers`

Purpose:

- Customer record for udhaar and future contact shortcuts.

Fields:

- `id`
- `name`
- `phone`
- `createdAt`
- `updatedAt`

DM-004 v2 target fields:

- `localUuid`
- `remoteId`
- `shopId`
- `syncStatus`
- `deletedAt`
- `lastSyncedAt`
- `createdByUid`
- `updatedByUid`
- `sourceDeviceId`
- `isActive`

## UdhaarTransaction

Table: `udhaar_transactions`

Purpose:

- Credit and payment ledger for customers.

Fields:

- `id`
- `customerId`
- `saleId`
- `type`
- `amount`
- `note`
- `createdAt`

DM-004 v2 target fields:

- `localUuid`
- `remoteId`
- `shopId`
- `syncStatus`
- `deletedAt`
- `lastSyncedAt`
- `createdByUid`
- `updatedByUid`
- `sourceDeviceId`
- `amountPaise`

Business rules:

- `CREDIT` increases customer balance.
- `PAYMENT` decreases customer balance.
- Udhaar sale creates a `CREDIT` transaction.
- Payment entry creates a `PAYMENT` transaction.

## StockAdjustment

Table: `stock_adjustments`

Purpose:

- Audit trail for stock changes.

Fields:

- `id`
- `productId`
- `oldStock`
- `newStock`
- `difference`
- `reason`
- `createdAt`

DM-004 v2 target fields:

- `localUuid`
- `remoteId`
- `shopId`
- `syncStatus`
- `deletedAt`
- `lastSyncedAt`
- `createdByUid`
- `updatedByUid`
- `sourceDeviceId`
- `oldQuantityBase`
- `newQuantityBase`
- `differenceBase`
- `displayUnitSnapshot`

Business rules:

- Opening stock creates adjustment.
- Manual correction creates adjustment.
- Billing tracked products creates adjustment.

## User

Table: `users`

Purpose:

- Local owner login.

Fields:

- `id`
- `username`
- `email`
- `passwordHash`
- `createdAt`

Security note:

- Current password storage is basic SHA-256.
- Future production work should improve local credential handling.

## DataStore Settings

Purpose:

- Store app-level preferences and login session.

Current fields:

- `shopName`
- `ownerPhone`
- `staticPaytmQrImageUri`
- `welcomeChantEnabled`
- `firstLaunchCompleted`
- `loggedInUsername`
- `loggedInEmail`
- `isUserLoggedIn`

Needed future field:

- `selectedLanguage`, default `en`, supported values `en` and `hi`.

## Planned Cloud Source Of Truth

Firebase planning must define ownership and sync before implementation. M02F-A recommends hybrid phased migration: Firestore canonical over time, Room local cache/offline queue, and no sensitive broad sync until security, migration, and conflict rules are accepted.

Working assumptions until approved:

- Firebase Auth identifies the signed-in owner by UID.
- Firestore stores cloud-backed business records under an owner/shop boundary.
- One owner may belong to one or more shops in the future, but multi-shop UI is deferred.
- Room remains the local cache/offline working store unless an approved migration changes this.
- DataStore remains preferences/session-local state only and must not store secrets or server tokens.

Candidate Firestore collections, pending architecture approval:

- `users/{uid}`
- `shops/{shopId}`
- `shops/{shopId}/members/{uid}`
- `shops/{shopId}/settings/main`
- `shops/{shopId}/categories/{categoryId}`
- `shops/{shopId}/products/{productId}`
- `shops/{shopId}/customers/{customerId}`
- `shops/{shopId}/sales/{saleId}`
- `shops/{shopId}/sales/{saleId}/items/{itemId}`
- `shops/{shopId}/udhaarTransactions/{transactionId}`
- `shops/{shopId}/stockAdjustments/{adjustmentId}`

Cloud model rules to settle before implementation:

- Canonical ID strategy for Room rows versus Firestore documents.
- Cloud product documents must support future unit-of-measure and decimal quantity fields before product/billing hardening locks a piece-only model.
- Cloud sale item documents must store product, quantity, unit, price, and line-total snapshots.
- Initial migration flow from existing local Room data to a new cloud shop.
- Restore flow after clear storage/reinstall/device change.
- Offline write queue and conflict resolution policy.
- Per-shop membership/ownership checks.
- Security rules, App Check, abuse/cost controls, and privacy logging boundaries.
- Final Firebase app registration must use `com.harrylabs.shreeshyamstore`, not the old random application id.
- Product/category/settings sync must pass restore QA before real inventory entry.
- QR/logo/image assets should move to Firebase Storage later; Android local URI values are not portable long-term.

Recommended conflict posture:

- Shop profile/settings may use last-write-wins only with `updatedAt`, `updatedByUid`, and `sourceDeviceId`.
- Sales, sale items, stock adjustments, and udhaar transactions should be append-only cloud events with client operation ids for idempotency.
- Product stock must not rely only on blind last-write-wins when multi-device use is enabled.
- Reports should remain derived from canonical sale, stock, payment, and udhaar records.

## Migration Rules

- Do not change entity fields without planning a Room migration.
- Do not rely on destructive migration for production app updates.
- When adding fields, prefer nullable/default values and explicit migration.
- Do not introduce cloud sync fields, remote IDs, or sync metadata into Room entities until the Room-to-cloud migration strategy is approved.
- Do not migrate current `Int` stock and sale item quantities to measured/decimal quantities without an approved unit/base-quantity migration plan.
- DM-004 exception: because no real shop inventory has been entered yet, FR-B may reset Room from v1 to v2 after FR-A and FR-C accept the v2 contract. This reset must be explicitly documented and verified.

## DM-004 Calculator Rules

- Money is stored as `Long` paise.
- Quantity is stored as `Long` base units.
- Piece base unit is count.
- Weight base unit is grams.
- Volume base unit is ml.
- Product rate is `pricePerUnitPaise` plus `priceUnitBaseQty`.
- `Rs.47/kg` is represented as `4700` paise per `1000` grams.
- Quantity-to-amount stores exact paise with half-up division rounding when division is needed.
- Amount-to-quantity rounds to nearest gram/ml for weight and volume.
- Fractional pieces are invalid.
- Invalid, zero, negative, or unsupported precision inputs are invalid.
