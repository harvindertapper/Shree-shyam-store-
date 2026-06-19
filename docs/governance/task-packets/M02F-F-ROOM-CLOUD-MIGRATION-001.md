# M02F-F-ROOM-CLOUD-MIGRATION-001

> **Historical/Superseded - do not execute.** Retained as pre-FR-B/pre-FR-P planning history. Old package paths and broad migration-risk wording below describe the historical state; current code uses Room v2 and `com.harrylabs.shreeshyamstore`.

## task_id

M02F-F-ROOM-CLOUD-MIGRATION-001

## goal

Define the migration and sync strategy from current local Room data to Firebase-backed shop data without destructive schema changes or data loss.

## repo

`C:\Users\Harvinder\Documents\Codex\2026-06-14\can-you-work-on-android-project\work\Shree-shyam-store`

## role

Data Migration Worker for Shree Shyam Store Firebase foundation.

## read_first

- `AGENTS.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/DATA_MODEL.md`
- `docs/governance/02_SCOPE_CONTROL.md`
- `docs/governance/03_DECISION_LOG.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`
- `docs/governance/MANUAL_QA_BACKLOG.md`
- `app/src/main/java/com/example/data/Entities.kt`
- `app/src/main/java/com/example/data/Daos.kt`
- `app/src/main/java/com/example/data/ShopDatabase.kt`
- `app/src/main/java/com/example/data/ShopRepository.kt`
- `app/src/main/java/com/example/data/SettingsDataStore.kt`

## scope_paths

Allowed:

- `docs/DATA_MODEL.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/governance/MANUAL_QA_BACKLOG.md`
- `docs/governance/03_DECISION_LOG.md`

Read-only inspection allowed:

- `app/src/main/java/com/example/data/**`

## dependencies

- `M02F-A-FIREBASE-ARCHITECTURE-DECISION-001`
- `M02F-C-AUTH-MODEL-001`
- `M02F-D-FIRESTORE-DATA-MODEL-001`

## constraints

- Do not change Room schema, database version, migrations, or repository code in this packet.
- Current `fallbackToDestructiveMigration()` risk must be considered before any schema change.
- Existing local shop data must not be silently overwritten by cloud restore.
- Migration must cover fresh install, clear storage, reinstall, existing local data, and future second device.
- DataStore must not store secrets, tokens, or service credentials.

## acceptance_criteria

- Migration states are documented: no local data, local-only data, cloud-only data, both local and cloud, conflict present.
- Initial upload, restore, merge, and user-confirmation behavior are documented.
- ID mapping and sync metadata strategy are defined or marked as owner decision.
- The plan states what blocks Room schema changes and destructive migrations.
- Manual QA backlog clear-storage issue is linked to cloud restore expectations.

## required_evidence

- Files changed and purpose.
- Migration flow summary.
- Data-loss risks and mitigations.
- Confirmation that no Room/app code was changed.
- `git status --short --untracked-files=all`.
- Docs-only validation commands run, or explanation if not needed.

## review_owner

Delivery manager plus data reviewer plus owner.

## do_not_touch

- Room entities, DAOs, database version, migrations, repository implementation
- `app/src/main/java/**` in write mode
- Gradle files
- `.env`, secret files, signing files, or service-account keys
- Billing, stock, udhaar, invoice, payment, printer, PDF, WhatsApp, barcode, supplier, GST, release signing, package rename, and welcome sound/asset work.
