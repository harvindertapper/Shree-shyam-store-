# FR-P-APP-IDENTITY-RENAME

## task_id

FR-P-APP-IDENTITY-RENAME

## goal

Rename Android app identity to the final Firebase/Play Store identity before Firebase setup.

## scope_paths

- `app/build.gradle.kts`
- `settings.gradle.kts` only if needed.
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/**`
- `app/src/test/java/**`
- `app/src/androidTest/java/**`
- `AGENTS.md`
- `docs/**`

## dependencies

- `FR-B-ROOM-V2-RESET`

## constraints

- Final `applicationId` is `com.harrylabs.shreeshyamstore`.
- Final namespace is `com.harrylabs.shreeshyamstore`.
- Kotlin source packages must be renamed from `com.example` to `com.harrylabs.shreeshyamstore`.
- Do not configure Firebase in this packet.

## acceptance_criteria

- Gradle application id and namespace use final identity.
- Kotlin packages/imports/tests compile after rename.
- Instrumented package assertion expects final identity.
- Launch docs use final package/activity.

## required_evidence

- Full changed file list.
- Build, unit test, androidTest build, and connected test results if emulator is available.
- Confirmation Firebase config was not added.

## review_owner

Delivery manager plus QA.

## do_not_touch

- Firebase console/config.
- Product/Billing feature behavior.
- Room schema beyond package/import compatibility.
- Secrets, signing credentials, welcome sound/assets.

