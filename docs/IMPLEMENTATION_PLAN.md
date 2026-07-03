# Implementation Plan

Status: Draft source of truth for module sequencing. Each module should be assigned as a separate task packet unless the user explicitly approves a combined pass.

## Implementation Sequence

Completed checkpoints:

1. `FR-A` V2 data/calculator design — `0bd189b`.
2. `FR-C` Quantity/price calculator utility — `f36d613`.
3. `FR-B` Room v2 reset — `4bb4927`.
4. `FR-P` App identity rename to `com.harrylabs.shreeshyamstore` — `b7d92f2`.

5. Firebase project/config prerequisites are complete for project `shreeshyamstore`.
6. `FR-K` Room UUID primary keys and relationship refactoring is complete.

Current professional delivery sequence:

Detailed route: `docs/superpowers/plans/2026-07-02-professional-delivery-plan.md`.

1. `PD-00` Source-of-truth and checklist sync so future agents do not follow stale Room v2/Firebase-not-implemented text.
2. `PD-01` Stabilization checkpoint and main-safety verification for current FR-G/sync/billing changes.
3. `PD-02` Firebase owner trust gate: final live Google Sign-In, owner/shop/membership restore, account-switch behavior, and no empty-shop false success.
4. `PD-03A/B` Professional sync engine for settings, categories, products, customers, and udhaar with restore gate, pending/error visibility, retry, and idempotency.
5. `PD-04` Product and stock setup for real kiryana use: piece/weight/volume, inline category creation, stock-by-exception, and quick-added product cleanup.
6. `PD-05` Billing counter hardening: no forced customer/invoice, one-tap sale save, loose amount-to-quantity flow, quantity-to-amount flow, per-line rate override, smart product quick-add, and tracked-stock validation.
7. `PD-03C` Sales, sale items, stock adjustments, and invoice metadata sync after billing persistence is stable.
8. `PD-06` Quick udhaar: customer quick credit/payment entry plus WhatsApp/share reminder text without Contacts permission.
9. `PD-07` Private Owner Desk: total stock value, category-wise stock value, profit reports from sale-item cost snapshots, and private access controls.
10. `PD-08` End-of-day "Aaj ka hisaab": cash, UPI, udhaar, total sale, profit where allowed, low stock, top items, pending sync, and shop-close summary.
11. `PD-09` Optional invoice PDF and Android share/WhatsApp workflow using saved sale snapshots.
12. `PD-10` Professional release hardening: App Check/cost controls, Android backup policy, privacy, release signing, localization polish, and final QA gates.


## Shared Constraints

- Preserve existing Room data unless an approved migration is included.
- Do not add `fallbackToDestructiveMigration()` to new production schema work.
- Owner decision on 2026-06-17 approves Firebase Auth/Firestore/cloud sync as mandatory foundation before trusted billing and real inventory entry.
- Owner decision on 2026-06-17 sets final application id, namespace, Firebase app identity, and Play Store identity to `com.harrylabs.shreeshyamstore`.
- No real inventory has been entered yet. The approved early reset/migration path has already been used through Room v4; Room v4 to v5 now uses an intentional migration for the local sync outbox. Future v5 onward changes require intentional migrations or a new explicit owner-approved reset.
- Real inventory entry is blocked until live Firebase owner restore, App Check/cost posture, product unit/stock setup, product/category/settings sync, and inventory restore gates pass.
- Do not add random Firebase implementation without accepted architecture, security rules, privacy, migration, offline/conflict, and QA packets.
- Do not add Bluetooth printer, multi-store implementation, staff role implementation, or real UPI verification without owner approval.
- All user-facing text must go through English and Hindi string resources.
- Billing, stock, udhaar, reports, backup/import/export, and auth changes need focused tests or documented manual proof.
- UI-facing changes should include screenshots or emulator proof when possible.

## Task Packets

DM-004 task packet files live under `docs/governance/task-packets/FR-*.md`. These supersede the older M02F/M05/M06 route until the foundation reset gates are complete.

