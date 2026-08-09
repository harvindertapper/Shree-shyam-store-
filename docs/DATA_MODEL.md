# Data Model

## Storage

The app uses a local Room database named:

```text
shree_shyam_store_db
```

Settings/session use Android DataStore.

Owner decision on 2026-06-17: Firebase Auth and Firestore/cloud sync are mandatory professional foundation. The accepted split makes Firestore canonical over time while Room remains the local working cache/offline transaction store.

M02F-A/DM-004 architecture decision: use a hybrid phased migration with Firestore as the canonical cloud source of truth over time and Room as the local working cache/offline transaction store. See `docs/governance/FIREBASE_CLOUD_SYNC_ARCHITECTURE.md` and `docs/governance/FOUNDATION_RESET_DM004.md`.

DM-004 foundation reset decision: no real shop inventory has been entered yet, so the approved early reset/migration path is allowed before real inventory entry. Model work must use `Long` paise for money, `Long` base units for quantities, cloud sync fields, soft delete where appropriate, and final app identity `com.harrylabs.shreeshyamstore`.

FR-K/FR-G/PD-03 Room v5 UUID, auth, and sync-outbox implementation:

- `AppDatabase` is now Room schema version 5.
- All syncable entities (Category, Product, Sale, SaleItem, Customer, UdhaarTransaction, and StockAdjustment) use a client-generated `String` `localUuid` as their `@PrimaryKey`, and legacy `id: Long` columns have been completely removed.
- Relationships (foreign keys and references) between these entities now use `String` UUIDs.
- `sync_outbox_operations` stores retryable local cloud-sync markers with `clientOperationId`, `shopId`, `entityType`, `entityUuid`, `sourceDeviceId`, `createdByUid`, `syncStatus`, `retryCount`, and `lastError`.
- The transition from v1 and v2 uses the explicit approved `fallbackToDestructiveMigrationFrom(true, 1, 2)` reset only, based on the owner decision that no real shop inventory has been entered yet.
- The broad production `fallbackToDestructiveMigration()` policy is not retained for future versions.
- Default seeded categories are recreated on fresh database create and on the approved reset.
- Version 3 to 4 uses an intentional migration that drops the deprecated local users table after Firebase Auth becomes the runtime owner identity provider.
- Version 4 to 5 uses an intentional migration that adds the local sync outbox without deleting inventory, credit, sale, or stock data.
- Future Room changes from v5 onward require intentional migrations.
- Legacy `Double` money and `Int` quantity/stock columns remain temporarily as compatibility fields for existing Product/Billing UI code. The `Long` paise and base-unit columns are present now and are synchronized by repository inserts/updates until later UI packets migrate fully to the current contract.

## Entities

## Category

Table: `categories`

Purpose:

- Product grouping such as Grocery, Dairy, Snacks, Household.

Fields:

- `localUuid` (String, PrimaryKey)
- `name`
- `createdAt`
- `updatedAt`

## Product

Table: `products`

Purpose:

- Sellable shop item.

Fields:

- `localUuid` (String, PrimaryKey)
- `name`
- `categoryId` (String UUID)
- `mrp`
- `sellingPrice`
- `purchasePrice`
- `currentStock`
- `trackStock`
- `lowStockAlertQty`
- `isActive`
- `createdAt`
- `updatedAt`

FR-B v2 cloud/unit readiness fields implemented in Room while legacy fields remain for UI compatibility:

- `unitType`
- `displayUnit`
- `baseUnit`
- `allowsDecimalQuantity`
- `quantityScale`
- `stockQuantityBase`
- `lowStockAlertBase`

FR-B v2 fields implemented in Room:

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
- Stock tracking is per product. If `trackStock` is true, billing must reduce stock and prevent negative tracked stock unless a later owner-approved oversell setting exists.
- If `trackStock` is false, the product can be sold without stock blocking or low-stock alerts.
- Quick-added products created during billing should be allowed with minimal required fields and should be flagged for later cleanup when category, purchase price, or stock tracking needs review.
- `purchasePrice` and purchase-price-per-unit fields support private owner reports. Historical profit must use sale-item purchase-cost snapshots, not a later edited product purchase price.

