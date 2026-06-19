# FR-G-FIREBASE-AUTH-SHOP-PROFILE

## task_id

FR-G-FIREBASE-AUTH-SHOP-PROFILE

## goal

Add Firebase Auth with Android Credential Manager Sign in with Google, create/restore owner user, shop, and membership profile, and deprecate the local Room `users` table in favor of Firebase Auth offline session cache.

## scope_paths

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
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
- `app/src/androidTest/**`
- `docs/DATA_MODEL.md`
- `app/schemas/com.harrylabs.shreeshyamstore.data.AppDatabase/4.json`
- `firestore-rules-tests/**`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`


## dependencies

- `FR-P-APP-IDENTITY-RENAME`
- `FR-K-ROOM-UUID-PRIMARY-KEYS`
- Owner-provided Firebase project id, final app registration, SHA-1/SHA-256 keys, Firestore region, `google-services.json` policy, and App Check/cost decisions.

## constraints

- Use final app id `com.harrylabs.shreeshyamstore`.
- Use Firebase main modules `firebase-auth` and `firebase-firestore`.
- Use Android Credential Manager Sign in with Google first; do not introduce legacy Google Sign-In client APIs.
- The local Room `users` table must be dropped/removed from the schema (incrementing Room database version to 4 with a proper data-preservation migration).
- All registration, login, and session persistence logic must use Firebase Auth (with offline support). Password hashing and local credentials verification must be deleted.
- Formulate and deploy minimum secure bootstrap Firestore rules:
  - Users document `users/{uid}`: Only accessible by the owner user itself.
  - Shops document `shops/{shopId}`: Only writeable by the owner (`request.auth.uid == resource.data.ownerUid`), ownerUid must be immutable, and deletion is deferred/denied.
  - Members subcollection `shops/{shopId}/members/{memberUid}`: Manage membership creation atomically and validate role immutability.
- No phone OTP implementation.
- No service-account keys, signing passwords, or production credentials in repo.
- No product/category/settings sync yet.
- No staff UI.

## acceptance_criteria

- Owner can sign in with Google through Credential Manager in a test environment.
- App creates/restores `users/{uid}` (storing `activeShopId`), `shops/{shopId}`, and owner membership atomically using a Firestore transaction.
- Secure Firestore rules are deployed to the project and verified via automated rules unit tests.
- Session restore after app restart works for the implemented profile slice using Firebase Auth.
- Local `users` table is completely removed from Room schema using Migration(3,4).
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

