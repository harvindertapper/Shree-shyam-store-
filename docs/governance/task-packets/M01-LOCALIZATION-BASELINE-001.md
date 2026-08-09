# M01-LOCALIZATION-BASELINE-001

> **Historical/Superseded - do not execute.** Completed before FR-P and retained as historical evidence. Old `com.example` paths below are not current instructions; current package is `com.harrylabs.shreeshyamstore`.

## task_id

M01-LOCALIZATION-BASELINE-001

## goal

Fix the localization baseline so Hindi resources are verified as real Devanagari Hindi and the tests no longer expect mojibake text.

## repo

`C:\Users\Harvinder\Documents\Codex\2026-06-14\can-you-work-on-android-project\work\Shree-shyam-store`

## role

You are the Module Execution Agent for Shree Shyam Store, a native Kotlin Android kiryana/general-store app.

## read_first

- `AGENTS.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/DELIVERY_WORKFLOW.md`
- `docs/SCREEN_FLOW.md`
- `docs/DATA_MODEL.md`
- `docs/governance/04_DEFINITION_OF_DONE.md`
- `APP_BUILD_CHECKLIST.md`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-hi/strings.xml`
- `app/src/test/java/com/example/LocalizationBaselineTest.kt`

## current_evidence

- English and Hindi string resources currently have matching key counts: 165 English keys and 165 Hindi keys.
- A UTF-8 codepoint scan of `app/src/main/res/values-hi/strings.xml` found zero occurrences of common mojibake markers such as Latin-1/cp1252 corruption characters and the Unicode replacement character.
- `app/src/main/res/values-hi/strings.xml` currently contains the correct Hindi app name: `श्री श्याम स्टोर`.
- `app/src/test/java/com/example/LocalizationBaselineTest.kt` currently expects the correct Hindi app name. Treat this task as a verification-and-cleanup task unless your fresh scan finds corrupted values.
- The worktree is already dirty. Preserve existing unrelated changes and do not revert files you did not intentionally modify.

## scope_paths

Allowed:

- `app/src/test/java/com/example/LocalizationBaselineTest.kt`
- `app/src/main/res/values-hi/strings.xml` only if your own scan finds actual invalid or corrupted Hindi values.
- `app/src/main/res/values/strings.xml` only if key parity is broken or an English resource required by this test is missing.

Do not touch:

- Billing, payment, stock, udhaar, reports, or invoice logic.
- Room entities, DAOs, database version, migrations, or repository logic.
- Auth/password/security implementation.
- Gradle dependency files.
- Package/application id.
- Release signing.
- Bluetooth, cloud sync, backend/API, printer, or real UPI verification work.
- `.env` or secret files.

## dependencies

- Parent module: `M01-FOUNDATION-001`.
- No upstream implementation module is required.

## constraints

- English remains the default language.
- Hindi must be proper Devanagari Hindi, not Hinglish and not mojibake.
- Preserve every existing string key.
- Do not add features.
- Do not add dependencies.
- Do not broaden this into full UI copy cleanup. Hardcoded screen text cleanup is a later task.

## acceptance_criteria

- `LocalizationBaselineTest.hindiResourcesProvideHindiAppName` expects `श्री श्याम स्टोर`.
- `app/src/main/res/values-hi/strings.xml` has no mojibake markers after the worker's scan.
- English and Hindi string key sets still match exactly.
- No unrelated files are modified.
- `.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon` passes.
- `.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon` passes.

## required_evidence

The worker completion response must include:

- Files changed.
- Key parity result for English and Hindi resources.
- Mojibake scan result for `values-hi/strings.xml` and `LocalizationBaselineTest.kt`.
- Build command result.
- Unit-test command result.
- Any skipped verification with exact reason.
- Confirmation that no `.env` or secrets were read.

## review_owner

Delivery manager plus QA.

## manager_review_gate

The delivery manager should block acceptance if:

- The worker rewrites unrelated UI copy or changes app behavior.
- Hindi text is still mojibake or mixed Hinglish.
- The test still expects corrupted text.
- Key parity is not proven.
- Build or unit tests are missing or failing without a clear environmental blocker.
- Unrelated dirty-worktree changes are reverted, staged, or included in the worker's claimed scope.

## expected_next_task_after_acceptance

After this task is accepted, the next likely task is a focused UI copy cleanup slice for login/setup/home strings, not billing logic yet.
