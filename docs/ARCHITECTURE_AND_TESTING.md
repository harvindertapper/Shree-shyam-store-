# Architecture and Testing Context

## Current architecture

The app is an offline-first Android application. Compose screens render state exposed by `ShopViewModel`; the ViewModel currently coordinates navigation, local settings, authentication flows, catalog, cart/billing, udhaar, reports, manual cloud backup/restore, and sync status. Room is the local source of truth for business tables, while DataStore Preferences stores settings and session metadata. WorkManager schedules background synchronization when connectivity is available. Firebase Auth/Firestore support identity and background sync, while a separate OkHttp/Moshi REST helper remains for manual JSON backup/restore.

The current code can be evolved incrementally. Do not start with a broad rewrite. Extract domain use cases at the seams where business invariants are currently embedded in UI orchestration, beginning with checkout/inventory, ledger, identity/session, and cloud restore/sync. Keep Room and Compose behavior stable while tests establish the intended contract.

```text
Compose screens
      |
      v
ShopViewModel  ---- SettingsDataStore (settings/session)
      |
      v
Use cases / domain rules       [target boundary]
      |
      +---- ShopRepository ---- Room AppDatabase/DAOs/entities
      |
      +---- Sync/restore adapters ---- Firestore / authenticated backup service
      |
      +---- WorkManager ---- retry/periodic background execution
```

## Target boundaries

Presentation code should collect state and dispatch user intent; it should not calculate authoritative totals, decide authorization, mutate multiple tables, or serialize cloud payloads. Use cases should own business decisions such as checkout validation, stock policy, ledger entries, restore validation, and retry/idempotency behavior. Repositories should expose persistence operations with clear transaction semantics. External adapters should translate domain records to authenticated provider payloads without leaking local credentials or provider details into the UI.

The target data model should use stable global record identity for multi-device sync, explicit version or conflict metadata, tombstones, and a documented server/client clock policy. New money fields should use integer minor units or a documented decimal type. New state fields should use enums/value objects at the domain boundary, with compatibility-safe serialization at the persistence and cloud edges.

## Environment contract

Local development requires Java 21, Android SDK platform/build tools 36, and the committed Gradle wrapper 9.7.0. The app's secrets plugin reads `.env` and falls back to `.env.example`; `.env` is local-only and must never be committed. Firebase URL/configuration and any future service credentials must be supplied by the appropriate local, staging, or production environment. No production credential, customer export, or private Firebase rules file belongs in this repository.

The current package identity is prototype-like (`com.example` namespace and AI-Studio application ID). A production identity, signing key ownership, versioning scheme, Firebase project, privacy disclosures, and release channel must be decided before public distribution. Until then, only debug artifacts should be treated as development outputs.

## Test layers

| Layer | Scope | Examples to add or maintain |
| --- | --- | --- |
| Domain/unit | Pure business invariants and value conversions | Money rounding, payment modes, stock policy, udhaar balance, validation |
| Repository/Room | Transactions, migrations, persistence queries, restore boundaries | Checkout atomicity, migration chain, device-owned data preservation, indexes |
| Adapter/integration | Cloud serialization, retries, auth boundaries, provider failures | No credential payloads, idempotency, cursor retry, malformed snapshot rejection |
| ViewModel/state | Intent-to-state behavior and error handling | Login/session transitions, sync messages, restore failure state |
| Compose/UI | Critical user journeys and accessibility smoke paths | Browse, billing, udhaar, settings, error/retry states |
| Release/smoke | Built artifact and operational behavior | Debug/release build, install/launch, migration/restore rehearsal, rollback |

Keep tests deterministic and independent of live Firebase data. Use local fixtures and in-memory or isolated Room databases. Add a test whenever a bug reveals an invariant; do not rely on screenshot coverage as proof of data correctness.

## Stable CI gate

The stable headless CI selection runs `assembleDebug`, `lintDebug`, and selected unit/Robolectric tests, including `SecurityUtilsTest`, `RestoreSecurityTest`, `ExampleUnitTest`, and `ExampleRobolectricTest`. The native graphics screenshot test remains a developer-side check because it can hang on headless runners. As the app becomes production-ready, add migration tests, domain tests, authenticated adapter tests, dependency/secret checks, and release smoke tests before widening branch protection.