## Sale

Table: `sales`

Purpose:

- One completed bill/invoice.

Fields:

- `localUuid` (String, PrimaryKey)
- `billNumber`
- `totalAmount`
- `paymentMode`
- `customerId` (String UUID, Nullable)
- `note`
- `createdAt`

FR-B v2 fields implemented in Room:

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
- Cash and UPI sales may have no customer.
- Udhaar sale must reference an existing or newly created customer.
- UPI remains a manual payment record, not verified payment.
- Invoice/PDF/share is optional and user-triggered after the sale is saved.
- Bill idempotency must prevent duplicate bills after retry or sync.
- Sale status should be planned for future cancel/return/refund support, without implementing that UI now.

## SaleItem

Table: `sale_items`

Purpose:

- Line items belonging to a sale.

Fields:

- `localUuid` (String, PrimaryKey)
- `saleId` (String UUID)
- `productId` (String UUID)
- `productNameSnapshot`
- `quantity`
- `unitPrice`
- `lineTotal`

Business rules:

- Store product name and price snapshots so old invoices remain readable after product edits.
- Store quantity unit, display text, product unit, and decimal precision so measured goods invoices remain readable after product unit edits.
- Store purchase-cost snapshots so private profit reports remain historically correct after product cost edits.

FR-B v2 sale item snapshot fields implemented in Room:

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
- Amount-to-quantity billing, for example `Rs. 50 sugar @ Rs. 47/kg`, must store the calculated base quantity and entered amount/line total in auditable sale-item fields.

## Customer

Table: `customers`

Purpose:

- Customer record for udhaar and future contact shortcuts.

Fields:

- `localUuid` (String, PrimaryKey)
- `name`
- `phone`
- `createdAt`
- `updatedAt`

FR-B v2 fields implemented in Room:

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

- `localUuid` (String, PrimaryKey)
- `customerId` (String UUID)
- `saleId` (String UUID, Nullable)
- `type`
- `amount`
- `note`
- `createdAt`

FR-B v2 fields implemented in Room:

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
- Quick udhaar entries are normal append-only ledger transactions.
- WhatsApp/share reminders are generated from customer balance data but are only sent through explicit user share action.

## StockAdjustment

Table: `stock_adjustments`

Purpose:

- Audit trail for stock changes.

Fields:

- `localUuid` (String, PrimaryKey)
- `productId` (String UUID)
- `oldStock`
- `newStock`
- `difference`
- `reason`
- `createdAt`

FR-B v2 fields implemented in Room:

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

## User (Removed in Room v4)

Table: `users` (Dropped in version 4 migration)

Purpose:
- Local owner login has been deprecated in favor of Firebase Authentication.

## Cloud Firestore Profile Schema

Firestore acts as the canonical store for user profiles, shop registration details, and ownership memberships. All document creations are performed atomically in a Firestore Transaction.

### User Profile (`users/{uid}`)
- `uid` (String): Google authenticated unique user ID.
- `email` (String): User's authenticated email (must match Google Auth token).
- `displayName` (String): Display name of the owner (length <= 100).
- `activeShopId` (String, Nullable): ID of the currently active/selected shop.
- `createdAt` (Timestamp): Firestore server-assigned creation timestamp.

### Shop Profile (`shops/{shopId}`)
- `shopId` (String): Unique UUID of the shop.
- `name` (String): Local shop name (length <= 100).
- `ownerPhone` (String): Owner's contact number (length 10-15 digits).
- `ownerUid` (String): Reference to the owner's `uid`.
- `createdAt` (Timestamp): Firestore server-assigned creation timestamp.

### Shop Membership (`shops/{shopId}/members/{uid}`)
- `uid` (String): Member's unique user ID.
- `shopId` (String): Reference to the shop ID.
- `role` (String): Static role, always `'owner'` for shop registration.
- `status` (String): Static status, always `'active'`.
- `createdAt` (Timestamp): Firestore server-assigned creation timestamp.

## DataStore Settings

Purpose:
- Store app-level preferences and local session state.

