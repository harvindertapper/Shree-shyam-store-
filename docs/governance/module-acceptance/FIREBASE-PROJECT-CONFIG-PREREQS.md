# Firebase Project/Config Prerequisites Acceptance

## Goal

- Register the final Android identity with Firebase and integrate the public Android client configuration needed for upcoming Firebase packets.

## Scope Paths

- `gradle/libs.versions.toml`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `app/google-services.json`

## Acceptance Criteria

- Firebase project is `shreeshyamstore` with project number `703624679442`.
- Android app is registered as `com.harrylabs.shreeshyamstore`.
- `app/google-services.json` matches the registered Android app.
- Google Services, Firebase Auth, and Firestore dependencies resolve.
- Debug build, unit tests, and Android test APK compilation succeed.

## Evidence

- Firebase Android app ID: `1:703624679442:android:c36bcf4581bcd1f02a48cb`.
- Fresh local verification produced the debug APK and Android test APK.
- Unit-test results: 19 tests, 0 failures, 0 errors.
- Owner accepted the worker-selected dependency/tooling versions on 2026-06-18.

## Security/Privacy Review

- `google-services.json` is treated as public Android client configuration.
- No service-account private key or client email was present in the config.
- No signing password, service-account key, or production credential may be added to the repo.

## Source-of-Truth Updates

- `AGENTS.md` now routes `FR-K` before `FR-G`, matching `docs/IMPLEMENTATION_PLAN.md` and the FR-G dependency.
- `docs/governance/03_DECISION_LOG.md` records the owner acceptance.

## Result

- Accepted with follow-ups.
- `FR-K-ROOM-UUID-PRIMARY-KEYS` may start.

## Follow-Ups

- Before FR-G acceptance: register required SHA-1/SHA-256 certificates and complete Google Sign-In provider configuration.
- Before Firestore-dependent acceptance: provision the approved Firestore database/region.
- FR-G2 must establish security rules, App Check posture, and cost controls before product sync.
