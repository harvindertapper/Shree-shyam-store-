# DM-004 Foundation Reset Plan

Status: Source of truth for foundation reset before real shop inventory entry and Billing Phase 2.

## Owner Decisions Locked

- Real shop inventory will be entered only after Firebase Auth/shop profile, Firestore rules/App Check/cost guardrails, Product Add/Edit unit/rate/stock UI, product/category/settings sync, and inventory restore QA pass.
- Room v2 reset is allowed because no real shop inventory has been entered yet.
- Final Android app identity is `com.harrylabs.shreeshyamstore`.
- Firebase app registration, Google Sign-In, SHA keys, and Play Store identity must use `com.harrylabs.shreeshyamstore`.
- Do not configure Firebase with the old random application id.
- Money uses `Long` paise for new billing logic.
- Quantity uses `Long` base units: pieces for count, grams for weight, and ml for volume.
- Product rate uses `pricePerUnitPaise` plus `priceUnitBaseQty`.
- Rounding policy: quantity-to-amount saves exact paise; amount-to-quantity rounds to nearest gram/ml; fractional pieces are blocked.
- UPI remains a manual payment record. Do not add real payment verification.

## Implementation Order

1. `FR-A-V2-DATA-CALC-DESIGN` — completed at `0bd189b`.
2. `FR-C-QUANTITY-PRICE-CALCULATOR` — completed at `f36d613`.
3. `FR-B-ROOM-V2-RESET` — completed at `4bb4927`.
4. `FR-P-APP-IDENTITY-RENAME` — completed at `b7d92f2`.
5. `FR-G-FIREBASE-AUTH-SHOP-PROFILE`
6. `FR-G2-FIRESTORE-RULES-APP-CHECK-COST-GUARDRAILS`
7. `FR-D-PRODUCT-UNIT-STOCK-UI`
8. `FR-H-PRODUCT-CATEGORY-SETTINGS-SYNC`
9. `FR-I1-INVENTORY-RESTORE-QA`
10. `FR-E-BILLING-WEIGHTED-ENTRY-RATE-OVERRIDE`
11. `FR-F-SALE-STOCK-INVOICE-UDHAAR-PERSISTENCE`
12. `FR-I2-BILLING-RESTORE-QA`
13. `FR-J-SALES-STOCK-UDHAAR-CLOUD-SYNC-PLAN`

## Real Inventory Gate

Owner can start real product/category/settings entry only after:

- `FR-G-FIREBASE-AUTH-SHOP-PROFILE`
- `FR-G2-FIRESTORE-RULES-APP-CHECK-COST-GUARDRAILS`
- `FR-D-PRODUCT-UNIT-STOCK-UI`
- `FR-H-PRODUCT-CATEGORY-SETTINGS-SYNC`
- `FR-I1-INVENTORY-RESTORE-QA`

Real billing use waits until:

- `FR-E-BILLING-WEIGHTED-ENTRY-RATE-OVERRIDE`
- `FR-F-SALE-STOCK-INVOICE-UDHAAR-PERSISTENCE`
- `FR-I2-BILLING-RESTORE-QA`

## Final App Identity Gate

- Current code uses the final `com.harrylabs.shreeshyamstore` identity after `FR-P-APP-IDENTITY-RENAME`.
- `FR-P` sets `applicationId` to `com.harrylabs.shreeshyamstore`.
- `FR-P` sets Android namespace to `com.harrylabs.shreeshyamstore`.
- `FR-P` renames Kotlin packages to `com.harrylabs.shreeshyamstore`.
- `FR-P` updates package assertions, launch commands, imports, and current docs.
- `FR-G` must not start until `FR-P` passes.

## V2 Calculation Contract

Money:

- Store money as `Long` paise.
- Do not use `Double` for new permanent billing, invoice, udhaar, product price, or report totals.
- UI may parse decimal text into paise, but saved entities store paise.

Quantity:

- Piece products use count as base quantity.
- Weight products use grams as base quantity.
- Volume products use ml as base quantity.
- Packet, box, and custom units are deferred but schema should not block them.

Rate:

