# M02F-E-SECURITY-RULES-PRIVACY-001

> **Historical/Superseded - do not execute.** Retained as planning history. Current rules, App Check, and cost-control work routes through `FR-G2`.

## task_id

M02F-E-SECURITY-RULES-PRIVACY-001

## goal

Prepare the Firebase security rules, privacy, abuse, and cost-control plan before any shop/customer/sales data is synced.

## repo

`C:\Users\Harvinder\Documents\Codex\2026-06-14\can-you-work-on-android-project\work\Shree-shyam-store`

## role

Security/Privacy Worker for Shree Shyam Store Firebase foundation.

## read_first

- `AGENTS.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/DATA_MODEL.md`
- `docs/governance/01_PROJECT_CHARTER.md`
- `docs/governance/02_SCOPE_CONTROL.md`
- `docs/governance/03_DECISION_LOG.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`
- `APP_BUILD_CHECKLIST.md`

## scope_paths

Allowed:

- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`
- `docs/DATA_MODEL.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/governance/03_DECISION_LOG.md`

## dependencies

- `M02F-A-FIREBASE-ARCHITECTURE-DECISION-001`
- `M02F-C-AUTH-MODEL-001`
- `M02F-D-FIRESTORE-DATA-MODEL-001`

## constraints

- Security rules must be planned before sensitive data sync.
- App Check, Firestore rules tests, rate limits, quotas, and cost controls must be considered.
- Phone OTP must include abuse/cost/recovery notes if kept as future option.
- Do not implement Firebase rules files or deploy rules in this packet unless the owner explicitly expands scope.
- Do not read secrets or real production data.

## acceptance_criteria

- Security checklist covers Auth identity, shop membership, per-shop rules, App Check, indexes, quota/cost guardrails, backup/retention/delete policy, logs/screenshots, and rules tests.
- The plan states which data must never be public or queryable across shops.
- The plan identifies minimum rules-test cases for owner, non-member, signed-out, and cross-shop access.
- The plan blocks first implementation slice from syncing sensitive data until rules are reviewable.

## required_evidence

- Files changed and purpose.
- Security/privacy risks found and proposed gates.
- Rules-test scenarios list.
- Confirmation that no secrets, production data, or Firebase deployment was touched.
- `git status --short --untracked-files=all`.
- Docs-only validation commands run, or explanation if not needed.

## review_owner

Delivery manager plus security/privacy reviewer plus owner.

## do_not_touch

- `app/src/main/**`
- `app/src/test/**`
- `app/src/androidTest/**`
- Gradle files
- Firebase console/project state
- `.env`, secret files, signing files, service-account keys, or real customer/shop data
- Billing, stock, udhaar, invoice, payment, printer, PDF, WhatsApp, barcode, supplier, GST, release signing, package rename, and welcome sound/asset work.
