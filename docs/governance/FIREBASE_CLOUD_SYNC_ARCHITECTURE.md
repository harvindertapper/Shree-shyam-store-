# Firebase Cloud Sync Architecture

Status: M02F-A recommendation accepted by owner as part of DM-004 foundation reset.

## Summary Recommendation

Use a hybrid phased migration that moves Shree Shyam Store toward Firestore as the canonical cloud source of truth while keeping Room as the local working cache and offline transaction store.

This is the safest MVP path for a real kiryana shop because the current app already uses Room for products, sales, stock, customers, and udhaar, while the owner requirement now requires cloud-backed recovery after clear storage, reinstall, phone loss, and future device change.

Do not implement Firebase code until the config, Auth, Firestore data model, security rules, migration, offline/conflict, and QA packets are accepted.

Final app identity gate:

- Final Android application id, namespace, Firebase app registration, Google Sign-In setup, SHA keys, and Play Store identity must use `com.harrylabs.shreeshyamstore`.
- Current code uses `com.harrylabs.shreeshyamstore` after completed packet `FR-P` at `b7d92f2`.

## Alternatives Considered

### Firestore Canonical With Room Cache

Recommended end state.

Pros:

- Gives the owner cloud-backed recovery and future multi-device support.
- Keeps a local cache for fast counter use and offline billing.
- Allows sensitive domains such as sales, stock, and udhaar to be synced only after rules and migration are ready.
- Fits the existing Room-heavy app without a risky rewrite.

Cons:

- Requires explicit sync metadata, conflict handling, migration, and QA.
- Requires careful security rules and shop membership checks.
- Adds more architecture work before Billing Phase 2.

### Firestore Direct With SDK Offline Persistence

Not recommended as the immediate MVP architecture.

Pros:

- Less local database sync code in a newly built app.
- Firestore SDK can support local cache behavior for simple documents.

Cons:

- The current app is already Room-based.
- Billing, stock, sale item snapshots, and udhaar need auditable local transactions.
- A direct rewrite would be too broad before billing correctness is stabilized.
- Multi-device stock and udhaar conflicts still need product-specific rules.

### Local Room With Manual Backup Only

Rejected for MVP foundation.

Pros:

- Lowest short-term implementation effort.
- Keeps current app architecture almost unchanged.

Cons:

- Does not satisfy the owner decision that local-only storage is unacceptable.
- Does not reliably protect against clear storage, phone loss, reinstall, or device change.
- Does not prepare future staff or multi-device operation.

## Account And Shop Model

Recommended cloud identity boundary:

- `users/{uid}`: minimal Firebase Auth user profile and metadata.
- `shops/{shopId}`: shop profile, created by the owner after sign-in or restored after reinstall.
- `shops/{shopId}/members/{uid}`: membership document with `role`, `status`, `createdAt`, `updatedAt`, and future permission flags.
- Optional later index: `users/{uid}/shopMemberships/{shopId}` for faster shop lookup after login.

MVP behavior:

- Firebase Auth UID identifies the signed-in owner.
- First signed-in owner creates a shop and receives `role = "owner"`.
- On reinstall, clear storage, or new device, the owner signs in, the app loads memberships for that UID, and restores the active shop profile.
- Only one active shop should be exposed in UI for now. Multi-store UI remains deferred.
- Staff membership may be modeled in documents now, but staff-role UI and permission management remain deferred.

Remaining implementation decision:

- Whether the existing local Room `User` table remains a transitional local login fallback, is linked to the Firebase UID, or is replaced by Firebase Auth in the MVP path.

## Data-Domain Strategy

All cloud business data must be scoped by `shopId`.

