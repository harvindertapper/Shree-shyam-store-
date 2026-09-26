# Shree Shyam Store — Verified Ground-Level Audit

**Audit date:** 21 September 2026  
**Reviewed baseline:** `main` at `b00ec95a13189d6e7307f6509385a7f92350cb4a`  
**Product:** Android Merchant OS for small Indian retailers  
**Verdict:** Useful hardened staging candidate; not yet ready for public launch

## How this was verified

This audit reviewed the source and repository controls directly, including:

- `MainActivity`, startup routing, auth/onboarding screens, Home, Billing, Products, Udhaar, Reports, and Settings.
- `ShopViewModel`, `InventoryViewModel`, billing cart/checkout boundaries, and repository/DAO code.
- Room entities, migrations, schema version 11, sync outbox, Firebase sync, WorkManager, backup/restore, identity, and security utilities.
- Gradle release configuration, CI workflow, tests, branches, open issues, pull requests, tags, and releases.

A feature is marked **verified** only when the code is present and there is relevant test or CI evidence. A code path that depends on Firebase rules, Play Console, signing secrets, real devices, or an external backend is marked **not proven** until that external evidence exists.

## 1. What is genuinely complete in code

| Area | Status | Evidence | Confidence |
| --- | --- | --- | --- |
| Offline-first Room data model | Done in code | `AppDatabase.kt`, `Entities.kt`, `ShopRepository.kt` | High |
| Integer-paise money representation | Done in code | `Sale`, `SaleItem`, `Product`, `Customer`; commerce validation tests | High |
| Atomic checkout transaction | Done in code | `SaleDao.completeBillCheckout`; checkout invariant tests | High |
| Bill-number uniqueness | Done in code | Unique Room index plus `OnConflictStrategy.ABORT` | High |
| Stock-underflow rejection | Done in code | `deductProductStockIfAvailable` plus transaction checks | High |
| Udhaar credit-limit and ledger checks | Done in code | `SaleDao`, `LedgerAuditPolicy`, Udhaar audit tests | High |
| Stable record IDs and mutation metadata | Done in code | `globalId`, `mutationVersion`, `mutationDeviceId`, sync identity tests | High |
| Sync outbox states | Done in code | Pending, in-flight, retryable, acknowledged, dead-letter states | High |
| Tenant/actor authorization boundary | Done in code | `TenantAuthorization`, `OperatorActionPolicy`, authorization tests | High |
| Restore validation and rollback boundary | Done in code | `SnapshotEnvelope`, `RestoreSnapshotValidator`, `RestoreRecoveryCoordinator` | High |
| HTTPS/trusted-host/authenticated backup boundary | Done in code | `AuthenticatedRestBackupProvider`, backup provider tests | High |
| Room migration chain through schema 11 | Done in code | `AppDatabase` migrations and migration tests | High |
| Hindi/English string contracts | Done in code | Locale contract and bilingual semantics tests | High |
| Reports interval and eligibility policy | Done in code | `ReportPolicyTest` and report semantics tests | High |
| Critical journey test seams | Done in code | Journey contract, semantics, and device smoke tests | Medium–High |
| Debug/minified release build configuration | Done in code | `app/build.gradle.kts`, CI release assembly step | Medium |
| R8/resource shrinking enabled for release | Done in code | `release.isMinifyEnabled = true`, `isShrinkResources = true` | High |

## 2. What is not proven or is still incomplete

