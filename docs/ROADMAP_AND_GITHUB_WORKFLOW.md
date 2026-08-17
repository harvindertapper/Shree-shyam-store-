# Shree Shyam Store - Master Engineering Roadmap & GitHub Workflow

## Current takeover status (17 August 2026)

The repository baseline is recorded in `docs/BASELINE_AUDIT.md`. The first stabilization branch, `fix/security-sync-boundary`, removes local user-account data from Firestore and REST backup payloads, preserves device-owned users and shop profile data during business-data restore, makes cloud restore replacement atomic through Room, adds the Gradle wrapper, expands the stable CI test selection, and adds local development/release documentation. The branch is under pull request review; `main` remains unchanged.

The next priorities are to choose one identity model, remove or migrate weak local credential compatibility paths, replace destructive Room fallback with tested migrations, define stable multi-device IDs and conflict policy, decide the inventory underflow policy, and add domain/integration coverage for billing, inventory, ledger, sync, restore, and authorization. Production release work must wait for the release and recovery gates in `docs/RELEASE_RUNBOOK.md`.

## 1. GitHub Collaboration & Workflow Governance

### A. Branching Strategy
- `main` : Production-ready, stable, and tested code.
- `feat/<issue-id>-<feature-slug>` : Feature branches (e.g., `feat/01-room-atomic-transactions`).
- `fix/<issue-id>-<bug-slug>` : Bug fix branches (e.g., `fix/02-udhaar-dao-aggregation`).

### B. Pull Request (PR) Checklist & Definition of Done
Every PR must fulfill:
1. **Compilation & Lint**: Zero build errors (`./gradlew assembleDebug` and `./gradlew lintDebug`).
2. **Local Tests**: All stable unit/Robolectric tests pass (`./gradlew testDebugUnitTest`).
3. **Data Safety**: No destructive database schema changes without migration.
4. **Offline Resilience**: App must work seamlessly offline without network crashes.
5. **Shopkeeper UX**: Large touch targets (>= 48dp), bilingual Hindi/English clarity, ₹ currency formatting.

---

## 2. Structured Task Breakdown & Issue Plan

```
┌────────────────────────────────────────────────────────────────────────┐
│                        MILESTONE BREAKDOWN                             │
├────────────────────────────────────────────────────────────────────────┤
│ M1: Core Integrity & Atomic Transactions   (Billing & Udhaar Fixes)    │
│ M2: Resilient Cloud Auto-Sync Engine       (WorkManager + Auto-Retry)  │
│ M3: Kirana Weighted & Loose Items          (Kg, Gram, Litre, Piece)    │
│ M4: Backup, Multi-Device & Final Polish    (Export, Reports, Status)   │
└────────────────────────────────────────────────────────────────────────┘
```

### Milestone 1 (M1): Core Integrity & Performance Fixes [COMPLETED ✅]
* [x] **Issue #1**: `[INTEGRITY] Atomic Room @Transaction for Billing & Stock Reduction`
  - Wrapped `insertSaleWithItems()` inside Room `database.withTransaction` to prevent partial writes.
* [x] **Issue #2**: `[PERFORMANCE] Direct SQL Aggregation for Udhaar Customer Balances`
  - Replaced in-memory Kotlin iteration with fast Room DAO SQL `SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END)`.
* [x] **Issue #3**: `[HYGIENE] Compose Material 3 & AutoMirrored Deprecation Cleanup`
  - Updated deprecated Icons (`Icons.AutoMirrored.Filled.*`).

---

### Milestone 2 (M2): Resilient Online Cloud Auto-Sync & Retry Engine [COMPLETED ✅]
* [x] **Issue #5**: `[SYNC] Android Jetpack WorkManager Auto-Sync Engine`
  - Implemented `SyncWorker` with `NetworkType.CONNECTED` constraints and `BackoffPolicy.EXPONENTIAL`.
* [x] **Issue #6**: `[SYNC] Event-Driven Realtime Trigger`
  - Automatically enqueued `SyncWorker` on every new bill, product change, stock adjustment, or udhaar payment.
* [x] **Issue #7**: `[UI] Shopkeeper Cloud Status Indicator`
  - Added live status pill on Home screen header with instant one-tap sync trigger.

---

### Milestone 3 (M3): Kirana Store Features (Weighted & Loose Goods)
* **Issue #8**: `[INVENTORY] Unit Support & Fractional Quantities`
  - Support `Kg`, `Gm`, `Ltr`, `Ml`, `Pcs`, `Pkt` with fractional quantities (e.g., 0.250 kg).
* **Issue #9**: `[BILLING] Quick Rate Override & Custom Weighing Input`
  - Allow entering direct amount (e.g., ₹20 ki cheeni) with automatic quantity calculation.

---

### Milestone 4 (M4): Data Safety, Backup & Reports
* **Issue #10**: `[SAFETY] Offline JSON Export & Restore`
  - Allow shopkeeper to take local phone backups on SD card / internal storage.
* **Issue #11**: `[ANALYTICS] Owner Desk Daily Profit & Stock Health Dashboard`
  - Fast margin tracking, fast-moving items, and collection reminders.
