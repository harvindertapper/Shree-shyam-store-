# Definition of Done

Status: Governance source of truth.

## Done for Docs-Only Tasks

- Requested docs are created or updated.
- Existing docs are not overwritten blindly.
- Source-of-truth conflicts are called out.
- File existence/content checks are run.
- Final response lists changed files and states that Gradle tests were not required because app code did not change.

## Done for Code Tasks

- Scope matches the assigned task packet.
- App builds successfully.
- Relevant unit tests pass.
- Instrumented tests compile when Android-facing code changes.
- Emulator launch/manual proof is captured when UI/navigation changes.
- User-facing text is in English and Hindi string resources.
- No unrelated files are modified.
- Security/privacy checklist is reviewed when data, auth, payment, backup, permission, API, migration, or release signing behavior changes.

## Done for Data/Room Tasks

- Migration impact is documented.
- Existing data preservation is tested or explicitly not applicable.
- No new destructive migration behavior is introduced.
- `docs/DATA_MODEL.md` is updated.

## Done for UI Tasks

- Text fits and is localized.
- English and Hindi paths are checked.
- Screenshots or emulator proof are provided when practical.
- No random Hinglish appears in final UI copy.

## Done for Release Tasks

- App id, version, signing, backup policy, and release notes are documented.
- Secrets are kept out of repo.
- Debug and release build steps are repeatable.
- Owner approval is recorded for release-sensitive changes.
