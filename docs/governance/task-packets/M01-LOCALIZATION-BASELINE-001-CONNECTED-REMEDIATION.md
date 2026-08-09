# M01-LOCALIZATION-BASELINE-001-CONNECTED-REMEDIATION

> **Historical/Superseded - do not execute.** Completed before FR-P and retained as historical test evidence. Old package names and commands below describe the historical failure state; current package is `com.harrylabs.shreeshyamstore`.

## task_id

M01-LOCALIZATION-BASELINE-001-CONNECTED-REMEDIATION

## goal

Close the remaining Phase 1 acceptance gap by making connected Android test evidence pass reliably after the localization baseline checkpoint.

## repo

`C:\Users\Harvinder\Documents\Codex\2026-06-14\can-you-work-on-android-project\work\Shree-shyam-store`

## role

You are the Module Execution Agent for Shree Shyam Store, a native Kotlin Android kiryana/general-store app.

This is a remediation task, not a new feature task.

## read_first

- `AGENTS.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/DELIVERY_WORKFLOW.md`
- `docs/SCREEN_FLOW.md`
- `docs/DATA_MODEL.md`
- `docs/governance/04_DEFINITION_OF_DONE.md`
- `docs/governance/module-acceptance/M01-LOCALIZATION-BASELINE-001.md`
- `app/src/androidTest/java/com/example/SettingsNavigationRegressionTest.kt`
- `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt`
- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/ui/screens/WelcomeScreen.kt`
- `app/src/main/java/com/example/data/SettingsDataStore.kt`

## current_evidence

- Current branch: `harry/phase-1-localization-baseline`.
- Current checkpoint commit: `d9009ceccf6596a48475103d9368dce1c07e73f8`.
- Manager verification passed:
  - `.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon`
  - `.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon`
  - `.\gradlew.bat :app:assembleDebugAndroidTest --stacktrace --console=plain --no-daemon`
- Manager verification failed:
  - `.\gradlew.bat :app:connectedDebugAndroidTest --stacktrace --console=plain --no-daemon`
- Failing test:
  - `com.example.SettingsNavigationRegressionTest.settingsScreenOpensWithLanguageSelector`
- Failure:
  - `ComposeTimeoutException: Condition still not satisfied after 7000 ms`
- Current dirty tree includes a post-commit change to `ExampleInstrumentedTest.kt` so it expects `com.aistudio.shreeshyamstore.pqwzkb` instead of `com.example`.

## scope_paths

Allowed:

- `app/src/androidTest/java/com/example/SettingsNavigationRegressionTest.kt`
- `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt`
- `app/src/main/java/com/example/MainActivity.kt` only if the connected failure proves locale/context wiring still breaks UI ownership or launch behavior.
- `app/src/main/java/com/example/ui/screens/WelcomeScreen.kt` only if startup routing state is the root cause.
- `app/src/main/java/com/example/data/SettingsDataStore.kt` only if DataStore seeding/default language handling is the root cause.
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-hi/strings.xml`
- `docs/governance/module-acceptance/M01-LOCALIZATION-BASELINE-001.md`

Do not touch:

- Billing, payment, product, stock, udhaar, reports, invoice, backup, or release-signing implementation.
- Room entities, DAOs, database version, migrations, or repository logic.
- Gradle version upgrades or dependency changes unless the manager explicitly approves them.
- Package rename or final release branding.
- `.env` or secret files.

## constraints

- Do not redo Phase 1 localization.
- English remains the default language.
- Hindi remains a proper second language through `values-hi`.
- Keep UI text in string resources when touching UI copy.
- Do not add sleeps as the main correctness mechanism unless there is no better Compose/Activity test synchronization option.
- Preserve unrelated dirty worktree changes; do not revert files you did not intentionally modify.

## likely_root_causes_to_investigate

- `SettingsNavigationRegressionTest` may be waiting for `Settings` before startup routing has deterministically reached Home.
- `WelcomeScreen` starts at `Screen.Welcome` and delayed routing depends on DataStore-backed settings; the test may read initial defaults before seeded DataStore values are emitted.
- The stale scaffold assertion in `ExampleInstrumentedTest.kt` must not expect namespace `com.example` when the runtime application id is `com.aistudio.shreeshyamstore.pqwzkb`.

## acceptance_criteria

- Connected Android tests pass reliably on the available emulator.
- `SettingsNavigationRegressionTest` proves Settings opens and the language selector is visible.
- `ExampleInstrumentedTest` is either corrected to the real application id or replaced with a meaningful app-context assertion.
- No broad localization rewrite is done.
- No unrelated app behavior is changed.
- All four verification commands pass.

## required_evidence

The worker completion response must include:

- Files changed.
- Exact root cause found.
- Explanation of why the Settings regression test is now deterministic.
- Confirmation that Phase 1 localization was not restarted.
- Current git status summary.
- Results for:

```powershell
.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:assembleDebugAndroidTest --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --stacktrace --console=plain --no-daemon
```

- Confirmation that no `.env` or secret files were read.

## review_owner

Delivery manager plus QA.

## manager_review_gate

Block acceptance if:

- Connected tests still fail.
- The fix only hides/removes the Settings regression without equivalent coverage.
- The worker broadens into billing/product/report/udhaar feature work.
- The worker changes Gradle/dependencies/package/release settings without approval.
- Dirty worktree changes are silently mixed into a claimed checkpoint without listing them.

## expected_next_task_after_acceptance

After this remediation is accepted and checkpointed, the next module-aligned task can be Phase 2 / M06 Billing Production Hardening.