Fields:
- `shopName` (String)
- `ownerPhone` (String)
- `staticPaytmQrImageUri` (String)
- `welcomeChantEnabled` (Boolean)
- `firstLaunchCompleted` (Boolean)
- `selectedLanguage` (String)
- `cachedOwnerUid` (String): Saved UID of the logged-in owner. Used to detect account switches.
- `cachedShopId` (String): Saved active shop ID. Used to detect shop transitions.

*Note: Legacy `loggedInUsername`, `loggedInEmail`, and `isUserLoggedIn` are deprecated as Firebase Auth is the runtime authority.*

Needed future fields:

- `selectedLanguage`, default `en`, supported values `en` and `hi`.
- Private Owner Desk/app-lock settings if owner approves local PIN/biometric gating. Raw PINs, tokens, and secrets must not be stored.

## Planned Cloud Source Of Truth

The accepted architecture uses a hybrid phased migration: Firestore becomes canonical over time, Room remains the local cache/offline queue, and no sensitive broad sync starts until security, migration, and conflict rules are implemented and verified.

Accepted architecture boundaries:

- Firebase Auth identifies the signed-in owner by UID.
- Firestore stores cloud-backed business records under an owner/shop boundary.
- One owner may belong to one or more shops in the future, but multi-shop UI is deferred.
- Room remains the local cache/offline working store unless an approved migration changes this.
- DataStore remains preferences/session-local state only and must not store secrets or server tokens.

Candidate Firestore collections under the accepted architecture; exact document shape remains an implementation-packet decision:

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
- Offline write queue, visible pending/error sync state, restore gate behavior, retry, idempotency, and conflict resolution policy.
- Per-shop membership/ownership checks.
- Security rules, App Check, abuse/cost controls, and privacy logging boundaries.
- Final Firebase app registration must use `com.harrylabs.shreeshyamstore`, not the old random application id.
- Product/category/settings sync must pass restore QA before real inventory entry.
- Customer/udhaar sync and restore must pass before real credit records are trusted.
- Sales, sale items, stock adjustments, and invoice metadata must sync with idempotency before billing restore is considered trustworthy.
- QR/logo/image assets should move to Firebase Storage later; Android local URI values are not portable long-term.

Recommended conflict posture:

- Shop profile/settings may use last-write-wins only with `updatedAt`, `updatedByUid`, and `sourceDeviceId`.
- Sales, sale items, stock adjustments, and udhaar transactions should be append-only cloud events with client operation ids for idempotency.
- Product stock must not rely only on blind last-write-wins when multi-device use is enabled.
- Reports should remain derived from canonical sale, stock, payment, and udhaar records.

## Derived Private Business Views

These are derived views/calculations, not canonical write records:

- Total stock value by selling price.
- Category-wise stock value by selling price.
- Purchase-cost stock value where purchase price is known.
- Today/month/custom-range profit from sale-item purchase-cost snapshots.
- Unknown-cost sales bucket where purchase price snapshots are missing.
- End-of-day "Aaj ka hisaab" summary: cash, manual UPI, udhaar added, udhaar payment received, total sale, low stock, top items, pending sync, and profit where private access is allowed.

Privacy rules:

- Purchase price, profit, and stock value must not appear in customer-facing billing screens.
- Owner-private views may require an app-lock/PIN/biometric packet before release.

## Migration Rules

- Do not change entity fields without planning a Room migration.
- Do not rely on destructive migration for production app updates.
- When adding fields, prefer nullable/default values and explicit migration.
- FR-B introduced the accepted v2 sync metadata fields, FR-K moved syncable tables to UUID primary keys, FR-G removed the runtime local auth table in Room v4, and PD-03 added the Room v5 local sync outbox. Future Room fields or schema changes require an intentional migration and the relevant implementation packet.
- Do not migrate current `Int` stock and sale item quantities to measured/decimal quantities without an approved unit/base-quantity migration plan.
- DM-004 exception applied in FR-B: because no real shop inventory has been entered yet, Room v1 may reset to v2 after FR-A and FR-C acceptance. This is scoped to v1 only and must not be reused as a broad production destructive-migration policy.

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