| Area | Ground-level finding | Status |
| --- | --- | --- |
| Startup flow | `ShopViewModel` starts on `Screen.Welcome`; `MainActivity` performs identity reconciliation in a `LaunchedEffect`. The Welcome/auth surface can therefore appear before routing finishes. A startup-gate fix is in draft PR #77, but it is not merged or CI-approved yet. | P0/P1 pending |
| Onboarding | First launch asks for shop details, owner details, phone, PIN, and biometric preference in one long screen. It is functional but not a guided merchant journey. There is no evidence of usability testing with shopkeepers. | P1 pending |
| Primary navigation | The bottom bar exposes six destinations: Home, Billing, Products, Udhaar, Reports, and Settings. This is functional but dense for a first-time merchant and lacks a clear primary “Sell” action hierarchy. | P1 pending |
| Sync truthfulness | Existing Home UI can show a green cloud/backup status based on `lastSyncTime`, while scheduling WorkManager is not itself proof of remote success. `SyncManager` callback lifecycle was fixed in draft PR #75, but the merchant-facing attempt/success/retry/conflict model is not complete. | P0 pending |
| Backup vs sync | Settings and sync flows still need separate last-attempt, last-success, pending, retry, conflict, and failure states for backup and synchronization. | P0 pending |
| Local credential sunset | PBKDF2-HMAC-SHA256 v2 with salt is present, but legacy SHA-256, legacy four-digit PIN, and the default `1234` migration fallback remain intentionally supported. A controlled compatibility sunset is not complete. | P0 pending |
| Firebase authorization | Client-side identity and tenant checks exist, but Firebase rules and the production server boundary were not verified from this repository. | P0 external evidence missing |
| REST backup | HTTPS, host allowlisting, bearer token, tenant headers, and snapshot validation exist. Actual Firebase rules, token issuance, expiry behavior, and server-side authorization were not verified. | P0 external evidence missing |
| Multi-device conflict operations | Conflict, retry, and dead-letter data structures exist, but there is no finished merchant/operator workflow for resolving them safely. | P0/P1 pending |
| Crash monitoring | No proven production crash-reporting backend, alert policy, or release health dashboard is committed. | P0 missing |
| Support operation | No finished merchant support route, incident owner, response policy, or rollback owner is proven. | P0 missing |
| Release identity | Current package ID remains `com.aistudio.shreeshyamstore.pqwzkb`, which is still a prototype-style identity until 7Zen Labs approves the final package. | P0 decision pending |
| Signed production artifact | No tag or GitHub release exists. A protected signed-candidate workflow is in draft PR #76, but its secrets are not configured and it has not produced a signed AAB. | P0 pending |
| Play rollout | No Play Console internal/closed-testing evidence was available in GitHub. | P0 external evidence missing |
| Public product assets | Root README, privacy policy, terms, support policy, screenshots, feature graphic, and final product story were not complete on the reviewed `main`. | P1/P2 pending |
| Activation and feedback | Privacy-safe activation milestones and merchant feedback route are not implemented as a reviewed production system. | P2 pending |

## 3. Old-code review findings that still need correction

### P0 — Startup routing and first impression

**Problem:** the first rendered state is an auth/welcome surface while session reconciliation is asynchronous. This causes the “window opens immediately” flash and makes the app feel broken or slow.

**Required fix:** keep a minimal branded startup frame until identity reconciliation completes, then route exactly once to:

- Welcome when there is no valid session.
- Setup when a valid session has not completed shop setup.
- App lock when setup is complete and lock is enabled.
- Home when the session is valid and no lock is required.

**Acceptance:** cold start, process restart, logged-out state, local session, Firebase session, expired Firebase session, and first-launch state each render only the correct destination. No Welcome flash is visible in screenshot/device tests.

### P0 — Truthful cloud status

**Problem:** “scheduled” is not “uploaded successfully”. A merchant must not believe a cloud backup exists merely because WorkManager accepted a job.

**Required states:** configured, signed-in, offline, queued, running, last attempt, last successful sync, last successful backup, retryable failure, permanent failure, conflict, dead-letter, and next action.

**Acceptance:** status tests cover stale values, process restart, offline/online transitions, worker retry, remote failure, and successful completion.

### P0 — Credential compatibility sunset

**Problem:** the new verifier is materially stronger, but compatibility paths still allow legacy credential formats and default-PIN migration behavior.

**Required fix:** document a migration deadline, require explicit PIN setup, remove the default fallback after migration, and add a recovery path that does not weaken the device boundary.

**Acceptance:** no new credential is stored as SHA-256/plaintext; no cloud/outbox/backup payload includes credential material; legacy migration tests remain green until the sunset version.

### P0 — Server-side authorization evidence

**Problem:** the client adds tenant and actor checks, but the real security boundary depends on Firebase rules or a controlled service that was not part of the reviewed repository.

