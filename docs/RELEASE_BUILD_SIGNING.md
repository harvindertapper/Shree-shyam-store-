# Release Build, Signing, R8, and Versioning

**Status:** Implemented in REL-38. This document records the reproducible build contract for Merchant Android OS artifacts.

## Version identity

The app module reads `APP_VERSION_CODE` and `APP_VERSION_NAME` from Gradle properties. The repository defaults are `1` and `1.0.0` in `gradle.properties`; a reviewed build may override them with `-PAPP_VERSION_CODE=<positive-integer>` and `-PAPP_VERSION_NAME=<non-empty-name>`. A production artifact must record the exact values together with the source commit or release tag and checksum.

The application ID remains `com.aistudio.shreeshyamstore.pqwzkb`. Version changes must not be used as a substitute for a database migration or a cloud-contract compatibility decision.

Debug cloud scheduling is disabled centrally in `SyncManager`, the background `SyncWorker`, and the ViewModel backup/restore entrypoints. A debug artifact therefore cannot silently enqueue Firestore work or perform authenticated cloud backup/restore. Production release builds are the only profile with `CLOUD_SYNC_ENABLED=true`.

## Build environments

Every build type exposes a generated `BuildConfig.BUILD_ENVIRONMENT` marker. The `debug` build is marked `debug`, sets `CLOUD_SYNC_ENABLED=false`, and uses the CI-generated or developer-local debug keystore. The `release` build is marked `production`, sets `CLOUD_SYNC_ENABLED=true`, enables minification and resource shrinking, and uses the production R8 configuration.

The runtime source currently derives the trusted backup host from the build-time Firebase URL while allowing the operator’s editable URL only within that trusted host policy. REL-38 does not add a new cloud endpoint or widen the `CloudSyncPolicy` allowlist. Debug builds must never be pointed at production data during staging or local verification.

## Signing contract

Release signing is intentionally external to Git. The Gradle module accepts the following environment variables:

| Variable | Meaning |
|---|---|
| `RELEASE_KEYSTORE_PATH` | Absolute or workspace-relative path to the controlled release keystore. |
| `RELEASE_STORE_PASSWORD` | Keystore password supplied by the protected build environment. |
| `RELEASE_KEY_ALIAS` | Alias for the release key. |
| `RELEASE_KEY_PASSWORD` | Key password supplied by the protected build environment. |
| `REQUIRE_RELEASE_SIGNING` | Set to `true` in the controlled release job to fail closed when any signing input is missing. |

The release build may be assembled unsigned in ordinary CI so R8 and packaging remain verifiable without exposing signing secrets. A production release job must set `REQUIRE_RELEASE_SIGNING=true`; the verification task then requires a real keystore file and complete signing inputs. No keystore, password, `.env`, `local.properties`, or generated artifact may be committed.

## R8 and reflection boundary

Release minification and resource shrinking are enabled. The existing restore codec uses Moshi Kotlin reflection for `SnapshotEnvelope`, `CloudRestorableSnapshot`, table counts, and the seven business snapshot entity types. `app/proguard-rules.pro` keeps these serialized models and their members stable so authenticated backup and restore remain schema-compatible after obfuscation.

The keep rules are deliberately scoped to the models used by the restore codec. They do not widen cloud serialization and do not make device-owned users, credential verifiers, app-lock state, or bearer tokens eligible for backup or sync.

## CI evidence

Android CI performs the following release checks for every relevant pull request:

1. It assembles the debug APK with a newly generated debug keystore.
2. It runs `:app:verifyReleaseConfiguration`, which checks positive version identity, production/debug build markers, and enabled release minification.
3. It assembles the minified release output without requiring signing secrets.
4. It rejects tracked `.env`, `local.properties`, keystore, and JKS files.
5. It runs lint and the stable unit/Robolectric test gate, then uploads debug and release outputs as review artifacts.

The controlled release workflow must repeat these checks with protected signing variables, set `REQUIRE_RELEASE_SIGNING=true`, verify the package ID and version, compute a checksum, and retain the evidence with the reviewed commit or tag.

## Rollback and release safety

A signed release is eligible for staging only after data migrations, offline commerce, sync retry/conflict handling, authenticated backup, restore validation, and recovery-point rehearsal are complete. A build-only regression should be rolled back to the last known-good artifact when database and cloud contracts remain compatible. A signing or cloud-contract failure must not be “fixed” by importing secrets, weakening R8, downgrading the database, or silently changing tenant or sync behavior.
