# AGENTS.md

## Project Goal

Build **Shree Shyam Store** as a professional Kotlin Android shop-management app customized for a small Indian kiryana/general store.

The app must feel reliable for daily kiryana shop use: cloud-backed owner access, recoverable shop data, fast billing, simple product/stock management, accurate udhaar tracking, owner-private business insights, useful daily close reports, and easy invoice sharing when needed.

## Product Language Rules

- The app must support **English and Hindi**.
- **English is the default language**.
- Hindi must be available as a proper second language, not mixed randomly into English screens.
- Do not use Hinglish for final app UI copy.
- Prefer Android string resources for all user-facing text.
- When adding or editing UI text, update both:
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-hi/strings.xml`
- Existing mixed or mojibake text should be cleaned gradually while working on affected screens.

## Current Stack

- Native Android app
- Kotlin
- Jetpack Compose
- Room database
- DataStore preferences
- Firebase Auth and Firestore are mandatory professional foundation work. FR-G implementation is in progress with local build/rules passing; final manual Google sign-in/shop-profile QA remains pending before broader sync work.
- Gradle Kotlin DSL
- Min Android: API 24 / Android 7.0
- Do not apply assumptions from unrelated app templates or cross-platform stacks.

## Approved Source-of-Truth Documents

Future agents must baseline these documents before changing code:

1. `AGENTS.md` - highest-priority repo operating rules.
2. `docs/PRODUCT_SPEC.md` - product scope, users, workflows, and owner decisions.
3. `docs/IMPLEMENTATION_PLAN.md` - module-by-module delivery sequence and task packets.
4. `docs/superpowers/plans/2026-07-02-professional-delivery-plan.md` - current professional delivery route for sync, billing, owner insights, invoice/share, and release hardening.
5. `docs/DELIVERY_WORKFLOW.md` - assignment, review, evidence, and completion workflow.
6. `docs/SCREEN_FLOW.md` - screen and navigation behavior.
7. `docs/DATA_MODEL.md` - Room entities, DataStore settings, and migration rules.
8. `docs/governance/*.md` - charter, scope control, decision log, definition of done, security/privacy checklist, and module acceptance guidance.
9. `APP_BUILD_CHECKLIST.md` - app inventory, professional delivery checklist, and QA checklist.
10. `README.md` - setup notes only. If README conflicts with the docs above, treat README as stale until updated.

`PROJECT_CONTEXT.md`, when present, is subordinate context only. It must not override the files above.

If source-of-truth documents disagree, stop and report the conflict instead of silently choosing one.

## Current Repo Shape

- Single Android app module: `app`.
- Main activity and manual Compose navigation: `app/src/main/java/com/harrylabs/shreeshyamstore/MainActivity.kt`.
- Screen state: `Screen` sealed class in `app/src/main/java/com/harrylabs/shreeshyamstore/viewmodel/ShopViewModel.kt`.
- Screens: `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/`.
- Business/data coordination: `app/src/main/java/com/harrylabs/shreeshyamstore/viewmodel/ShopViewModel.kt` and `app/src/main/java/com/harrylabs/shreeshyamstore/data/ShopRepository.kt`.
- Room entities/DAOs/database: `app/src/main/java/com/harrylabs/shreeshyamstore/data/`.
- Settings/session/language: `app/src/main/java/com/harrylabs/shreeshyamstore/data/SettingsDataStore.kt`.
- User-facing strings: `app/src/main/res/values/strings.xml` and `app/src/main/res/values-hi/strings.xml`.
- Unit/Robolectric tests: `app/src/test/java/com/harrylabs/shreeshyamstore/`.
- Instrumented tests: `app/src/androidTest/java/com/harrylabs/shreeshyamstore/`.

## Module-by-Module Work Rule

- Work on one module or task packet at a time.
- Read the source-of-truth docs before editing code.
- Keep write scope limited to the module named by the task.
- Do not combine unrelated cleanup with feature work.
- Any Room schema change requires an intentional migration plan and acceptance evidence.
- UI changes require English and Hindi string resources plus visual/manual verification when possible.
- Billing, stock, udhaar, reports, backup/import/export, auth, and release signing require extra review because they affect business records or app trust.

## Security and Privacy Non-Negotiables

- Treat shop data, customer names, phone numbers, invoices, udhaar balances, QR image URIs, and owner credentials as sensitive local business data.
- Do not store tokens, API keys, signing passwords, or production secrets in Room, DataStore, strings, source code, screenshots, or docs.
- Do not read `.env` or secret files unless the user explicitly approves it for that task. `.env.example` may be read.
- Legacy local password auth has been removed from the Room runtime path in FR-G/Room v4. Do not reintroduce local password storage without an owner-approved security packet.
- Room is schema version 5. The approved early reset uses `fallbackToDestructiveMigrationFrom(true, 1, 2)` because no real inventory existed; v3 to v4 uses an intentional migration that drops the deprecated local `users` table, and v4 to v5 intentionally adds the local `sync_outbox_operations` table for retryable cloud sync. Any future migration from v5 onward requires an intentional migration or a new explicit owner-approved reset; broad production destructive migration remains forbidden.
- UPI must remain a manual payment record unless real verification is implemented and approved.
- Backup/export/import must protect customer and sales data and must not silently leak files to shared storage.
- Android backup behavior must be reviewed before release because `android:allowBackup` is currently enabled.
- Do not add loan/credit scoring, GST/tax filing, legal compliance, or payment-success claims unless backed by approved workflow and evidence.

## Auth, Session, API, Storage, and Permission Rules

- The runtime auth/session flow uses Firebase Auth as owner identity. DataStore stores non-secret session markers such as cached owner UID/shop ID and display fields; Firebase tokens must not be stored in Room, DataStore, strings, or source code.
- DataStore may store preferences and non-secret session state, but not passwords, tokens, API secrets, or signing data.
- Room is the local source/cache for products, categories, sales, sale items, customers, udhaar transactions, and stock adjustments. The old local `users` table is removed from the runtime schema.
- Owner decision on 2026-06-17: Firebase Auth and Firestore/cloud sync are mandatory professional foundation, not deferred scope.
- Owner decision on 2026-06-17: final Android application id, namespace, Firebase app registration, Google Sign-In setup, SHA keys, and Play Store identity must be `com.harrylabs.shreeshyamstore`. Do not configure Firebase against the old random application id.
- Owner decision on 2026-06-17: no real shop inventory has been entered yet, so the approved early Room reset/migration path is allowed before real inventory entry.
- Firebase Auth/profile code and Firestore rules are implemented under FR-G. New broader Firebase sync/network behavior must follow the accepted professional delivery route, with security rules, privacy, migration, restore gates, conflict/idempotency, and QA evidence before trusted shop use.
- Do not commit Firebase service-account keys, signing secrets, API secrets, production credentials, or real customer data. Public client Firebase config may only be added through an approved implementation packet.
- No dangerous runtime permissions are currently declared in `AndroidManifest.xml`. Camera, location, notifications, contacts, storage, and Bluetooth require explicit approval and a privacy note before implementation.
- QR image selection must be treated as user-selected local content. Do not assume permanent URI access without testing.

## Standard Completion Response Format

Every future agent should finish with:

```text
## Summary
- What changed and why.

## Files changed
- Path and purpose for each changed file.

## Repo findings
- Relevant stack, architecture, data, security, or behavior findings.

## Source-of-truth impact
- Docs updated or consulted, and any remaining TBD decisions.

## Security/privacy impact
- Data, credential, payment, backup, permission, and migration impact.

## Tests run
- Commands run and results, or why tests were not required.

## Remaining owner decisions
- Decisions needing user approval.

## Next recommended task
- One module-aligned next task prompt.
```

## Engineering Rules

- Keep changes focused on the requested feature.
- Do not do broad refactors unless they are needed for the feature.
- Preserve existing Room data unless a migration is intentionally planned.
- Do not use `fallbackToDestructiveMigration()` for production schema changes without an explicit decision.
- Prefer small ViewModel/repository methods over pushing business logic deep into composables.
- Keep billing, stock, udhaar, and reports behavior auditable and testable.
- Never add real payment claims unless the app actually verifies payment.
- Do not add Bluetooth printer work yet. It is deferred.

## Priority Feature Roadmap

Completed foundation checkpoints:

- `FR-A` at `0bd189b`.
- `FR-C` at `f36d613`.
- `FR-B` at `4bb4927`.
- `FR-P` at `b7d92f2`.
- Firebase project/config prerequisites for project `shreeshyamstore`.
- `FR-K` Room UUID primary keys and relationship refactoring.

Current professional delivery route:

1. `PD-01` Stabilize the current FR-G/sync/billing work and verify main-safe Room access.
2. `PD-02` Finish/accept live Firebase owner sign-in and owner/shop/membership restore.
3. `PD-03A/B` Implement professional sync and restore gates for settings, categories, products, customers, and udhaar.
4. `PD-04` Finish product/stock setup for real kiryana use: piece/weight/volume units, inline category creation, stock-by-exception, and quick-added product cleanup.
5. `PD-05` Harden billing: no forced customer/invoice, one-tap sale save, loose amount-to-quantity flow, quantity-to-amount flow, per-line rate override, smart product quick-add, and tracked-stock validation.
6. `PD-03C` Sync sales, sale items, stock adjustments, and invoice metadata with idempotency.
7. `PD-06` Add quick udhaar entry and WhatsApp/share reminder workflow without Contacts permission.
8. `PD-07` Add private Owner Desk for total stock value, category-wise stock value, and profit using sale-item purchase-cost snapshots.
9. `PD-08` Add end-of-day "Aaj ka hisaab" summary with subtle shop-close routine joy.
10. `PD-09` Add optional PDF invoice and Android share/WhatsApp workflow.
11. `PD-10` Complete App Check/cost controls, Android backup policy, privacy, signing, localization polish, and release hardening.

## Deferred Scope

- Bluetooth thermal printer support.
- Multi-store or multi-branch management.
- Staff roles and permissions implementation. Future ownership/membership modeling is allowed inside Firebase foundation packets.
- Real UPI payment confirmation.

These can be added later, but do not start them without explicit approval.

## Verification Commands

Run these after meaningful code changes:

```powershell
.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon
```

When Android/emulator changes are involved:

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --stacktrace --console=plain --no-daemon
```

For manual launch QA:

```powershell
.\gradlew.bat :app:installDebug --stacktrace --console=plain --no-daemon
```

Then launch package:

```powershell
adb shell am start -n com.harrylabs.shreeshyamstore/com.harrylabs.shreeshyamstore.MainActivity
```

The launch package/activity uses the final `com.harrylabs.shreeshyamstore` identity.

## Definition of Done

A feature is done only when:

- It builds successfully.
- Relevant unit tests pass.
- Instrumented tests compile when Android-facing code changes.
- The app launches on emulator if the change affects UI/navigation.
- User-facing text is in English and Hindi resources.
- No unrelated files are modified.
- The final response lists changed files and verification results.

## Do Not Do

- Do not randomly rename packages or move files.
- Do not add new architecture/frameworks unless necessary.
- Do not hardcode secrets, API keys, signing passwords, or personal data.
- Do not delete existing app data behavior without explaining migration impact.
- Do not mark payment as successful unless it is only a manual record or actually verified.
- Do not add printer implementation in the current phase.
- Do not add random Firebase implementation before the approved Firebase architecture, security rules, migration, offline/conflict, and QA packets are accepted.
