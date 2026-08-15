# Shree Shyam Store - Master Engineering Roadmap & GitHub Workflow

## 1. GitHub Collaboration & Workflow Governance

### A. Branching Strategy
- `main` : Production-ready, stable, and tested code.
- `feat/<issue-id>-<feature-slug>` : Feature branches (e.g., `feat/01-room-atomic-transactions`).
- `fix/<issue-id>-<bug-slug>` : Bug fix branches (e.g., `fix/02-udhaar-dao-aggregation`).

### B. Pull Request (PR) Checklist & Definition of Done
Every PR must fulfill:
1. **Compilation & Lint**: Zero build errors (`gradle assembleDebug`).
2. **Local Tests**: All unit tests pass (`gradle testDebugUnitTest`).
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
