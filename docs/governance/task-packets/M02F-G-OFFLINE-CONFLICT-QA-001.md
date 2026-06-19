# M02F-G-OFFLINE-CONFLICT-QA-001

> **Historical/Superseded - do not execute.** Retained as planning history. Current restore QA routes through `FR-I1` and `FR-I2`.

## task_id

M02F-G-OFFLINE-CONFLICT-QA-001

## goal

Define offline behavior, conflict handling, and QA evidence for Firebase-backed recovery before billing hardening starts.

## repo

`C:\Users\Harvinder\Documents\Codex\2026-06-14\can-you-work-on-android-project\work\Shree-shyam-store`

## role

Offline/Conflict/QA Worker for Shree Shyam Store Firebase foundation.

## read_first

- `AGENTS.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/SCREEN_FLOW.md`
- `docs/DATA_MODEL.md`
- `docs/DELIVERY_WORKFLOW.md`
- `docs/governance/04_DEFINITION_OF_DONE.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`
- `docs/governance/MANUAL_QA_BACKLOG.md`
- `APP_BUILD_CHECKLIST.md`

## scope_paths

Allowed:

- `docs/SCREEN_FLOW.md`
- `docs/DATA_MODEL.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/governance/MANUAL_QA_BACKLOG.md`
- `APP_BUILD_CHECKLIST.md`
- `docs/governance/04_DEFINITION_OF_DONE.md`

## dependencies

- `M02F-A-FIREBASE-ARCHITECTURE-DECISION-001`
- `M02F-D-FIRESTORE-DATA-MODEL-001`
- `M02F-F-ROOM-CLOUD-MIGRATION-001`

## constraints

- Do not implement tests or app code in this packet.
- Offline behavior must be practical for kiryana shop use: billing must not be made less reliable later.
- Conflict handling must be conservative for sales, udhaar, and stock because these affect business records.
- QA must include clear storage, reinstall, second-device restore, offline start, offline write, reconnect, and cross-account isolation.
- Do not claim cloud backup/recovery works until tested.

## acceptance_criteria

- Offline read/write expectations are documented per domain.
- Conflict policy is documented for low-risk domains and high-risk domains.
- QA plan includes exact scenarios and required evidence.
- Acceptance gates say which tests/manual checks must pass before Billing Phase 2 resumes.
- Manual QA backlog is updated to route clear-storage data loss to cloud restore QA.

## required_evidence

- Files changed and purpose.
- QA matrix or checklist.
- Conflict policy summary.
- Confirmation that no app code or test code was changed.
- `git status --short --untracked-files=all`.
- Docs-only validation commands run, or explanation if not needed.

## review_owner

Delivery manager plus QA reviewer plus owner.

## do_not_touch

- `app/src/main/**`
- `app/src/test/**`
- `app/src/androidTest/**`
- Gradle files
- `.env`, secret files, signing files, or service-account keys
- Billing, stock, udhaar, invoice, payment, printer, PDF, WhatsApp, barcode, supplier, GST, release signing, package rename, and welcome sound/asset work.