| Domain | Recommended cloud ownership | Local cache strategy | Notes |
| --- | --- | --- | --- |
| Shop profile/settings | `shops/{shopId}` and `shops/{shopId}/settings/main` | DataStore for local preferences, Room/cache as needed | First safe sync domain. |
| Categories | `shops/{shopId}/categories/{categoryId}` | Room cache | Low-risk after rules/model accepted. |
| Products | `shops/{shopId}/products/{productId}` | Room cache | Must include future unit/measurement fields before billing hardening locks assumptions. |
| Stock adjustments | `shops/{shopId}/stockAdjustments/{adjustmentId}` | Room append-only cache | Treat as auditable events, not overwrite-only stock state. |
| Sales | `shops/{shopId}/sales/{saleId}` | Room append-only cache | Immutable completed bill record. |
| Sale items | `shops/{shopId}/sales/{saleId}/items/{itemId}` or flat `saleItems` if query needs demand it | Room cache | Store item snapshots for invoice history. |
| Customers | `shops/{shopId}/customers/{customerId}` | Room cache | Sensitive customer data; sync only after rules and privacy gates. |
| Udhaar transactions | `shops/{shopId}/udhaarTransactions/{transactionId}` | Room append-only cache | Ledger events should not be silently deleted or rewritten. |
| Reports | Derived from sales, sale items, payments, stock, and udhaar | Local derived calculations | Do not make reports the canonical record. |
| QR/logo/assets | Later Firebase Storage or owner reselect flow | Local URI remains local until approved | Local Android content URIs are not portable across reinstall/device change. |

## Unit And Measurement Readiness

MQA-003 must not be implemented in this packet, but the cloud architecture must leave room for it.

Recommended future product fields:

- `unitType`: `PIECE`, `WEIGHT`, `VOLUME`, `PACKET`, `BOX`, `CUSTOM`.
- `displayUnit`: owner-facing unit such as piece, kg, g, litre, ml, packet, box.
- `baseUnit`: canonical stock unit such as piece, gram, ml, or custom base.
- `allowsDecimalQuantity`: boolean.
- `quantityScale`: approved decimal precision.
- `stockQuantityBase`: canonical stock quantity in base units or a decimal-safe encoded value.
- `lowStockAlertBase`: low-stock threshold in the same canonical unit.

Recommended future sale item snapshots:

- `quantityValue`.
- `quantityUnit`.
- `quantityDisplayText`.
- `unitPriceAmountPaise`.
- `lineTotalAmountPaise`.
- `productUnitSnapshot`.

Architecture rule:

- Do not design Firestore product/sale documents as piece-only.
- Do not change current Room schema until M05/M06 unit work has an approved migration plan.
- Prefer paise for money and canonical base units for quantities in the future cloud model, instead of relying on floating-point values for permanent business records.

## Offline And Conflict Strategy

Practical MVP stance:

- Sign-in, first cloud restore, and first shop creation need internet.
- Once the shop profile and selected domains are cached, the app should remain usable offline for daily counter work.
- Billing should eventually be allowed offline, but only after sales, stock adjustments, and udhaar writes have an accepted local queue, idempotency, and reconciliation plan.
- Until then, the first implementation slice should avoid billing, sales, stock, customers, and udhaar sync.
- Firestore offline persistence is a transport/cache capability, not a complete business-conflict solution. Sales, stock, and udhaar still require idempotency, append-only records, and explicit reconciliation rules.

Conflict policy by risk:

- Shop profile/settings: last-write-wins can be acceptable if `updatedAt`, `updatedByUid`, and `updatedByDeviceId` are tracked.
- Products/categories: use revisions or `updatedAt` checks; avoid silently overwriting price/stock-related fields from another device.
- Sales/sale items: append-only immutable records with client-generated operation ids.
- Stock adjustments: append-only events; product stock state should be recalculated or reconciled from accepted events when needed.
- Udhaar transactions: append-only ledger entries; corrections should be new entries, not destructive edits.

Required sync metadata before broad sync:

- Stable cloud document ids.
- Local-to-cloud id map or cloud id stored locally.
- `localUuid`.
- `shopId`.
- `remoteId`.
- `createdAt`, `updatedAt`, and server timestamp fields.
- `updatedByUid`.
- `createdByUid`.
- `sourceDeviceId`.
- `lastSyncedAt`.
- `deletedAt` where soft delete is supported.
- `updatedByDeviceId`.
- Client operation id/idempotency key for offline queued writes.
- `syncStatus` locally, such as pending, synced, conflict, failed.

## Migration Approach

Fresh Firebase account with no local data:

1. Owner signs in.
2. App creates `users/{uid}` if missing.
3. Owner creates a new `shops/{shopId}` profile.
4. App creates owner membership.
5. App caches the shop profile locally.

Existing local shop data:

