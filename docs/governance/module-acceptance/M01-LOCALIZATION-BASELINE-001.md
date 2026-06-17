# M01-LOCALIZATION-BASELINE-001 Acceptance

## Decision

PASS.

## Audit Date

2026-06-17

## Claimed Checkpoint

- Branch: `harry/phase-1-localization-baseline`
- Commit: `d9009ceccf6596a48475103d9368dce1c07e73f8`
- Commit message: `feat: stabilize phase 1 localization baseline`

## Manager Finding

Phase 1 should not be restarted from scratch. The localization baseline work is accepted after connected Android test remediation.

The previous unit-test blocker is cleared. The later connected-test blocker is also cleared by `M01-LOCALIZATION-BASELINE-001-CONNECTED-REMEDIATION`, which made the Settings navigation regression test wait for seeded DataStore-backed Welcome state before using the real Welcome continue path.

## Evidence Reviewed

- User completion report for Phase 1 Baseline Stabilization.
- Commit contents for `d9009ceccf6596a48475103d9368dce1c07e73f8`.
- Current branch: `harry/phase-1-localization-baseline`.
- Current `HEAD`: `d9009ceccf6596a48475103d9368dce1c07e73f8`.
- Current worktree status.
- `app/src/androidTest/java/com/example/SettingsNavigationRegressionTest.kt`.
- `app/src/main/java/com/example/MainActivity.kt`.
- `app/src/main/java/com/example/data/SettingsDataStore.kt`.
- `app/src/main/java/com/example/ui/screens/WelcomeScreen.kt`.
- Connected Android test report after remediation:
  - `app/build/reports/androidTests/connected/debug/index.html`
  - `app/build/outputs/androidTest-results/connected/debug/TEST-Pixel_5_API_30(AVD) - 11-_app-.xml`

## Verification Run After Remediation

Passed:

```powershell
.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:assembleDebugAndroidTest --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --stacktrace --console=plain --no-daemon
```

Observed result:

- Debug build passed.
- Unit/Robolectric tests passed.
- Android test APK compile passed.
- Connected Android tests passed: 2 tests, 0 failures on `Pixel_5_API_30(AVD) - 11`.

Additional focused check:

```powershell
.\gradlew.bat --% :app:connectedDebugAndroidTest --stacktrace --console=plain --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=com.example.SettingsNavigationRegressionTest
```

Focused result:

- `SettingsNavigationRegressionTest`: 1 test, 0 failures.

## Findings By Severity

### High

- None after remediation.

### Medium

- Current worktree still has unrelated/post-checkpoint changes outside the connected remediation. These must be intentionally reviewed before push/PR.
- The exact original checkpoint commit `d9009ceccf6596a48475103d9368dce1c07e73f8` is no longer the full acceptance state by itself; acceptance now also depends on the connected-remediation working-tree changes being checkpointed.

### Low

- Welcome chant remains deferred in `docs/governance/MANUAL_QA_BACKLOG.md` and was not pulled into M01.

## Missing Proof

- No missing M01 test proof remains after the current remediation verification.
- A follow-up commit is still needed to checkpoint the accepted M01 state before PR.

## Required Remediation

- None for M01 acceptance.
- Checkpoint the accepted remediation state before opening a PR.

## Residual Risks

- `android:allowBackup` remains release-sensitive and must be reviewed before release.
- `fallbackToDestructiveMigration()` remains a production blocker before schema changes or release.
- Local password storage remains basic SHA-256 and needs owner-approved hardening before production release.

## Next Recommended Task

Checkpoint the accepted M01 remediation state, then prepare the next packet for Phase 2 / M06 Billing Production Hardening.
