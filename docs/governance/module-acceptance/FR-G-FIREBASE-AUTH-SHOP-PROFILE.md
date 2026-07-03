# FR-G Firebase Auth and Shop Profile Acceptance

## Goal

- Add Firebase Auth with Android Credential Manager Sign in with Google.
- Create/restore owner `users/{uid}`, `shops/{shopId}`, and `shops/{shopId}/members/{uid}` profile documents.
- Remove local Room `users` table from the runtime login path and use Firebase Auth as the owner identity source.

## Current Result

- LOCAL PASS / LIVE RULES DEPLOYED / MANUAL QA PENDING.
- The local Android code and Room v5 migration tests pass. Firestore security rules emulator tests still require a compatible local Java/Firebase emulator environment.
- Live Firestore `(default)` exists in project `shreeshyamstore` and tested rules are deployed.
- Final FR-G acceptance is pending manual Google Sign-In/shop-profile QA.

## Scope Paths

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/google-services.json`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/Entities.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/Daos.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/AppDatabase.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/ShopRepository.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/FirebaseOwnerRepository.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/SettingsDataStore.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/viewmodel/ShopViewModel.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/AuthScreens.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/WelcomeScreen.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/SettingsScreen.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/FirstLaunchSetupScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-hi/strings.xml`
- `app/src/test/**`
- `app/schemas/com.harrylabs.shreeshyamstore.data.AppDatabase/4.json`
- `firestore.rules`
- `firestore-rules-tests/**`
- `docs/DATA_MODEL.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`

## Local Evidence

- Debug build:
  - `./gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon`
  - Result: pass on 2026-07-01.
- JVM/unit tests and Android test APK build:
  - `./gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest --stacktrace --console=plain --no-daemon`
  - The shell command timed out, but fresh artifacts were produced.
  - Parsed test XML result on 2026-07-01: 29 tests, 0 failures, 0 errors, 0 skipped.
  - `app-debug-androidTest.apk` was generated on 2026-07-01.
- Firestore rules emulator:
  - `npx -y firebase-tools@latest --config firebase.json --project demo-shreeshyamstore emulators:exec --only firestore "npm --prefix firestore-rules-tests test"`
  - Result: 23 passing on 2026-07-01.
  - Portable JDK 21 was used from `C:/tmp/temurin21-jdk/jdk-21.0.11+10` because current system Java is 19 and Firebase CLI emulator requires Java 21+.
- Firestore live project check:
  - `npx -y firebase-tools@latest firestore:databases:list --project shreeshyamstore`
  - Result: `(default)` exists, `STANDARD`, `FIRESTORE_NATIVE`.
- Firestore live database details:
  - `npx -y firebase-tools@latest firestore:databases:get "(default)" --project shreeshyamstore`
  - Result: location `asia-south1`, edition `STANDARD`, delete protection disabled, PITR disabled.
- Firebase Android app registration:
  - `npx -y firebase-tools@latest apps:list --project shreeshyamstore --json`
  - Result: Android app active for namespace `com.harrylabs.shreeshyamstore` and app id `1:703624679442:android:c36bcf4581bcd1f02a48cb`.
- Source-of-truth docs alignment:
  - `AGENTS.md`, `docs/PRODUCT_SPEC.md`, `docs/SCREEN_FLOW.md`, `docs/IMPLEMENTATION_PLAN.md`, and `docs/DATA_MODEL.md` now describe Firebase Auth/Room v5 as the current FR-G/PD-03 path instead of the deprecated local username/password auth flow.
- Firestore rules deployment:
  - `npx -y firebase-tools@latest deploy --only firestore:rules --project shreeshyamstore`
  - Result: rules compiled and deployed successfully on 2026-07-01.

## Security/Privacy Review

- No service-account key, signing password, token, or production secret was added.
- `app/google-services.json` remains public Android Firebase client configuration for project `shreeshyamstore` and app id `com.harrylabs.shreeshyamstore`.
- Google Sign-In uses Android Credential Manager and the explicit-button `GetSignInWithGoogleOption`; legacy Google Sign-In client APIs are not used.
- First-launch owner phone is required and validated as 10-15 digits to match the Firestore rules.
- Owner phone is enforced as digits-only in both app validation and Firestore rules.
- Firebase display name falls back to `Owner` if Google returns an empty display name, preventing a rules mismatch.
- Firestore rules deny signed-out access, enforce user self-access, enforce shop owner/membership checks, deny shop/member deletion, and block update escalation paths in the bootstrap profile schema.


## Runtime QA Evidence - 2026-07-01

- Emulator used: `Pixel_5_API_30`.
- Install result: `adb install -r app/build/outputs/apk/debug/app-debug.apk` succeeded.
- Fresh launch result after `pm clear`: app opened to Welcome screen.
- Navigation result: Welcome `Continue` routed to Owner Login screen.
- Login UI result: `Sign in with Google` button rendered and was tappable.
- Google Sign-In completion result: blocked by emulator environment, not accepted as app-complete proof.
  - Log evidence: Google Play services out of date for `com.harrylabs.shreeshyamstore`; required `230815045`, found `201817022`.
  - Required next proof: run the same flow on a physical phone or updated Google Play emulator with current Play Services and a Google account.
- Updated emulator follow-up: `Pixel_7_API_35_Play`.
  - Google Play services version observed: `24.23.35 (190800-646585959)`.
  - Fresh app launch reached Owner Login and rendered `Sign in with Google`.
  - The visible Google button now uses Credential Manager `GetSignInWithGoogleOption` for the explicit sign-in flow.
  - Button tap opened Google Play services Credential UI (`com.google.android.gms.identitycredentials.ui.CredentialChooserActivity`) and did not leave the app in an ANR state.
  - Log evidence showed `GetGoogleIdOperation` succeeded and the remote provider returned `CREDENTIALS_RECEIVED`; final owner account selection/authorization remains manual.
  - Required next proof remains: complete sign-in on a physical phone or a stable Google Play emulator with a Google account available and Firebase Authentication Google provider enabled.

## Pending Live Acceptance Gates

- Firestore database creation is complete.
- Firestore rules deployment is complete.
- Delete protection is currently disabled on the live database; enabling it remains recommended before broad sync/release.
- Confirm/enable Firebase Authentication Google provider in the Firebase Console, then perform manual Google Sign-In QA on emulator/phone:
  - first sign-in
  - first shop creation
  - app restart session restore
  - logout
  - clear storage and same-account restore
  - different-account local reset behavior

## Result

- Do not route to product/category/settings cloud sync yet.
- FR-G may be accepted only after manual sign-in/shop-profile QA passes.
