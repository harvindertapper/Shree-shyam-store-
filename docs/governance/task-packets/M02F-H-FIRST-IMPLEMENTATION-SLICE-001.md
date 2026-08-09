# M02F-H-FIRST-IMPLEMENTATION-SLICE-001

> **Historical/Superseded - do not execute.** Retained as pre-FR-P planning history. Old `com.example` paths below are historical; current implementation routes through `FR-G` using `com.harrylabs.shreeshyamstore`.

## task_id

M02F-H-FIRST-IMPLEMENTATION-SLICE-001

## goal

Implement the first approved Firebase foundation slice after architecture acceptance: Auth plus shop profile restore and one safe data domain before billing hardening.

## repo

`C:\Users\Harvinder\Documents\Codex\2026-06-14\can-you-work-on-android-project\work\Shree-shyam-store`

## role

Android Integration Worker for Shree Shyam Store Firebase foundation.

## read_first

- `AGENTS.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/DELIVERY_WORKFLOW.md`
- `docs/SCREEN_FLOW.md`
- `docs/DATA_MODEL.md`
- `docs/governance/02_SCOPE_CONTROL.md`
- `docs/governance/03_DECISION_LOG.md`
- `docs/governance/04_DEFINITION_OF_DONE.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`
- `docs/governance/MANUAL_QA_BACKLOG.md`
- `APP_BUILD_CHECKLIST.md`
- Accepted outputs from `M02F-A` through `M02F-G`.

## scope_paths

Allowed after dependencies are accepted:

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `settings.gradle.kts` only if the accepted Firebase setup requires it.
- `app/google-services.json` only if owner-approved config handling allows committing this public client config.
- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/data/SettingsDataStore.kt`
- New Firebase integration files under `app/src/main/java/com/example/data/` or `app/src/main/java/com/example/firebase/`
- Auth/shop-profile UI files only if explicitly required by the accepted architecture.
- Tests under `app/src/test/java/com/example/` and `app/src/androidTest/java/com/example/`.

## dependencies

All must be accepted first:

- `M02F-A-FIREBASE-ARCHITECTURE-DECISION-001`
- `M02F-B-FIREBASE-PROJECT-CONFIG-PREREQS-001`
- `M02F-C-AUTH-MODEL-001`
- `M02F-D-FIRESTORE-DATA-MODEL-001`
- `M02F-E-SECURITY-RULES-PRIVACY-001`
- `M02F-F-ROOM-CLOUD-MIGRATION-001`
- `M02F-G-OFFLINE-CONFLICT-QA-001`

## constraints

- Do not start this packet until the delivery manager and owner accept the planning packets.
- First safe data domain should be shop profile/settings or another low-risk domain approved by the architecture packet, not sales, udhaar, stock decrement, or invoice records.
- Recommended M02F-A slice is Auth + user profile + shop profile/settings only.
- Keep user-facing text in English and Hindi string resources.
- Do not store secrets, service-account keys, signing passwords, or production credentials in repo.
- Do not add Room schema changes unless the accepted migration strategy explicitly approves them.
- Do not change billing behavior.

## acceptance_criteria

- Firebase SDK/config scaffold matches approved project/config policy.
- Owner can sign in with the approved provider in a test environment.
- Shop profile/settings can be created/restored from Firestore.
- Security rules are available for review before any sensitive data sync.
- Clear-storage/reinstall restore path is demonstrable for the implemented slice.
- Offline behavior for the implemented slice is documented and tested.
- No billing, payment, stock, udhaar, or invoice behavior changes.

## required_evidence

- Files changed and purpose.
- Owner/Firebase config decisions used.
- Build/test evidence:
  - `.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon`
  - `.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon`
  - `.\gradlew.bat :app:assembleDebugAndroidTest --stacktrace --console=plain --no-daemon`
  - `.\gradlew.bat :app:connectedDebugAndroidTest --stacktrace --console=plain --no-daemon` if Android/emulator flow changed.
- Manual QA evidence for sign-in, clear storage, restore, offline/reconnect, and cross-account isolation as applicable.
- `git status --short --untracked-files=all`.

## review_owner

Delivery manager plus Android reviewer plus security/privacy reviewer plus owner.

## do_not_touch

- Billing hardening, stock decrement behavior, udhaar calculations, invoice math, payment behavior, reports, printer, PDF, WhatsApp, barcode, supplier, GST, release signing, package rename, and welcome sound/asset work.
- Room schema/database version/migrations unless approved by `M02F-F`.
- `.env`, secret files, signing files, service-account keys, or real customer/shop production data.
- Staff-role UI/permissions implementation.
