# M02F-A-FIREBASE-ARCHITECTURE-DECISION-001

> **Historical/Superseded - do not execute.** The architecture decision is accepted in `FIREBASE_CLOUD_SYNC_ARCHITECTURE.md`; current work routes through the DM-004 FR packets.

## task_id

M02F-A-FIREBASE-ARCHITECTURE-DECISION-001

## goal

Produce the Firebase Cloud Sync Foundation architecture decision for Shree Shyam Store before any Billing Phase 2 work or Firebase implementation begins.

## repo

`C:\Users\Harvinder\Documents\Codex\2026-06-14\can-you-work-on-android-project\work\Shree-shyam-store`

## role

Cloud Architecture Worker for a native Kotlin Android kiryana/general-store app.

## read_first

- `AGENTS.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/DELIVERY_WORKFLOW.md`
- `docs/SCREEN_FLOW.md`
- `docs/DATA_MODEL.md`
- `docs/governance/01_PROJECT_CHARTER.md`
- `docs/governance/02_SCOPE_CONTROL.md`
- `docs/governance/03_DECISION_LOG.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`
- `docs/governance/MANUAL_QA_BACKLOG.md`
- `APP_BUILD_CHECKLIST.md`

## scope_paths

Allowed:

- `docs/DATA_MODEL.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/governance/03_DECISION_LOG.md`
- `docs/governance/task-packets/M02F-*.md` only if cross-packet dependency wording needs correction.

## dependencies

- Owner decision in `DM-002-CLOUD-SYNC-ROADMAP-RESET`.
- Phase 1 localization checkpoint remains complete and must not be restarted.

## constraints

- Firebase Auth and Firestore are mandatory MVP foundation.
- Default recommendation is Firebase/Auth/Firestore foundation before Billing Phase 2.
- Decide architecture in docs first; do not add Firebase SDKs, Gradle config, `google-services.json`, or app code.
- Keep the app offline-capable while defining cloud as the recovery and future multi-device foundation.
- Model future staff membership only as data ownership planning. Do not implement staff roles.
- Do not make payment verification claims.

## acceptance_criteria

- The architecture names the MVP source-of-truth strategy: Firestore canonical, Room cache, or hybrid, with clear tradeoffs.
- The architecture defines owner UID, shop id, membership boundary, and how all future business records are scoped to a shop.
- The plan identifies the first safe implementation slice before billing.
- Open owner decisions are listed explicitly instead of assumed.
- Billing Phase 2 remains blocked until cloud ownership, security, migration, offline, and QA gates are accepted.

## required_evidence

- Files changed and purpose.
- Summary of recommended architecture and rejected alternatives.
- List of unresolved owner decisions.
- Confirmation that no Firebase code/config/secrets were added.
- `git status --short --untracked-files=all`.
- Docs-only validation commands run, or explanation if not needed.

## review_owner

Delivery manager plus owner.

## do_not_touch

- `app/src/main/**`
- `app/src/test/**`
- `app/src/androidTest/**`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `google-services.json`
- `.env`, secret files, signing files, or service-account keys
- Billing, stock, udhaar, invoice, reports, payment, printer, PDF, WhatsApp, barcode, supplier, GST, release signing, package rename, and welcome sound/asset work.
