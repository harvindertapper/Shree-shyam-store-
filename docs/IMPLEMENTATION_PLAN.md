# Implementation Plan

Status: Draft source of truth for module sequencing. Each module should be assigned as a separate task packet unless the user explicitly approves a combined pass.

## Implementation Sequence

1. M01 Foundation, repo hygiene, and localization baseline.
2. FR-A V2 data/calculator design.
3. FR-C Quantity/price calculator utility.
4. FR-B Room v2 reset.
5. FR-P App identity rename to `com.harrylabs.shreeshyamstore`.
6. FR-G Firebase Auth/shop profile.
7. FR-G2 Firestore rules/App Check/cost guardrails.
8. FR-D Product Add/Edit unit/rate/stock UI with inline category creation.
9. FR-H Product/category/settings Firestore sync.
10. FR-I1 Inventory restore QA.
11. FR-E Billing weighted entry plus per-line rate override.
12. FR-F Sale/stock/invoice/udhaar persistence.
13. FR-I2 Billing restore QA.
14. FR-J Sales/stock/udhaar cloud sync plan.
15. M06 Billing Phase 2 hardening after foundation reset gates pass.

## Shared Constraints

- Preserve existing Room data unless an approved migration is included.
- Do not add `fallbackToDestructiveMigration()` to new production schema work.
- Owner decision on 2026-06-17 approves Firebase Auth/Firestore/cloud sync as mandatory MVP foundation before billing.
- Owner decision on 2026-06-17 sets final application id, namespace, Firebase app identity, and Play Store identity to `com.harrylabs.shreeshyamstore`.
- No real inventory has been entered yet; Room v2 reset is allowed in FR-B after FR-A and FR-C.
- Real inventory entry is blocked until FR-G, FR-G2, FR-D, FR-H, and FR-I1 pass.
- Do not add random Firebase implementation without accepted architecture, security rules, privacy, migration, offline/conflict, and QA packets.
- Do not add Bluetooth printer, multi-store implementation, staff role implementation, or real UPI verification without owner approval.
- All user-facing text must go through English and Hindi string resources.
- Billing, stock, udhaar, reports, backup/import/export, and auth changes need focused tests or documented manual proof.
- UI-facing changes should include screenshots or emulator proof when possible.

## Task Packets

DM-004 task packet files live under `docs/governance/task-packets/FR-*.md`. These supersede the older M02F/M05/M06 route until the foundation reset gates are complete.

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

### M02F-FIREBASE-CLOUD-FOUNDATION-001

Goal: Establish Firebase Auth and Firestore as the MVP foundation before billing hardening.

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

- Owner-approved Firebase architecture and Auth provider decision exist.
- M02F-A architecture recommendation is accepted or explicitly revised by the owner.
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
- `Accepted`: final app identity is `com.harrylabs.shreeshyamstore`.
- `TBD - owner decision required`: Firebase project/config ownership, environment separation, app registration for `com.harrylabs.shreeshyamstore`, Firestore region, SHA-1/SHA-256 keys, App Check, and cost guardrails.
- `TBD - owner decision required`: Firebase Auth providers for MVP.
- `TBD - owner decision required`: approve or revise the M02F-A hybrid architecture recommendation.
- `TBD - owner decision required`: Firestore ownership model, rules, cost controls, backup/retention, and conflict policy.
- `TBD - owner decision required`: unit list, decimal precision, and base-unit strategy for loose/weight-based products.
- `TBD - owner decision required`: database migration policy and removal of destructive migration.
- `TBD - owner decision required`: invoice legal/tax fields.
- `TBD - owner decision required`: backup/export/import format and restore policy.
- `TBD - owner decision required`: final package/branding/signing choices.
