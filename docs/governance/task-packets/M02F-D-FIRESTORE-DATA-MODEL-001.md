# M02F-D-FIRESTORE-DATA-MODEL-001

> **Historical/Superseded - do not execute.** Retained as pre-FR-P planning history. Old `com.example` paths below are historical; current package is `com.harrylabs.shreeshyamstore`.

## task_id

M02F-D-FIRESTORE-DATA-MODEL-001

## goal

Map Shree Shyam Store business data to Firestore collections and documents while preserving local Room auditability and future sync safety.

## repo

`C:\Users\Harvinder\Documents\Codex\2026-06-14\can-you-work-on-android-project\work\Shree-shyam-store`

## role

Firestore Data Model Worker for Shree Shyam Store.

## read_first

- `AGENTS.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/DATA_MODEL.md`
- `docs/governance/02_SCOPE_CONTROL.md`
- `docs/governance/03_DECISION_LOG.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`
- `app/src/main/java/com/example/data/Entities.kt`
- `app/src/main/java/com/example/data/Daos.kt`
- `app/src/main/java/com/example/data/ShopDatabase.kt`
- `app/src/main/java/com/example/data/ShopRepository.kt`

## scope_paths

Allowed:

- `docs/DATA_MODEL.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/governance/03_DECISION_LOG.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`

Read-only inspection allowed:

- `app/src/main/java/com/example/data/Entities.kt`
- `app/src/main/java/com/example/data/Daos.kt`
- `app/src/main/java/com/example/data/ShopDatabase.kt`
- `app/src/main/java/com/example/data/ShopRepository.kt`

## dependencies

- `M02F-A-FIREBASE-ARCHITECTURE-DECISION-001`
- `M02F-C-AUTH-MODEL-001`

## constraints

- Do not change Room entities, DAOs, database version, migrations, repository code, or app behavior.
- Include users, shops, members, settings, categories, products, customers, sales, sale items, udhaar transactions, and stock adjustments.
- Business records must be scoped under a shop ownership boundary.
- Sales, udhaar, and stock changes are sensitive and must remain auditable.
- Product and sale item cloud documents must be ready for future unit-of-measure and decimal quantity work from MQA-003 without implementing that UI/schema change now.
- Do not add GST/tax, supplier, returns, or staff-role implementation.

## acceptance_criteria

- Firestore collection/document structure is documented for all MVP domains.
- Current Room entities are mapped to proposed Firestore documents.
- Product, stock, and sale item documents include a planned unit/measurement strategy for piece, weight, volume, packet/box/custom, and decimal quantities.
- Each domain states canonical id strategy, timestamps, deletion behavior, and offline-write concerns.
- The model identifies which domain is safe for the first implementation slice before billing.
- Sensitive fields and privacy notes are documented.

## required_evidence

- Files changed and purpose.
- Room-to-Firestore mapping table or equivalent structured section.
- List of unresolved modeling decisions.
- Confirmation that app code and Room schema were not changed.
- `git status --short --untracked-files=all`.
- Docs-only validation commands run, or explanation if not needed.

## review_owner

Delivery manager plus data/security reviewer.

## do_not_touch

- Any app source file in write mode
- Room schema, DAOs, database version, migrations, repository logic
- Gradle files
- `.env`, secret files, signing files, or service-account keys
- Billing hardening, stock decrement behavior, udhaar calculations, invoice math, payment behavior, printer, PDF, WhatsApp, barcode, supplier, GST, release signing, package rename, and welcome sound/asset work.
