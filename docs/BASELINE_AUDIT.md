# Shree Shyam Store — Baseline Audit

**Audit date:** 17 August 2026  
**Audited revision:** `831bbffde713a6902fc4426608fc1a02d8d7d99e` on `main`  
**Repository:** [`harvindertapper/Shree-shyam-store-`](https://github.com/harvindertapper/Shree-shyam-store-)

## Executive assessment

The repository is an **Android Kotlin/Jetpack Compose inventory and shop-ledger application** for a general store. It already contains a meaningful offline-first prototype: Room persistence, Compose screens, barcode scanning, WorkManager scheduling, Firestore synchronization, manual Firebase REST backup/restore, local authentication, app-lock PIN handling, billing, udhaar ledger, reports, and a GitHub Actions build workflow.

The codebase is a promising functional prototype, but it is **not yet production-enterprise ready**. The most urgent risks are security and data integrity rather than visual polish. In particular, the app mixes Firebase sign-in with a separate local username/password system, synchronizes local `passwordHash` values to cloud storage, retains compatibility paths for plaintext/default PINs, performs a destructive restore in two separate database transactions, permits potentially negative stock during checkout, and uses `Double` for money. The release configuration also still uses placeholder package identifiers, disables minification, lacks a Gradle wrapper, and has no protected default branch or release process.

The recommended modernization strategy is **incremental hardening**, beginning with security and data-safety boundaries before adding more commerce features. A rewrite is not justified by the current evidence; the existing Room/Compose foundation can be evolved if schema, domain, authentication, synchronization, and release boundaries are made explicit.

## Repository and workflow baseline

| Area | Observed state | Assessment |
| --- | --- | --- |
| Remote and default branch | Public repository with `main` as the default branch | GitHub can remain the source of truth, but public visibility and branch protection require an explicit security decision |
| Branches | `main` and `fix/26-27-sync-compatibility`; `main` is not protected | Feature-branch work exists, but merges are not currently guarded by required checks |
| Recent work | Recent changes address data sync compatibility, auth/reports/checkout hardening, Android CI, credit limits/UPI, barcode scanning, Firebase sync, and stock/reorder flows | The project is actively evolving; audit and changes must avoid overwriting recent behavior |
| Pull requests and releases | Two merged pull requests; no tagged releases found | Release provenance and rollback are not yet formalized |
| CI | One Android workflow at `.github/workflows/ci.yml` | Build/lint/unit checks exist, but security, dependency, instrumentation, release, and environment gates are incomplete |
| Local build entrypoint | No `gradlew` or `gradlew.bat` and no Gradle wrapper directory found | Local reproduction depends on an externally installed Gradle version |
| Working tree | Clean at the audited `main` revision before this audit document | The baseline code remains recoverable at the recorded commit |

## Technology inventory

The application is built with Kotlin and Jetpack Compose. The module uses Android Gradle Plugin 9.1.1, Kotlin 2.4.0, Compose Material 3, Room 2.7.0, WorkManager 2.10.0, Firebase Auth/Firestore, Android Credentials and Google ID, CameraX and ML Kit barcode scanning, DataStore Preferences, Moshi, OkHttp, Retrofit, Robolectric, and Roborazzi. The compile and target SDK are 36, while the minimum SDK is 24. The CI workflow runs Java 21 and installs Gradle 9.7.0, whereas the project compiles application code for Java/Kotlin 11.

The app namespace is `com.example` and the application ID is `com.aistudio.shreeshyamstore.pqwzkb`. These values are prototype/AI-Studio identifiers rather than a stable production identity. The release build has `isMinifyEnabled = false`, uses environment-provided signing credentials, and relies on a release keystore path that is not documented as part of a release runbook.

| Component | Current location | Production implication |
| --- | --- | --- |
| UI/navigation | `app/src/main/java/com/example/MainActivity.kt` and `ui/screens/*` | Navigation and screen orchestration are centralized; deeper feature boundaries should be introduced gradually |
| View-model orchestration | `viewmodel/ShopViewModel.kt` | The ViewModel owns authentication, navigation, billing, sync, backup, restore, and much of the business logic; this is the largest maintainability and testability hotspot |
| Persistence | `data/AppDatabase.kt`, `data/Daos.kt`, `data/Entities.kt`, `data/ShopRepository.kt` | Room is a solid offline foundation, but schema evolution, constraints, money representation, and conflict identity need hardening |
| Preferences/session | `data/SettingsDataStore.kt` | Local session and settings are useful, but credential/session trust boundaries must be documented and unified |
| Cloud sync | `utils/FirebaseSyncService.kt`, `SyncManager.kt`, `SyncWorker.kt` | There are two cloud paths—Firestore background sync and REST JSON backup/restore—with different semantics and security controls |
| Authentication | `utils/AuthManager.kt` plus local auth in `ShopViewModel.kt` | Firebase Google sign-in and local password auth are separate identity systems; this creates account and authorization ambiguity |
| Testing | `app/src/test/*`, `app/src/androidTest/*`, `.github/workflows/ci.yml` | Tests are mostly scaffolding plus security helper coverage; critical domain, database, sync, auth, and UI journeys are not yet protected |

## Data model and persistence findings

The Room schema contains shop profiles, categories, products, sales, sale items, customers, udhaar transactions, stock adjustments, and users. Most records use auto-generated local `Long` identifiers, soft-delete flags, sync flags, and millisecond timestamps. Several business concepts are represented as free-form strings, including payment mode, transaction type, and product unit. Monetary values and quantities are represented as `Double`.

The checkout transaction in `SaleDao.completeBillCheckout()` is a useful foundation: it validates a non-empty cart, normalizes payment mode, verifies customer presence for udhaar, inserts the sale and items, deducts tracked stock, writes stock adjustment history, and creates an udhaar credit record. However, the transaction does not reject stock underflow, does not independently verify each supplied `lineTotal` against `unitPrice × quantity`, and does not establish database-level foreign keys, unique business identifiers, or indexes for the most important lookup paths. These rules should move toward typed domain values and explicit database constraints.

`AppDatabase` is version 3 and provides a 2→3 migration that creates `shop_profiles`. It also enables `fallbackToDestructiveMigration()`. This is acceptable only as a temporary development recovery mechanism; on an unexpected schema version or missing migration, it can erase local shop data. `exportSchema = false` also prevents migration history from being reviewed and tested as an artifact. There is no evidence in the current source of a complete migration chain for all historical versions.

## Critical security and privacy findings

### Cloud synchronization includes password hashes

`FirebaseSyncService` collects local `User` records and serializes `passwordHash` into both Firestore and the manual JSON backup path. A password hash is still credential material. It should not be replicated as part of ordinary shop data, especially when the application also supports Firebase authentication. This is a **critical finding** because a cloud rules mistake, backup exposure, or account compromise could turn a data-sync feature into an authentication compromise.

**Immediate treatment:** stop syncing `User.passwordHash`; decide whether local accounts will be removed in favor of a single Firebase identity model or retained only as a local-device credential that never leaves the device. Existing cloud records should be treated as sensitive and reviewed before any cleanup or migration.

### Local passwords use a fast unsalted SHA-256 digest

Local registration stores `sha256(password)` in `ShopViewModel`. SHA-256 is not a password-specific slow KDF and is not salted per account. This leaves offline password cracking substantially easier than a password hashing scheme designed for credentials. The app-lock PIN helper has a similar compatibility concern: it accepts legacy four-digit plaintext values and a blank value mapped to the historical default PIN `1234`.

**Treatment:** select one identity model. If local credentials must remain, use a platform-appropriate salted password KDF and a migration path that never exports credentials. For the app lock, require explicit setup, remove the default fallback after migration, and store only a verifier suitable for the threat model. Add negative and migration tests before changing existing behavior.

### REST backup appears to rely on a user-configurable database URL and prefix

The manual backup/restore path accepts a configurable Firebase URL and prefix, performs unauthenticated HTTP GET/PUT requests, and checks only HTTP success. The isolation boundary is a hashed user identifier embedded in the path. This is not an authorization system. Data safety therefore depends heavily on external Firebase database rules that are not part of this repository.

**Treatment:** make the backend authorization model explicit, prefer authenticated Firebase SDK operations or a controlled server boundary, validate allowed hosts/configuration, and document rules and least-privilege access. A backup operation should produce a verifiable snapshot with integrity metadata rather than overwrite arbitrary remote JSON paths.

## Critical data-integrity and recovery findings

### Restore can destroy local data before a later insert failure

`ShopViewModel.restoreAllFromCloud()` calls `repository.clearAllLocalTables()` and only afterward calls `repository.insertRestoredData()`. Each repository method is internally transactional, but they are two separate transactions. If restore insertion fails after the clear commits, the device is left empty or partially recoverable. The UI also treats an all-empty download as “no data,” but a legitimate empty store and a failed/misconfigured response are not strongly distinguished by protocol metadata.

**Treatment:** download and validate a complete snapshot first; write the snapshot through one database transaction or staged tables with an atomic swap; verify counts, schema version, checksum, and referential consistency; preserve a local pre-restore backup; provide an explicit preview and recovery path.

### Multi-device identity and conflict semantics are under-specified

Firestore documents use auto-generated local numeric IDs as document IDs. Two devices can independently create record `id = 1`, and merge semantics can overwrite one record with another. The pull path inserts cloud records into Room with `REPLACE`, while conflict resolution is effectively last-writer-wins without a documented clock, device identity, tombstone policy, or user-visible conflict handling.

**Treatment:** introduce globally unique stable record IDs, device/source metadata, monotonic or server-backed version fields, explicit tombstones, idempotency keys, and a per-entity conflict policy. Migrate existing local IDs carefully rather than changing primary keys in place without a data plan.

### Checkout can permit negative stock

The checkout transaction computes `newStock = oldStock - quantity` and updates stock without a guard. If negative inventory is not an intentional business rule, concurrent or repeated sales can produce invalid stock. The transaction also creates an adjustment record from the pre-update value but does not verify the database update affected the expected row/version.

**Treatment:** define the inventory policy (allow backorders or reject underflow), enforce it in the database transaction, add concurrency tests, and expose the policy in the product/admin UX.

## Maintainability and production-release findings

The main `ShopViewModel` is a large orchestration class that crosses UI state, authentication, persistence, billing, ledger behavior, sync, export, and restore. This makes critical flows difficult to test in isolation and increases the chance that a UI change changes data semantics. The first architecture increment should extract use cases and domain services around checkout, inventory adjustment, customer ledger, authentication/session, and sync/restore.

The application currently uses `Double` for money and free-form strings for payment modes and units. Monetary calculations should move to integer minor units or a decimal type with a single rounding policy. Payment mode, ledger transaction type, product unit, and order state should become enums/value objects at the domain boundary with strict serialization compatibility.

The test suite includes example arithmetic and Robolectric launch checks, a screenshot test that is intentionally excluded from CI, and `SecurityUtilsTest`. There are no evident tests for checkout atomicity, stock invariants, totals, restore failure recovery, sync conflict behavior, authentication authorization, cloud serialization privacy, or critical Compose journeys. CI currently assembles debug, runs lint, and runs a selected set of unit/Robolectric tests. It does not run a production release build, dependency audit, secret scan, migration test, instrumentation test, or end-to-end smoke path.

## Prioritized risk register

| ID | Severity | Risk | Evidence | First treatment |
| --- | --- | --- | --- | --- |
| R-01 | Critical | Password hashes leave the device through cloud sync and backup | `FirebaseSyncService.kt:46-65`, `:275-293`, `:339-343` | Remove credential fields from cloud payloads and define one identity model |
| R-02 | Critical | Restore clears local data before a separate restore transaction | `ShopViewModel.kt:1033-1045`, `ShopRepository.kt:187-223` | Stage/validate snapshot and atomically replace or roll back |
| R-03 | High | Local passwords use unsalted fast SHA-256 | `ShopViewModel.kt:247-251`, `:294-295` | Migrate to a password-specific KDF or remove local passwords |
| R-04 | High | Default/legacy PIN verification permits weak compatibility paths | `SecurityUtils.kt:46-56` | Require explicit migration and remove default fallback |
| R-05 | High | Configurable unauthenticated REST backup boundary | `FirebaseSyncService.kt:375-417`, `ShopViewModel.kt:888-908` | Enforce authenticated, allow-listed, integrity-checked backup protocol |
| R-06 | High | Room destructive fallback can erase local data | `AppDatabase.kt:72-77` | Remove from production path and add complete migration/schema testing |
| R-07 | High | Numeric local IDs create cross-device collision/overwrite risk | `Entities.kt:20-154`, `FirebaseSyncService.kt:55-64`, `:121-293` | Introduce stable global IDs and conflict/version policy |
| R-08 | High | Checkout can create negative stock and trusts supplied line totals | `Daos.kt:156-245` | Define/enforce inventory and pricing invariants with tests |
| R-09 | Medium | Money uses binary floating point | `Entities.kt:35-47`, `:63-94` | Migrate to minor-unit integers/decimal policy |
| R-10 | Medium | Prototype package identity and release hardening remain | `app/build.gradle.kts:12-75` | Choose production application ID, enable release optimization, document signing |
| R-11 | Medium | No Gradle wrapper, reducing local reproducibility | Repository root | Add and validate wrapper using the project-approved Gradle version |
| R-12 | Medium | CI and repository controls are incomplete | `.github/workflows/ci.yml`, GitHub settings | Add security/release gates and protect `main` after confirming policy |

## Recommended first implementation slice

The first stabilization pull request should be security- and data-safety-focused, not a broad UI rewrite. It should remove `passwordHash` from all cloud payloads and pull mappings, add tests that assert credential fields are not serialized, introduce a safe restore staging/validation boundary or at minimum prevent destructive clear when the downloaded snapshot is incomplete, and document the current identity/sync decision. The same change should add a regression test for checkout stock policy once the business rule is confirmed.

A second stabilization pull request should add the Gradle wrapper, document the supported Java/Gradle/Android SDK versions, preserve an example environment without real credentials, and expand CI with a production configuration check and dependency/secret scanning. Branch protection and release controls should be enabled only after the required CI check names are stable.

## Evidence and references

The audit is grounded in the checked-out repository revision and its GitHub metadata.

[1]: https://github.com/harvindertapper/Shree-shyam-store- "Shree Shyam Store GitHub repository"
[2]: ../app/build.gradle.kts "Android app build configuration"
[3]: ../app/src/main/java/com/example/data/Entities.kt "Room entities"
[4]: ../app/src/main/java/com/example/data/AppDatabase.kt "Room database configuration"
[5]: ../app/src/main/java/com/example/data/Daos.kt "Room DAO queries and checkout transaction"
[6]: ../app/src/main/java/com/example/utils/FirebaseSyncService.kt "Firebase synchronization and REST backup"
[7]: ../app/src/main/java/com/example/viewmodel/ShopViewModel.kt "Application orchestration and restore flow"
[8]: ../app/src/main/java/com/example/utils/SecurityUtils.kt "PIN security helper"
[9]: ../.github/workflows/ci.yml "Android CI workflow"
[10]: ../docs/ROADMAP_AND_GITHUB_WORKFLOW.md "Existing roadmap and workflow document"


## Post-stabilization implementation update — 18 August 2026

The security and data-integrity roadmap has now merged the credential-sync boundary, atomic restore boundary, P0 checkout policies, minor-unit money migration, immutable udhaar audit events, stable sync identity/outbox through PR #18, the explicit Firebase-or-local session authority through PR #19, non-destructive migration policy documentation through PR #20, and the production package identity alignment through PR #21. The app-lock hardening slice adds serialized five-attempt PIN cooldown, strong-biometric capability checks, inactivity timeout enforcement, new-PIN strength validation, and bilingual failure messaging. Slow credential-KDF migration, registration/login rate limiting, domain-level authorization negatives, release signing, and authenticated backup-provider enforcement remain open production work.