- `pricePerUnitPaise` stores the rate amount.
- `priceUnitBaseQty` stores the base quantity for that rate.
- Example: `Rs.47/kg` is `pricePerUnitPaise = 4700`, `priceUnitBaseQty = 1000`.
- Example: `Rs.25/piece` is `pricePerUnitPaise = 2500`, `priceUnitBaseQty = 1`.

Rounding:

- Quantity to amount: `lineTotalPaise = roundHalfUp(quantityBase * pricePerUnitPaise / priceUnitBaseQty)`.
- Amount to quantity: `quantityBase = roundHalfUp(amountPaise * priceUnitBaseQty / pricePerUnitPaise)`.
- Amount-to-quantity rounds to nearest gram/ml for weight and volume.
- Fractional pieces are invalid.
- Zero, negative, invalid decimal, unsupported precision, and missing rate values are invalid.

Required examples:

- `Rs.47/kg`, `160g` -> `Rs.7.52`.
- `Rs.47/kg`, `Rs.30` -> about `638g`.

## Per-Line Rate Override

Per-line override is mandatory in billing:

- Product master rate remains unchanged.
- Cart line stores original product rate and effective billed rate.
- Calculator uses effective billed rate.
- Sale item snapshots both rates and whether an override was applied.
- Cart clearly shows override, for example `Rs.22/kg, discounted from Rs.25/kg`.
- Invoice shows effective billed rate.
- Applies to weight/volume products and may apply to piece products.

## V2 Sync And Audit Fields

All cloud-syncable local records should plan these fields unless a packet documents why not:

- `localUuid`
- `remoteId`
- `shopId`
- `syncStatus`
- `deletedAt`
- `lastSyncedAt`
- `createdByUid`
- `updatedByUid`
- `sourceDeviceId`

Sale/bill records also need:

- `deviceId`
- `billSequence`
- `idempotencyKey`
- `createdAt`
- `saleStatus`

The idempotency model must prevent duplicate bills after retry or sync.

## Soft Delete

- Products, categories, and customers should use `isActive` plus `deletedAt`.
- Do not hard delete syncable business records by default.
- Sales, sale items, stock adjustments, and udhaar transactions should remain append-only except for future owner-approved correction workflows.

## Sale Status And Profit Foundation

Plan sale status now, but do not implement return/refund UI:

- `COMPLETED`
- `CANCELLED`
- `RETURNED`
- `REFUNDED`

Sale item snapshots must include purchase cost when available:

- `purchasePricePerUnitPaiseSnapshot`
- `purchasePriceUnitBaseQtySnapshot`

This protects future profit reporting after product purchase cost changes.

## Cloud Asset Policy

- Current QR/logo/image URI behavior is local and not portable across reinstall or device change.
- Do not treat Android content URI values as long-term cloud assets.
- Plan Firebase Storage later for portable shop logo/QR assets.
- Do not touch welcome sound/assets in this foundation reset.

## Firebase Config Decisions Required Before FR-G

- Firebase project id.
- Dev/prod project policy.
- Firestore region.
- SHA-1 and SHA-256 keys for `com.harrylabs.shreeshyamstore`.
- `google-services.json` handling.
- No service-account keys in repo.
- Budget/cost guardrails.
- App Check strategy.

## Current Foundation State

- Room is schema version 2 with the approved v1-only `fallbackToDestructiveMigrationFrom(true, 1)` reset.
- Weight/volume calculation and per-line effective-rate foundations exist in pure Kotlin and Room v2 fields.
- Product and Billing UI integration remains pending.
- Firebase Auth, Credential Manager Sign in with Google, Firestore integration, cloud restore, rules deployment, and App Check are not implemented.
- The welcome sound issue remains tracked separately and is not part of the Firebase or foundation cleanup route.

## Explicitly Out Of Scope For This Reset

- Returns/refunds UI.
- Staff UI/permissions.
- Barcode.
- GST.
- Printer.
- PDF.
- WhatsApp share.
- Bulk import.
- Real UPI/payment verification.
- Sales/stock/udhaar cloud sync implementation.
- Welcome sound/assets.