The M01 and M02F sections below are retained as historical module records. Do not route new work from them when an FR packet or the current sequence above covers the same area.

### M01-FOUNDATION-001

Goal: Stabilize repo hygiene, source-of-truth docs, and localization baseline before feature work.

Scope paths:

- `AGENTS.md`
- `README.md`
- `APP_BUILD_CHECKLIST.md`
- `docs/**`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-hi/strings.xml`
- `app/src/test/**`

Dependencies: none.

Constraints:

- Docs and tests first; avoid feature changes.
- Keep English default.
- Clean Hindi mojibake only on affected strings and update tests to expect valid Hindi.

Acceptance criteria:

- Source-of-truth docs are present and consistent.
- Hindi resources are valid UTF-8 text, not mojibake, for touched screens.
- Build and unit tests pass when code/resource files change.

Required evidence:

- File list.
- `.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon`
- `.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon`

Review owner: delivery manager plus QA.

### M02-NAV-SHELL-001

Goal: Make startup routing and main shell behavior predictable across logged-out, setup-incomplete, and logged-in states.

Scope paths:

- `app/src/main/java/com/harrylabs/shreeshyamstore/MainActivity.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/viewmodel/ShopViewModel.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/WelcomeScreen.kt`
- `app/src/test/**`
- `app/src/androidTest/**`

Dependencies: M01.

Constraints:

- Do not introduce a new navigation framework unless approved.
- Preserve current screen names unless a migration plan is documented.

Acceptance criteria:

- Welcome routes according to session/setup state.
- Bottom navigation is hidden during welcome, auth, register, and setup.
- Back/forward behavior is documented or tested for critical flows.

Required evidence:

- Unit/Robolectric tests for startup routing.
- Emulator launch proof when UI behavior changes.

Review owner: QA.

### M02F-FIREBASE-CLOUD-FOUNDATION-001 — Historical/Superseded

Historical goal: establish Firebase Auth and Firestore as the foundation before billing hardening. Current active route is the professional delivery sequence above.

Scope paths:

- `docs/governance/FIREBASE_CLOUD_SYNC_ARCHITECTURE.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/DATA_MODEL.md`
- `docs/governance/**`
- `APP_BUILD_CHECKLIST.md`
- Firebase config and Android Gradle files only after architecture/config prerequisites are accepted.

Dependencies: M01.

Constraints:

- Architecture, security rules, privacy, migration, offline/conflict, and QA packets first.
- Do not add Firebase SDK/config files until the Firebase project/config prerequisite packet is accepted.
- Do not commit service-account keys, secrets, signing credentials, real customer data, or production exports.
- Staff roles may be modeled for future memberships, but staff-role UI/permissions implementation is deferred.
- UPI remains a manual payment record.

Acceptance criteria:

- The Firebase architecture is accepted; remaining owner inputs are console/config and provider rollout details.
- Firestore collection/data ownership model is documented.
- Security rules and privacy model are reviewed.
- Room-to-cloud migration and local cache strategy are documented.
- Offline and conflict handling policy is documented.
- First implementation slice is approved before any Firebase code changes.

Required evidence:

- Docs changed.
- Decision log entry.
- Architecture decision document.
- Task packets for architecture, config, auth, data model, rules/privacy, migration, offline/conflict, first implementation, and QA.
- Security/governance review notes.

Review owner: delivery manager plus security/governance.

### M03-AUTH-SESSION-001

Goal: Make local owner login/session behavior reliable and prepare a hardening path for credentials.

Scope paths:

- `app/src/main/java/com/harrylabs/shreeshyamstore/viewmodel/ShopViewModel.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/Entities.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/Daos.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/AppDatabase.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/SettingsDataStore.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/AuthScreens.kt`
- `app/src/test/**`

Dependencies: M01, M02.

Constraints:

- Any entity change needs a Room migration plan.
- Do not store plaintext passwords or secrets.
- Do not claim production-grade security until credential hardening is complete.

Acceptance criteria:

- Register/login validation is localized.
- Duplicate user/email cases are deterministic.
- Logout clears session state.
- Security limitation of current SHA-256 storage is tracked.

Required evidence:

- Unit tests for validation/session behavior.
- Security/privacy note if credential behavior changes.

Review owner: security/governance.

### M04-SHOP-SETTINGS-001

Goal: Make first-launch setup and settings reliable, localized, and restart-safe.

Scope paths:

- `app/src/main/java/com/harrylabs/shreeshyamstore/data/SettingsDataStore.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/FirstLaunchSetupScreen.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/SettingsScreen.kt`
- `app/src/main/res/values*/strings.xml`
- `app/src/test/**`

Dependencies: M01, M02.

Constraints:

- QR image selection remains local user-selected content.
- Language changes must not mix English and Hindi strings.

Acceptance criteria:

- Shop name and owner phone persist.
- Selected language persists and applies on app restart.
- Static QR URI behavior is documented and tested enough for current Android targets.

Required evidence:

- DataStore tests.
- UI/manual proof for language switch.

Review owner: QA plus delivery manager.

### M05-PRODUCT-STOCK-001

Goal: Make products, categories, opening stock, and stock adjustments auditable and safe.

Scope paths:

- `app/src/main/java/com/harrylabs/shreeshyamstore/data/**`
- `app/src/main/java/com/harrylabs/shreeshyamstore/viewmodel/ShopViewModel.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/ProductsAndStockScreen.kt`
- `docs/DATA_MODEL.md`
- `app/src/test/**`

Dependencies: M01.

Constraints:

- Preserve data with migrations for schema changes.
- Do not silently delete products or stock history.

Acceptance criteria:

- Product add/edit validates names and prices.
- Category add/rename is deterministic.
- Opening stock creates adjustment history.
- Manual correction records old/new/difference/reason.
- Low-stock indicators are accurate.

Required evidence:

- Repository/ViewModel tests for stock adjustments.
- Manual screen proof if UI changes.

Review owner: QA plus delivery manager.

### M06-BILLING-INVOICE-001

Goal: Make billing production-grade for cart behavior, stock validation, sale persistence, and invoice data correctness.

Scope paths:

- `app/src/main/java/com/harrylabs/shreeshyamstore/viewmodel/ShopViewModel.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/ShopRepository.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/BillingAndPaymentScreen.kt`
- `docs/DATA_MODEL.md`
- `app/src/test/**`

Dependencies: M01, M02F-FIREBASE-CLOUD-FOUNDATION-001, M05.

Constraints:

- Prevent negative tracked stock unless owner explicitly approves overselling.
- UPI remains a manual payment record.
- Sale and sale-item snapshots must remain auditable.

Acceptance criteria:

- Cart quantity controls cannot create invalid quantities.
- Tracked stock is validated before sale completion.
- Sale, sale items, stock adjustments, and udhaar transactions are created atomically enough for local use.
- Invoice text/data matches saved sale records.

Required evidence:

- Unit tests for cart totals, stock validation, sale persistence, and udhaar sale creation.
- Manual bill creation proof for cash, UPI, and udhaar.

Review owner: delivery manager plus QA.

### M07-UDHAAR-001

Goal: Make customer credit ledger accurate and understandable.

Scope paths:

- `app/src/main/java/com/harrylabs/shreeshyamstore/data/**`
- `app/src/main/java/com/harrylabs/shreeshyamstore/viewmodel/ShopViewModel.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/UdhaarScreen.kt`
- `app/src/test/**`

Dependencies: M06.

Constraints:

- Customer phone numbers are sensitive business data.
- Payments must reduce balance without deleting original credit history.

Acceptance criteria:

- Udhaar sale creates a credit transaction.
- Payment entry creates a payment transaction.
- Customer balance calculation is correct.
- Debtor filters and transaction history match stored data.

Required evidence:

- Balance-calculation tests.
- Manual proof for customer detail/payment flow if UI changes.

Review owner: QA.

### M08-REPORTS-001

Goal: Make daily/monthly reports reliable for shop decisions.

Scope paths:

- `app/src/main/java/com/harrylabs/shreeshyamstore/data/**`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/ReportsScreen.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/utils/**`
- `app/src/test/**`

Dependencies: M06, M07.

Constraints:

- Do not add profit reporting until purchase price and purchase workflow expectations are owner-approved.

Acceptance criteria:

- Daily totals, monthly totals, payment-mode breakdown, invoice count, and recent bills are correct.
- Date boundaries are explicit and tested.

Required evidence:

- Tests for report calculations.
- Manual proof with seeded demo data if UI changes.

Review owner: QA.

### M09-INVOICE-SHARE-001

Goal: Add local PDF invoice generation and Android share/WhatsApp share after billing data is correct.

Scope paths:

- `app/src/main/java/com/harrylabs/shreeshyamstore/**`
- `app/src/main/res/values*/strings.xml`
- `app/src/test/**`
- `app/src/androidTest/**`

Dependencies: M06.

Constraints:

- Do not add Bluetooth printer work.
- Handle missing WhatsApp gracefully.
- Do not expose invoices without explicit user share action.

Acceptance criteria:

- PDF includes shop name, phone, bill number, date, items, payment mode, and total.
- Share sheet works with generated file URI.
- Direct WhatsApp intent is optional and failure-safe.
- English and Hindi invoice labels are available.

Required evidence:

- Invoice formatting tests.
- Manual Android share proof.
- Screenshot or generated PDF evidence.

Review owner: delivery manager plus QA.

### M10-BACKUP-IMPORT-001

Goal: Add safe local backup/export/import for business records.

Scope paths:

- `app/src/main/java/com/harrylabs/shreeshyamstore/data/**`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/SettingsScreen.kt`
- `app/src/main/res/xml/**`
- `app/src/test/**`

Dependencies: M06, M07, M08.

Constraints:

- Owner must approve file format and restore conflict behavior.
- No silent cloud upload.
- Export/import must warn about sensitive customer and sales data.

Acceptance criteria:

- Export captures required business tables.
- Import validates file shape before writing.
- Restore behavior is deterministic and documented.

Required evidence:

- Round-trip tests with sample data.
- Security/privacy review.

Review owner: security/governance plus QA.

### M12-RELEASE-HARDENING-001

Goal: Prepare a trustworthy release build with branding, package decisions, signing, backup policy, and versioning.

Scope paths:

- `app/build.gradle.kts`
- `settings.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/**`
- `README.md`
- `docs/**`

Dependencies: core MVP modules accepted.

Constraints:

- Do not commit signing passwords or keystores.
- Package rename requires explicit owner decision.
- Backup policy must be reviewed before release.

Acceptance criteria:

- App name, icon, package/application id, version code/name, and signing workflow are documented.
- Release build process is repeatable without secrets in repo.

Required evidence:

- Debug and release build notes.
- Secret-handling checklist.

Review owner: delivery manager plus security/governance.

## Known Blockers

- `TBD - owner decision required`: production credential storage.
- Firebase project/config ownership is partially resolved: project `shreeshyamstore`, app id `com.harrylabs.shreeshyamstore`, Firestore `(default)` in `asia-south1`, and Android config are present. Remaining owner decisions: dev/prod separation, delete protection/PITR posture, App Check, and cost guardrails.
- `TBD - owner decision required`: complete/confirm live Firebase Authentication Google provider enablement and manual owner sign-in/shop-profile QA; current direction is Credential Manager Sign in with Google first.
- `TBD - owner decision required`: App Check enforcement timing, cost monitoring, backup/retention, and high-risk conflict policy details.
- `TBD - owner decision required`: invoice legal/tax fields.
- `TBD - owner decision required`: backup/export/import format and restore policy.
- `TBD - owner decision required`: release icon and signing setup. Final package identity is already accepted and implemented.
