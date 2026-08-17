## Summary

Describe the user or engineering outcome of this change.

## Scope

- [ ] This pull request is limited to the stated change.
- [ ] Unrelated refactors are excluded or separately justified.

## Verification

- [ ] `./gradlew assembleDebug`
- [ ] `./gradlew lintDebug`
- [ ] `./gradlew testDebugUnitTest`
- [ ] Relevant focused tests were added or updated.

## Data and security review

- [ ] No credentials, tokens, keystores, `.env` files, customer data, or production exports are committed.
- [ ] Authentication, authorization, cloud-sync, backup, and restore behavior was reviewed if affected.
- [ ] Local-only identity and credential data remains local and is not included in cloud payloads.
- [ ] Destructive operations are guarded and recoverable.

## Database and release impact

- [ ] No schema change.
- [ ] Schema change includes a migration and migration test.
- [ ] Release/signing/configuration impact is documented.
- [ ] Rollback or recovery notes are included when the change can affect stored data.

## Notes for reviewers

Add screenshots, test evidence, known limitations, and follow-up work where relevant.
