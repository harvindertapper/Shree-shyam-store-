# M01-LOCALIZATION-BASELINE-001 Review Checklist

> **Historical/Superseded - do not execute.** Retained as pre-FR-P review evidence. Old package/path references below are historical; current code uses `com.harrylabs.shreeshyamstore`.

Use this checklist when reviewing a worker completion for `M01-LOCALIZATION-BASELINE-001`.

## Required Inputs

- Worker completion response.
- `docs/governance/task-packets/M01-LOCALIZATION-BASELINE-001.md`.
- Current `git status --short`.
- Current diff for allowed files.
- Build and unit-test command outputs.

## Scope Check

- [ ] Changed files are limited to the task packet's allowed scope.
- [ ] No billing, payment, stock, udhaar, reports, Room, auth, Gradle, package id, signing, API, Bluetooth, or cloud-sync files were changed for this task.
- [ ] Existing unrelated dirty-worktree changes were not reverted or claimed as worker scope.

## Localization Evidence

- [ ] English and Hindi resource key sets match exactly.
- [ ] `app/src/main/res/values-hi/strings.xml` has no mojibake markers or Unicode replacement characters.
- [ ] Hindi app name is `श्री श्याम स्टोर`.
- [ ] `LocalizationBaselineTest.hindiResourcesProvideHindiAppName` expects `श्री श्याम स्टोर`.
- [ ] English remains the default resource path.
- [ ] No final UI copy is converted into Hinglish during this task.

## Verification Evidence

- [ ] Worker ran:

```powershell
.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon
```

- [ ] Worker ran:

```powershell
.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon
```

- [ ] Any skipped or failed command has a concrete environmental reason and enough output to reproduce.

## Security And Privacy Check

- [ ] Worker confirms `.env` and secret files were not read.
- [ ] No secrets, customer data, payment claims, or credential behavior changed.

## Review Decision

Accept only if all scope, localization, and verification evidence is present.

Block if:

- Build or unit-test evidence is missing.
- The test expects mojibake text.
- Key parity is not proven.
- Unrelated files were changed or reverted.
- Hindi resources contain corrupted text or Hinglish final UI copy.

## Next Task If Accepted

Recommend a focused UI-copy cleanup slice for login/setup/home screens, keeping English and Hindi string resources in sync.
