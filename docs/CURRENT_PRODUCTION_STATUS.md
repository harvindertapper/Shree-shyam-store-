# Shree Shyam Store — Current Production Status

**Status date:** 20 August 2026  
**Repository:** [`harvindertapper/Shree-shyam-store-`](https://github.com/harvindertapper/Shree-shyam-store-)  
**Current main:** `7abf378` — PR #35, restore snapshot envelope and recovery point  
**Product scope:** Merchant Android OS first; SaaS Control Plane/Admin Portal and Pickup Marketplace remain separate future repositories.

## Executive status

The repository is now a **hardened internal/staging candidate**, not yet a production-distributable release. The original prototype risks around credential payload privacy, checkout money representation, stock policy, stable sync identity, tenant/device persistence, repository authorization, authenticated backup, snapshot integrity, and restore recovery have been addressed through the merged hardening sequence through PR #35.

The remaining production blockers are concentrated in five areas: billing architecture decomposition, temporary credential-compatibility cleanup, operational sync/conflict support, reproducible release engineering, and staging/recovery evidence. The core local credential KDF migration and login throttling are already implemented and covered by the current security tests. Server-authoritative tenant operations belong to the future private Control Plane repository and must not be implemented by making the Android client its own authority.

> **Current source-of-truth rule:** The checked-out source, current `main`, the CI workflow, and this document describe the present state. Older audit statements are historical findings and must not be treated as evidence that already-merged controls are still absent.

## Completed hardening sequence

| Area | Completed work | Evidence |
|---|---|---|
| Commerce foundation | Integer-paise money, checkout/payment lifecycle states, stock underflow policy, Udhaar credit/payment validation, and immutable correction/audit behavior. | PRs #14–#18 and the required commerce regression suite. |
| Identity and local security | Firebase-versus-local authority reconciliation, app-lock hardening, credential privacy boundary, local-login throttling foundation, and migration coverage. | PRs #19, #22, #30; `SecurityUtilsTest`, `CredentialMigrationTest`, and `AppLockPolicyTest`. |
| Persistence and schema | Non-destructive migration policy, Room schema artifacts, and current schema version 9 with explicit v8→v9 tenant/device migration. | PRs #20, #31, #32; `app/schemas/.../9.json` and migration tests. |
| Architecture | Reporting and inventory state extraction, billing cart-state boundary, platform contracts, and tenant-aware domain models. | PRs #24–#26 and #29. |
| Authorization | Repository-boundary tenant authorization for actor, membership, store, device, capability, and stale-command checks. | PR #33; `TenantAuthorizationTest`. |
| Backup and recovery | HTTPS-only trusted-host backup provider, Firebase-authenticated short-lived bearer token, tenant-scoped paths, snapshot envelope, checksum/integrity validation, local recovery point, atomic replacement, and rollback. | PRs #34–#35; `AuthenticatedBackupProviderTest`, `RestoreRecoveryEnvelopeTest`, `RestoreSecurityTest`, `docs/BACKUP_PROVIDER.md`, and `docs/RESTORE_RECOVERY.md`. |
| CI baseline | Debug assembly, lint, Dependency Review, stable unit/Robolectric test selection, Room schema verification, and unit-test report artifact upload. | `.github/workflows/ci.yml`; current main checks are required before merge. |

## Current release-blocker register

| ID | Priority | Blocker | Planned PR or evidence | Current state |
|---|---:|---|---|---|
| APP-29 | P1 | Billing and checkout orchestration remains concentrated in `ShopViewModel`, limiting isolated testing and maintainability. | `refactor/billing-viewmodel`, followed by billing UI migration. | Next implementation slice. |
| SEC-30 | P1 | The core versioned salted PBKDF2 verifier and local-login throttling are implemented; the remaining work is a separately approved sunset of the legacy app-lock blank/default-PIN compatibility window and stale documentation. | `security/credential-compatibility-cleanup` or a release migration decision. | Core migration complete in PR #30; compatibility cleanup remains intentionally gated by recovery evidence. |
| SYNC-36 | P1 | Dead-letter, conflict, cursor, retry, and operator recovery outcomes are not yet exposed through structured redacted status. | `feat/sync-observability-conflicts`. | Planned. |
| SYNC-37 | P0 | Merchant-to-server synchronization contract needs versioned tenant authorization, replay, cursor, tombstone, and conflict semantics. | `feat/sync-contract-compatibility`; server implementation belongs to Control Plane. | Planned. |
| REL-38 | P0 | Release signing, versioning, R8/minification, production configuration separation, and artifact provenance are not yet demonstrated. | `release/build-signing-r8`. | Planned. |
| REL-39 | P0 | Clean-device migration, offline commerce, sync retry/conflict, authenticated backup/restore, recovery point, and rollback rehearsal evidence is missing. | `release/staging-smoke-rehearsal`. | Planned. |
| OPS-40 | P0 | Redacted production telemetry, crash monitoring, sync-health support procedure, incident ownership, and rollback runbook need operational evidence. | `release/operations-readiness`. | Planned. |
| CP-01 | P1 | Server-authoritative organization, membership, device enrollment, audit, backup metadata, and sync conflict operations do not yet exist. | New private Control Plane repository. | Future; after Merchant OS release candidate. |
| MKT-01 | P2 | Pickup Marketplace does not yet exist and must not be built against client-authoritative stock or payment state. | New consumer repository after CP-01 APIs stabilize. | Future. |

## Current maintenance lane

The following Dependabot PRs are open and independent from the feature roadmap: Retrofit 3.0.0 (#13), Firebase BOM 34.17.0 (#12), Room runtime 2.8.4 (#11), `actions/upload-artifact` 7 (#10), Play Services Location 21.4.0 (#9), and OkHttp logging-interceptor 5.4.0 (#8). They should be reviewed and merged separately from commerce, credential, sync, and release feature branches.

Room, Firebase, Retrofit, and OkHttp upgrades require particular care because they can affect migrations, authentication, Firestore background sync, the authenticated backup provider, and future Control Plane adapter compatibility. No dependency PR should widen cloud payloads or weaken the current required CI gate.

## Required quality gates

Every feature PR must start from updated `main`, preserve the offline-first repository transaction boundary, add deterministic regression coverage, run `git diff --check`, and pass Android CI and Dependency Review. The stable CI gate currently covers security utilities, credential migration, tenant context and authorization, app lock, restore security and recovery, commerce invariants, catalog and migration tests, payment migration, stable sync identity/outbox, and example unit/Robolectric tests.

A production release additionally requires a reproducible signed release artifact, explicit version code and name, release configuration separation, R8 validation, dependency and secret review, schema/migration evidence, clean-device staging rehearsal, offline commerce evidence, sync retry/conflict evidence, authenticated backup/restore evidence, recovery-point verification, and a documented rollback decision.

## Data and security invariants

The following invariants are non-negotiable for future PRs:

1. Password hashes, PIN verifiers, local credentials, bearer tokens, provider secrets, and raw authentication material remain device-local and never enter Firestore, REST backup, outbox JSON, logs, analytics, or crash reports.
2. All privileged business mutations pass through repository/domain authorization with trusted tenant, actor, membership, device, capability, and freshness context.
3. Money remains integer paise. New commerce logic must not introduce binary floating-point monetary calculations.
4. Checkout remains atomic and duplicate-submit safe. Stock, payment, Udhaar, ledger, audit, and receipt behavior must remain consistent on success and failure.
5. Restore downloads and validates the complete authenticated snapshot before changing local data, preserves device-owned tables, creates a verified local recovery point, and rolls back if replacement fails.
6. Sync uses stable identity, idempotency, retry/dead-letter semantics, tombstone policy, conflict rules, and redacted operator-visible errors.
7. The Android client is not authoritative for server tenant scope, role membership, entitlements, prices, marketplace availability, payment verification, or order acceptance.

## Recommended execution sequence

The next code PR is `APP-29`, the billing/cart ViewModel boundary. A documentation-only status update can merge independently. The core `SEC-30` KDF migration is already complete; only a later compatibility-cleanup decision should be made after app-lock recovery evidence is available. After the billing and credential boundaries are stable, implement `SYNC-36`, then `SYNC-37`, then the release build/signing slice `REL-38`, staging/recovery rehearsal `REL-39`, and operations readiness `OPS-40`. Only after those gates are evidenced should the separate private Control Plane repository begin.

## References

[1]: https://github.com/harvindertapper/Shree-shyam-store-/commits/main "Current main history"

[2]: https://github.com/harvindertapper/Shree-shyam-store-/blob/main/.github/workflows/ci.yml "Required Android CI workflow"

[3]: https://github.com/harvindertapper/Shree-shyam-store-/blob/main/docs/BACKUP_PROVIDER.md "Authenticated backup provider boundary"

[4]: https://github.com/harvindertapper/Shree-shyam-store-/blob/main/docs/RESTORE_RECOVERY.md "Restore snapshot and recovery boundary"

[5]: https://github.com/harvindertapper/Shree-shyam-store-/pulls "Open pull requests"