**Required fix:** verify production Firebase rules or build the private Control Plane boundary. Test wrong tenant, wrong membership, expired token, replay, stale command, and unauthorized device cases against the real backend.

### P1 — Restore and sync operations

**Problem:** data structures are strong, but merchant-visible recovery operations are not yet a complete product flow.

**Required fix:** provide a redacted health panel with retry, requeue/dead-letter handling, conflict explanation, last successful backup, recovery-point age, and safe next action. Never expose payloads or credentials.

### P1 — UI architecture and user flow

**Problem:** `ShopViewModel` is still a large orchestration surface and several screens mix data loading, validation, navigation, and side effects.

**Required fix:** continue extracting use cases/controllers around checkout, onboarding, sync/backup, restore, and app lock. Keep Room and authorization boundaries intact while reducing UI coupling.

## 4. UI redesign plan

### New first-run flow

1. Branded startup frame while identity is reconciled.
2. One decision screen: Google account or local/offline account.
3. Three short setup steps: shop identity, first product, security/backup choice.
4. Finish on a guided “Create your first bill” action, not a blank dashboard.
5. Show a progress indicator and allow safe back navigation without losing entered values.

### New merchant shell

- Home becomes a command center with one primary “New bill” action.
- Billing becomes the default operational destination.
- Products, Udhaar, Reports, and Settings move under a simpler navigation hierarchy.
- Sync/backup health becomes an honest, tappable status card rather than a green decorative badge.
- Empty states explain the next action: add product, create bill, add customer, or configure backup.
- Keep 48dp touch targets, Hindi/English parity, ₹ formatting, and low-end device performance.

### Screen-by-screen polish order

1. Startup and onboarding.
2. Billing/cart and payment confirmation.
3. Home dashboard and sync health.
4. Product/catalog and stock adjustment.
5. Udhaar customer flow and payment correction.
6. Reports/export.
7. Settings, backup, restore, app lock, and support.

## 5. Missing features required for a real pilot

- Truthful sync and backup health with recovery actions.
- Merchant support/contact route inside Settings.
- Crash monitoring and release health alerts.
- Privacy policy, terms, data retention statement, and support policy.
- Final package ID, Firebase mapping, release signing, version policy, and rollback ownership.
- Real-device rehearsal on clean install, upgrade, offline sale, interrupted sync, backup, restore, and rollback.
- Pilot checklist and feedback capture.
- Privacy-safe activation events with opt-out/retention rules.
- Conflict/dead-letter operator workflow.
- Final Play listing and staged rollout evidence.

## 6. Current go/no-go list

### Go for controlled pilot only after evidence

- Core offline commerce and local data integrity.
- Integer-paise money and checkout invariants.
- Basic authorization, migration, backup, and restore contracts.
- Hindi/English and critical journey test coverage.

### No-go for public launch until complete

- Signed reproducible AAB tied to an immutable tag.
- Final package/Firebase identity.
- Truthful sync/backup UI.
- Crash monitoring and support operation.
- Real-device recovery rehearsal.
- Production backend authorization evidence.
- Privacy/legal/store assets.

## 7. Verified implementation queue

| Order | Slice | Priority | Output |
| --- | --- | --- | --- |
| 1 | Merge and verify startup gate | P0 | No auth-screen flash; cold-start tests |
| 2 | Build truthful sync/backup health model | P0 | State model, UI card, retry/recovery tests |
| 3 | Finish credential sunset plan | P0 | Migration deadline and negative tests |
| 4 | Verify backend rules/service boundary | P0 | Real tenant/auth/replay evidence |
| 5 | Redesign onboarding and billing flow | P1 | Merchant-tested guided journey |
| 6 | Add crash/support operation | P0 | Alerts, owner, support route, rollback runbook |
| 7 | Configure signed release workflow | P0 | Signed AAB, mapping, provenance, checksum |
| 8 | Run 5–10 merchant pilot | P0 | Task completion, failures, support, feedback |
| 9 | Fix pilot P0/P1 issues | P0 | Updated release candidate |
| 10 | Staged Play rollout | P0 | Internal → closed → small production percentage |