1. Owner signs in.
2. App detects local Room data and asks whether to upload existing shop data or create a new empty cloud shop.
3. Preflight summary shows counts for products, customers, sales, sale items, udhaar transactions, stock adjustments, and settings.
4. Migration creates a cloud shop and stable document ids.
5. Migration uploads data domain by domain, preserving sale/stock/udhaar history.
6. Local records are marked or mapped to cloud ids only after successful upload.
7. Duplicate prevention uses local migration id, client operation id, and existing bill numbers/customer/product matching rules.

Restore after reinstall, clear storage, or device change:

1. Owner signs in with Firebase Auth.
2. App loads shop memberships.
3. Owner selects the shop if more than one membership ever exists.
4. App restores shop profile/settings first.
5. Later accepted packets restore products, customers, sales, stock, and udhaar according to the migration/sync plan.

Owner approval required before migration:

- Whether to upload existing local data automatically after consent or require an export-style review.
- Duplicate matching rules.
- What to do if local data and existing cloud shop data both exist.
- Whether manual encrypted backup/export remains required after cloud recovery.

## Security And Privacy Gates

Required before syncing sensitive data:

- Firebase project ownership and environment separation documented.
- Auth provider approved. Default planning assumption: Google Sign-In first; phone OTP later/optional due to cost and abuse risk.
- Firestore security rules enforce signed-in membership on every `shops/{shopId}` path.
- Rules prevent signed-out access and cross-shop reads/writes.
- Owner role is protected from client-side privilege escalation.
- App Check, quota/cost controls, and abuse monitoring are reviewed before production.
- No service-account keys, signing passwords, `.env` secrets, or production credentials are committed.
- Customer names/phones, invoices, udhaar balances, QR URIs, and sales data are not logged or exposed in screenshots/public docs.
- Retention, delete/account recovery, and backup/export expectations are documented.
- Firestore rules have emulator-backed tests before broad data sync is accepted.

## Foundation Implementation Slices

After the app identity rename and Firebase prerequisites are accepted, the first Firebase implementation slice should be:

1. Firebase SDK/config scaffold using owner-approved project/config policy.
2. Firebase Auth using Android Credential Manager Sign in with Google or another explicitly owner-approved provider.
3. Create/restore `users/{uid}`.
4. Create/restore one `shops/{shopId}` profile.
5. Create/restore owner membership.
6. Sync shop profile/settings first.

After Firestore rules, App Check posture, and cost guardrails pass, the next cloud slice must sync product/category/settings before real inventory entry.

Product/category/settings sync must happen before real shop inventory entry. Do not sync sales, stock adjustments, customers, or udhaar in the first Firebase slice.

## Android Firebase Implementation Guidance

- Use the main Firebase modules `firebase-auth` and `firebase-firestore`; do not introduce deprecated KTX module artifacts.
- Use Android Credential Manager for Sign in with Google.
- Use the App Check debug provider only for emulator/development. Play Integrity is the production direction.
- Firebase budget alerts monitor spending but do not hard-cap charges; production cost controls also need quotas, usage monitoring, and conservative query/write design.
- The Firebase BoM entry currently present in Gradle does not mean Auth or Firestore is integrated.

## Owner Decisions Needed

- Firebase project id, Android app registration for `com.harrylabs.shreeshyamstore`, and environment split.
- Whether `google-services.json` can be committed as public client config or must be provisioned another way.
- MVP Auth provider: Google Sign-In recommended; phone OTP later/optional.
- Firestore region and billing/cost guardrails.
- SHA-1 and SHA-256 keys for the final application id.
- App Check strategy.
- Existing local data migration behavior.
- Whether local Room `User` remains transitional or Firebase Auth replaces local login.
- Exact Product UI exposure for packet/box/custom units; the accepted foundation already uses count, grams, and ml base units.
- Retention/delete/account recovery expectations.
- Whether manual encrypted backup/export remains after Firebase recovery.

## Blocked Until Later Packets

- Billing Phase 2.
- Product/stock/billing unit implementation.
- Firebase SDK, Gradle plugin, `google-services.json`, or Kotlin app code.
- Firestore rules deployment.
- Staff role UI or permissions implementation.
- Phone OTP implementation.
- Sync of products, sales, customers, udhaar, stock adjustments, or invoice data.
- Future Room schema changes beyond implemented v2 fields.
- Service-account keys, signing secrets, `.env`, or production credentials in repo.
