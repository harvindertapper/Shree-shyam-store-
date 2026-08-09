# M02F-B-FIREBASE-PROJECT-CONFIG-PREREQS-001

> **Historical/Superseded - do not execute.** Retained as planning history. Current Firebase prerequisites and routing are defined by `docs/IMPLEMENTATION_PLAN.md`, `FOUNDATION_RESET_DM004.md`, and the FR packets.

## task_id

M02F-B-FIREBASE-PROJECT-CONFIG-PREREQS-001

## goal

Define the Firebase project, Android app registration, environment, and config-file prerequisites before Android Firebase integration work starts.

## repo

`C:\Users\Harvinder\Documents\Codex\2026-06-14\can-you-work-on-android-project\work\Shree-shyam-store`

## role

Firebase Project/Config Worker for Shree Shyam Store.

## read_first

- `AGENTS.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/DELIVERY_WORKFLOW.md`
- `docs/DATA_MODEL.md`
- `docs/governance/02_SCOPE_CONTROL.md`
- `docs/governance/03_DECISION_LOG.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`
- `APP_BUILD_CHECKLIST.md`

## scope_paths

Allowed:

- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/DATA_MODEL.md`
- `docs/governance/02_SCOPE_CONTROL.md`
- `docs/governance/03_DECISION_LOG.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`
- `APP_BUILD_CHECKLIST.md`

## dependencies

- `M02F-A-FIREBASE-ARCHITECTURE-DECISION-001` should be accepted or have stable draft assumptions.

## constraints

- Do not create or modify real Firebase project settings from this repo unless owner explicitly asks.
- Do not read `.env` or secret files.
- `google-services.json` handling must be decided before any config file is committed.
- Public client Firebase config is not the same as a secret, but service-account keys, signing passwords, and production credentials must never be committed.
- Account for debug, release, and future store package/application id changes.

## acceptance_criteria

- The required owner-provided Firebase values are listed: Firebase project id, Android package/application id, SHA certificates if needed, environment policy, Firestore region, and enabled Auth providers.
- The repo policy for `google-services.json` is documented.
- The packet identifies whether separate dev/prod Firebase projects are required before implementation.
- The plan includes cost/billing guardrails that must be checked before Firestore writes are enabled.

## required_evidence

- Files changed and purpose.
- Exact list of owner inputs still needed.
- Confirmation that no secrets, service-account keys, or real credentials were read or committed.
- `git status --short --untracked-files=all`.
- Docs-only validation commands run, or explanation if not needed.

## review_owner

Delivery manager plus owner.

## do_not_touch

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `settings.gradle.kts`
- `google-services.json`
- `app/src/main/**`
- `app/src/test/**`
- `.env`, secret files, signing files, or service-account keys
- Billing, stock, udhaar, invoice, payment, printer, PDF, WhatsApp, barcode, supplier, GST, package rename, and welcome sound/asset work.
