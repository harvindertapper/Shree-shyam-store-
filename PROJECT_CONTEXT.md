# Project Context

Status: Subordinate context only. This file summarizes current repo findings for future agents and must not override `AGENTS.md`, `docs/PRODUCT_SPEC.md`, or `docs/IMPLEMENTATION_PLAN.md`.

## Repo Snapshot

- App: Shree Shyam Store.
- Platform: Android.
- Language: Kotlin.
- Product type: customized kiryana/general-store shop-management app.
- Scope correction: use only Shree Shyam Store kiryana-shop and native Kotlin Android assumptions.
- UI: Jetpack Compose Material 3.
- Navigation: manual screen state through `Screen` sealed class and `ShopViewModel`; Navigation Compose dependency is present but no typed navigation graph was found.
- Local database: Room, database name `shree_shyam_store_db`, schema version 2.
- Preferences/session: DataStore preferences named `store_settings`.
- Minimum SDK: 24.
- Target SDK: 36.
- Compile SDK: Android 36.1 style Gradle declaration.
- Application ID and namespace: `com.harrylabs.shreeshyamstore`.
- Kotlin source and test package root: `com.harrylabs.shreeshyamstore`.

## Current Functional Areas

- Welcome/startup flow.
- Login/register.
- First-launch setup.
- Home dashboard.
- Products, categories, opening stock, stock adjustment.
- Billing cart, payment, bill success.
- Cash, manual UPI, and udhaar payment modes.
- Customer udhaar ledger and payment entry.
- Reports.
- Settings with language switch and static Paytm QR image URI.

## Current Tests

- Basic JUnit arithmetic sample.
- Robolectric app-name test.
- Robolectric `MainActivity` launch test.
- Localization/DataStore baseline tests.
- Roborazzi screenshot sample for a simple text composable.
- Instrumented package-name test.

## Important Risks Found

- `app/src/main/java/com/harrylabs/shreeshyamstore/data/AppDatabase.kt` uses the approved v1-only `fallbackToDestructiveMigrationFrom(true, 1)` reset. Room v2 onward requires intentional migrations or a new explicit owner-approved reset.
- `app/src/main/java/com/harrylabs/shreeshyamstore/viewmodel/ShopViewModel.kt` stores local passwords as basic SHA-256 hashes.
- Several screens still contain hardcoded English/Hindi toasts and UI strings.
- `metadata.json`, `.env.example`, Gradle dependencies, and related build scaffolding still reference AI Studio/Gemini concepts; README has been corrected to Shree Shyam Store scope.
- Retrofit, OkHttp, Moshi, a Firebase BoM entry, and the secrets plugin are present, but Firebase Auth, Google Sign-In, Firestore, cloud restore, rules deployment, and App Check are not implemented.
- `android:allowBackup` is enabled and should be reviewed before release.

## Best Next Step

Complete Firebase project/config prerequisites, then implement `FR-G` using Android Credential Manager Sign in with Google and the accepted owner/shop/membership architecture. Product UI, sync/restore, and weighted billing follow the approved sequence in `docs/IMPLEMENTATION_PLAN.md`.

The welcome sound issue remains tracked in `docs/governance/MANUAL_QA_BACKLOG.md`; it is not part of the Firebase foundation route.
