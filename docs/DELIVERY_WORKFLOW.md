# Delivery Workflow

Status: Source of truth for how future agents should accept, execute, verify, and close work in this repo.

## Assignment Rules

Every task should name:

- Task ID from `docs/IMPLEMENTATION_PLAN.md` or a new clearly scoped ID.
- Goal.
- Allowed scope paths.
- Required source-of-truth files to read first.
- Do-not-touch paths.
- Acceptance criteria.
- Required evidence.
- Review owner.

If the user asks broadly, first map the request to the smallest matching module packet. Ask only for decisions that cannot be safely inferred from repo docs.

## Required Baseline Before Code Changes

Read these before implementation:

1. `AGENTS.md`
2. `docs/PRODUCT_SPEC.md`
3. `docs/IMPLEMENTATION_PLAN.md`
4. `docs/DELIVERY_WORKFLOW.md`
5. The module-specific docs or files named by the task
6. Existing tests touching the module

For data, auth, backup, payment, release, or privacy work, also read:

- `docs/DATA_MODEL.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`
- `docs/governance/04_DEFINITION_OF_DONE.md`

## When Code Changes Are Allowed

Code changes are allowed only when:

- The task explicitly requests implementation, or the needed change is a direct fix for the assigned module.
- The agent has checked the current repo state and relevant docs.
- Scope paths are clear.
- Owner-gated decisions are not being guessed.

Docs-only tasks must not modify app code.

## No Silent Scope Expansion

Do not add any of these unless explicitly approved:

- Bluetooth thermal printer support.
- Firebase/cloud sync outside the approved Firebase Cloud Sync Foundation packets.
- Multi-store support implementation.
- Staff roles and permissions.
- Real UPI confirmation.
- New backend/API workflow outside an approved Firebase/cloud task packet.
- New dangerous Android permission.
- New dependency or framework.
- Room schema changes without migration planning.
- Package rename, signing changes, or release secrets.

## Review Process

- Delivery manager reviews scope, acceptance criteria, and evidence.
- QA reviews user workflows, tests, emulator/manual proof, and screenshots for UI work.
- Security/governance reviews auth, secrets, backup/import/export, permissions, payment claims, migrations, and release signing.
- Owner approval is required for product decisions marked `TBD - owner decision required`.

## Acceptance Evidence

Use the lightest evidence that proves the change:

- Docs-only: file existence checks and content spot checks.
- Kotlin/business logic: unit tests.
- Compose/UI: unit/Robolectric tests plus screenshot/manual emulator proof when practical.
- Android-facing changes: `:app:assembleDebugAndroidTest` and connected test when emulator/device is required.
- Data migrations: migration tests and before/after data preservation notes.
- Security/privacy changes: checklist update and explicit residual risk note.

## Standard Verification Commands

For meaningful code changes:

```powershell
.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon
```

For Android/emulator-facing work:

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --stacktrace --console=plain --no-daemon
```

For manual launch QA:

```powershell
.\gradlew.bat :app:installDebug --stacktrace --console=plain --no-daemon
adb shell am start -n com.harrylabs.shreeshyamstore/com.harrylabs.shreeshyamstore.MainActivity
```

## Completion Format

Finish with:

```text
## Summary
- What changed and why.

## Files changed
- Path and purpose.

## Repo findings
- Relevant discovered facts.

## Source-of-truth impact
- Docs consulted or updated, and remaining TBD decisions.

## Security/privacy impact
- Credential, customer data, payment, permission, backup, and migration impact.

## Tests run
- Commands and results, or why not required.

## Remaining owner decisions
- Decisions still blocked on the user.

## Next recommended task
- One module-aligned task prompt.
```
