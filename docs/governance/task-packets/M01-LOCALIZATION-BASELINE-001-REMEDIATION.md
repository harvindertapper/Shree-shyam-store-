# M01-LOCALIZATION-BASELINE-001-REMEDIATION

> **Historical/Superseded - do not execute.** Completed before FR-P and retained as historical evidence. Old `com.example` paths below are not current instructions; current package is `com.harrylabs.shreeshyamstore`.

## task_id

M01-LOCALIZATION-BASELINE-001-REMEDIATION

## goal

Unblock `M01-LOCALIZATION-BASELINE-001` by correcting the out-of-scope localization source-quality test so the required unit-test command validates only the M01 localization baseline requirements.

## repo

`C:\Users\Harvinder\Documents\Codex\2026-06-14\can-you-work-on-android-project\work\Shree-shyam-store`

## role

You are the Module Execution Agent for Shree Shyam Store, a native Kotlin Android kiryana/general-store app.

## read_first

- `AGENTS.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/DELIVERY_WORKFLOW.md`
- `docs/governance/task-packets/M01-LOCALIZATION-BASELINE-001.md`
- `docs/governance/module-acceptance/M01-LOCALIZATION-BASELINE-001.md`
- `docs/governance/review-checklists/M01-LOCALIZATION-BASELINE-001.md`
- `app/src/test/java/com/example/LocalizationSourceQualityTest.kt`
- `app/src/test/java/com/example/LocalizationBaselineTest.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-hi/strings.xml`

## current_evidence

- `M01-LOCALIZATION-BASELINE-001` is blocked because `.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon` fails.
- Failing tests are in `LocalizationSourceQualityTest`.
- `LocalizationSourceQualityTest.localizedScreensDoNotContainMojibakeOrMixedTranslationCopy` checks broad billing/products/reports/udhaar hardcoded and mixed UI copy. That is outside M01 and belongs to a later UI-copy cleanup task.
- `LocalizationSourceQualityTest.englishAndHindiStringResourcesHaveMatchingKeys` failed in the latest manager run, while a manager-side XML/Python parse after the failure showed `165` English keys and `165` Hindi keys.
- `LocalizationBaselineTest` currently expects the correct Hindi app name: `श्री श्याम स्टोर`.
- The worktree is dirty. Preserve unrelated user/worker changes and do not revert files outside the allowed scope.

## scope_paths

Allowed:

- `app/src/test/java/com/example/LocalizationSourceQualityTest.kt`
- `app/src/test/java/com/example/LocalizationBaselineTest.kt` only if it no longer expects `श्री श्याम स्टोर`
- `app/src/main/res/values/strings.xml` only if key parity is actually broken after a fresh XML parse
- `app/src/main/res/values-hi/strings.xml` only if key parity is actually broken or mojibake is found after a fresh UTF-8 scan

Do not touch:

- Billing, payment, stock, udhaar, reports, or invoice behavior.
- `app/src/main/java/com/example/ui/screens/BillingAndPaymentScreen.kt`
- `app/src/main/java/com/example/ui/screens/ProductsAndStockScreen.kt`
- `app/src/main/java/com/example/ui/screens/ReportsScreen.kt`
- `app/src/main/java/com/example/ui/screens/UdhaarScreen.kt`
- Room entities, DAOs, database version, migrations, or repository logic.
- Auth/password/security implementation.
- Gradle dependency files.
- Package/application id.
- Release signing.
- Bluetooth, cloud sync, backend/API, printer, or real UPI verification work.
- `.env` or secret files.

## dependencies

- Parent task: `M01-LOCALIZATION-BASELINE-001`.
- Blocked audit: `docs/governance/module-acceptance/M01-LOCALIZATION-BASELINE-001.md`.

## constraints

- Keep this remediation narrow.
- Do not convert broad hardcoded UI copy in billing/products/reports/udhaar screens.
- Do not add new localization requirements beyond M01.
- If `LocalizationSourceQualityTest.kt` was created for M01, either rewrite it to M01 scope or remove it. If unsure who created it, prefer rewriting it narrowly over deleting it.
- English remains the default language.
- Hindi must be Devanagari Hindi, not mojibake.
- Preserve string keys unless a fresh XML parse proves a key-parity bug.

## acceptance_criteria

- `LocalizationSourceQualityTest`, if present, validates only M01 requirements:
  - English/Hindi string key parity.
  - No mojibake markers or Unicode replacement characters in `values-hi/strings.xml`.
  - Optional: `LocalizationBaselineTest.kt` source contains `श्री श्याम स्टोर`.
- `LocalizationSourceQualityTest` does not scan billing/products/reports/udhaar source files for broad hardcoded or mixed UI copy.
- `LocalizationBaselineTest.hindiResourcesProvideHindiAppName` expects `श्री श्याम स्टोर`.
- English and Hindi string key sets match exactly after a fresh parse.
- `values-hi/strings.xml` has no mojibake markers after a fresh UTF-8 scan.
- No unrelated files are modified by this remediation.
- `.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon` passes.
- `.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon` passes.

## required_evidence

The worker completion response must include:

- Files changed.
- Explanation of what was done to `LocalizationSourceQualityTest.kt`.
- Fresh English/Hindi key-parity result.
- Fresh mojibake scan result for `values-hi/strings.xml`.
- Confirmation that `LocalizationBaselineTest.kt` expects `श्री श्याम स्टोर`.
- Build command result.
- Unit-test command result.
- Confirmation that no `.env` or secret files were read.
- Any skipped or failed verification with exact reason and output.

## review_owner

Delivery manager plus QA.

## manager_review_gate

Block if:

- Unit tests still fail.
- The test continues to require broad app-wide UI-copy cleanup.
- The worker changes billing/products/reports/udhaar behavior or other out-of-scope files.
- Key parity is not proven.
- Mojibake scan evidence is missing.
- The worker reads `.env` or touches secrets.

## expected_next_task_after_acceptance

After remediation and M01 acceptance, create a focused UI-copy cleanup packet for login/setup/home screens.
