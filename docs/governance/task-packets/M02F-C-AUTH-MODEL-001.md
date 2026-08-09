# M02F-C-AUTH-MODEL-001

> **Historical/Superseded - do not execute.** Retained as planning history. Current Auth direction is Android Credential Manager Sign in with Google through `FR-G`.

## task_id

M02F-C-AUTH-MODEL-001

## goal

Define the MVP authentication model: Google Sign-In first, phone OTP later/optional, owner UID, shop ownership, and future membership boundaries.

## repo

`C:\Users\Harvinder\Documents\Codex\2026-06-14\can-you-work-on-android-project\work\Shree-shyam-store`

## role

Auth Model Worker for Shree Shyam Store.

## read_first

- `AGENTS.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/SCREEN_FLOW.md`
- `docs/DATA_MODEL.md`
- `docs/governance/02_SCOPE_CONTROL.md`
- `docs/governance/03_DECISION_LOG.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`
- `docs/governance/MANUAL_QA_BACKLOG.md`

## scope_paths

Allowed:

- `docs/PRODUCT_SPEC.md`
- `docs/SCREEN_FLOW.md`
- `docs/DATA_MODEL.md`
- `docs/governance/03_DECISION_LOG.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`
- `docs/governance/MANUAL_QA_BACKLOG.md`

## dependencies

- `M02F-A-FIREBASE-ARCHITECTURE-DECISION-001`
- `M02F-B-FIREBASE-PROJECT-CONFIG-PREREQS-001`

## constraints

- Treat Google Sign-In as the default planning assumption unless the owner decides differently.
- Phone OTP is optional/later because it introduces cost, abuse, rate-limit, and recovery concerns.
- Local owner/password behavior must not be worsened during planning.
- Do not implement Auth UI, Firebase SDK calls, or sign-in code in this packet.
- Model future staff membership but do not implement staff roles or permissions UI.

## acceptance_criteria

- MVP sign-in flow is documented, including first launch, returning owner, clear storage/reinstall restore, and sign-out expectations.
- Owner UID to shop ownership/membership relationship is documented.
- Local Room owner account and Firebase owner identity interaction is explicitly decided or marked as an owner decision.
- Phone OTP risks and deferral conditions are documented.
- Screen flow updates do not conflict with current Compose navigation.

## required_evidence

- Files changed and purpose.
- Auth provider recommendation and owner decisions still needed.
- Confirmation that no Auth code/config/secrets were added.
- `git status --short --untracked-files=all`.
- Docs-only validation commands run, or explanation if not needed.

## review_owner

Delivery manager plus security/privacy reviewer.

## do_not_touch

- `app/src/main/java/**`
- `app/src/main/res/**`
- `app/src/test/**`
- `app/src/androidTest/**`
- Gradle files
- `.env`, secret files, signing files, or service-account keys
- Billing, stock, udhaar, invoice, payment, printer, PDF, WhatsApp, barcode, supplier, GST, release signing, package rename, and welcome sound/asset work.
