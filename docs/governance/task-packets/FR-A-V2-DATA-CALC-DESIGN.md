# FR-A-V2-DATA-CALC-DESIGN

> **Completed - do not rerun.** Checkpoint: `0bd189b`.

## task_id

FR-A-V2-DATA-CALC-DESIGN

## goal

Lock the v2 data, calculation, app identity, sync, audit, and reset contract before any code implementation.

## scope_paths

Allowed:

- `docs/governance/FOUNDATION_RESET_DM004.md`
- `docs/DATA_MODEL.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/governance/FIREBASE_CLOUD_SYNC_ARCHITECTURE.md`
- `docs/governance/03_DECISION_LOG.md`
- `docs/governance/task-packets/FR-*.md`

## dependencies

- Owner-approved DM-004 direction.
- Phase 1 localization checkpoint remains complete.

## constraints

- Docs and task packets only.
- Final app identity is `com.harrylabs.shreeshyamstore`.
- Money uses `Long` paise.
- Quantity uses `Long` base units: pieces, grams, ml.
- Product rate model is `pricePerUnitPaise` plus `priceUnitBaseQty`.
- Rounding policy must be documented for quantity-to-amount and amount-to-quantity.
- Use clean `Rs.` or verified UTF-8 rupee symbol only. Do not introduce corrupted currency text.

## acceptance_criteria

- V2 product, sale, sale item, stock adjustment, customer, category, and udhaar target fields are documented.
- Sync-safe fields include `localUuid`, `remoteId`, `shopId`, `syncStatus`, `deletedAt`, `lastSyncedAt`, `createdByUid`, `updatedByUid`, and `sourceDeviceId`.
- Bill idempotency fields include `deviceId`, `billSequence`, `idempotencyKey`, `createdByUid`, and `createdAt`.
- Product/category/customer soft delete is documented.
- Sale status foundation is documented without implementing returns/refunds UI.
- SaleItem purchase price snapshot is documented.
- Firebase config owner decisions are listed.
- FR worker packets exist and are ordered.

## required_evidence

- Files changed and purpose.
- Docs-only validation grep for app identity, v2 fields, and mojibake/currency corruption.
- `git status --short --untracked-files=all`.
- Confirmation no app source, Gradle, Firebase config, Room schema implementation, UI, or strings were changed in FR-A.

## review_owner

Delivery manager plus data/security reviewer.

## do_not_touch

- `app/src/**`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `settings.gradle.kts`
- `google-services.json`
- `.env`, signing files, service-account keys, production credentials
- Firebase console/project settings
- Product UI, Billing UI, Room implementation, package rename, Firebase implementation, welcome sound/assets.
