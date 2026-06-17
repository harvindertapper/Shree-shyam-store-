# FR-G-FIREBASE-AUTH-SHOP-PROFILE

## task_id

FR-G-FIREBASE-AUTH-SHOP-PROFILE

## goal

Add Firebase Auth with Google Sign-In and create/restore owner user, shop, and membership profile.

## scope_paths

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/java/**`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-hi/strings.xml`
- `app/src/test/**`
- `app/src/androidTest/**`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`

## dependencies

- `FR-P-APP-IDENTITY-RENAME`
- Owner-provided Firebase project id, final app registration, SHA-1/SHA-256 keys, Firestore region, `google-services.json` policy, and App Check/cost decisions.

## constraints

- Use final app id `com.harrylabs.shreeshyamstore`.
- Google Sign-In first.
- No phone OTP implementation.
- No service-account keys, signing passwords, or production credentials in repo.
- No product/category/settings sync yet.
- No staff UI.

## acceptance_criteria

- Owner can sign in with Google in a test environment.
- App creates/restores `users/{uid}`, `shops/{shopId}`, and owner membership.
- Session restore after app restart works for the implemented profile slice.
- UI strings are English/Hindi resource-backed.

## required_evidence

- Build and test results.
- Emulator/manual sign-in evidence.
- Secret hygiene confirmation.
- Security checklist update.

## review_owner

Security/governance plus delivery manager.

## do_not_touch

- Product sync, sales sync, stock sync, udhaar sync.
- Billing behavior.
- Staff permissions UI.
- Phone OTP.
- Welcome sound/assets.

