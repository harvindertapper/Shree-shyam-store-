# Authenticated Backup Provider

## Purpose

PR #34 replaces the manual REST backup and restore path with an authenticated, tenant-scoped provider boundary. The Android Merchant OS remains offline-first: local Room data is the operational source of truth, while manual cloud backup and restore are explicitly authenticated network operations. Background Firestore synchronization is a separate flow and is not widened by this change.

The boundary is implemented in `AuthenticatedBackupProvider.kt` [1]. `ShopViewModel.syncAllToCloud()` and `ShopViewModel.restoreAllFromCloud()` obtain an `AuthenticatedBackupTableClient` only after reconciling the current identity session, confirming an active Firebase user, and obtaining a short-lived Firebase ID token.

## Authentication and identity binding

A backup request requires a `BackupAuthContext` containing the Firebase identity UID, the persisted tenant scope, and a short-lived bearer token. Local offline sessions cannot construct this context: `BackupAuthContext.fromFirebaseSession()` rejects them with `BackupUnauthorizedException` before a transport is called.

The token is obtained from the current Firebase user at operation time. The provider does not persist the token, include it in JSON table payloads, or log it. Each HTTP request sends the token only in the `Authorization: Bearer ...` header and repeats the resolved identity and tenant fields in dedicated headers for server-side authorization.

| Header | Meaning | Source |
| --- | --- | --- |
| `Authorization` | Short-lived Firebase ID token | Current Firebase user |
| `X-Backup-Identity` | Authenticated identity UID | Reconciled Firebase session |
| `X-Backup-Organization` | Organization scope | Persisted tenant/device context |
| `X-Backup-Store` | Store scope | Persisted tenant/device context |
| `X-Backup-Membership` | Membership scope | Persisted tenant/device context |
| `X-Backup-Device` | Device binding | Persisted tenant/device context |
| `X-Backup-Installation` | App-installation binding | Persisted tenant/device context |

The provider rejects a response carrying an `x-backup-tenant` value that does not match the authenticated store or organization. This check occurs before the response can be returned to restore orchestration.

## HTTPS and trusted-host policy

The provider constructor accepts a base URL and an explicit trusted-host set. The URL must have an HTTPS scheme and a non-empty host. The normalized host must appear in the trusted set; otherwise construction fails with `BackupIncompatibleException`. In the Android factory, the trusted set is anchored to the build-time `FIREBASE_URL` host; a user-editable settings URL cannot add an arbitrary host to the bearer-token routing policy. The default OkHttp transport is therefore never created around an untrusted or plain-HTTP endpoint.

The provider constructs table paths below a tenant-scoped prefix:

```text
<configured-prefix>/scopes/<sha256(organizationId/storeId/membershipId)>/<table>.json
```

The tenant scope is hashed for path isolation, while the request headers retain the explicit authorization context required by the future server-side boundary. Device and installation identifiers are not used as a substitute for organization, store, or membership authorization.

## Cloud data contract

The provider delegates table authorization to `CloudSyncPolicy` [2]. The allowlist remains intentionally narrow and contains only these business tables:

| Allowlisted table | Business purpose |
| --- | --- |
| `categories` | Product categories |
| `products` | Catalog and inventory records |
| `sales` | Sale headers |
| `sale_items` | Sale line items |
| `customers` | Customer records |
| `udhaar_transactions` | Credit-ledger transactions |
| `stock_adjustments` | Inventory adjustment history |

Credential and device-owned tables are excluded. In particular, `users`, passwords, password hashes, PIN verifiers, app-lock secrets, session secrets, and authentication tokens are not valid cloud business tables. The provider rejects a request for a non-allowlisted table before transport execution.

## Upload and restore behavior

Uploads use typed Moshi serialization through `AuthenticatedBackupTableClient.uploadTable()`. A successful response may return an `x-backup-revision` value, which is surfaced to the caller for future snapshot and conditional-write workflows. The current provider sends each allowlisted table through the authenticated boundary and does not mark local records as cloud-confirmed merely because a local operation was started.

Restore downloads every allowlisted table before calling `ShopRepository.replaceCloudRestorableTables()`. If authorization, tenant validation, transport availability, status validation, malformed JSON, or replay detection fails, the repository replacement is not reached. The repository replacement remains the single transaction boundary that preserves device-owned users and the local shop profile [3].

An empty or `null` response for a table is treated as malformed rather than as an instruction to clear local data. A restore whose downloaded business tables are all empty is rejected by `ShopViewModel`, preserving the existing no-destructive-empty-restore rule.

The provider accepts an expected revision on download. If the server returns the same revision as the caller's expected revision, the operation fails with `BackupReplayException`. HTTP `409` is also mapped to the replay error class, allowing a future snapshot envelope or server-side replay policy to reuse the same typed boundary.

## Typed failure model

The UI receives stable, non-sensitive messages from `ShopViewModel.backupFailureMessage()`. Raw response bodies, bearer tokens, and internal stack traces are not surfaced to the user.

| Failure | Provider exception | User-facing behavior |
| --- | --- | --- |
| HTTP 401/403 or local session | `BackupUnauthorizedException` | Ask the user to sign in again |
| Mismatched tenant response | `BackupWrongTenantException` | Reject the backup as belonging to another store |
| Timeout, network error, HTTP 408/429/5xx | `BackupUnavailableException` | Keep local data unchanged and allow retry |
| Empty, `null`, or invalid JSON | `BackupMalformedException` | Reject the restore without replacement |
| Invalid URL, untrusted host, unsupported status | `BackupIncompatibleException` | Reject configuration or format |
| Repeated revision or HTTP 409 | `BackupReplayException` | Reject the repeated snapshot |

## Test coverage

`AuthenticatedBackupProviderTest` uses an injectable `BackupTransport`; no real network or credential is required. The required CI gate covers unauthorized responses, wrong-tenant headers, timeout and server failures, malformed JSON, replayed revisions, successful authenticated upload/download, untrusted hosts, non-HTTPS URLs, credential-table exclusion, and rejection of local sessions.

## Control Plane handoff

The current provider is a deliberately small transitional REST boundary. A future SaaS Control Plane can implement the same contract behind a private authenticated API without changing the Merchant OS repository or Room model. The handoff should preserve the following properties:

1. The server must derive authorization from the verified bearer token and enforce organization, store, membership, device, and installation scope. Client-supplied scope headers are context signals, not authority.
2. The server must validate the allowlisted table name and reject credential or device-owned data at the API boundary.
3. The server should return a versioned snapshot envelope containing schema version, tenant identity, creation time, table counts, checksum, and completeness marker. That envelope is the next restore-recovery slice and is intentionally not fabricated by PR #34.
4. The server should issue an opaque revision or snapshot identifier and support conditional writes and replay detection.
5. The API should provide audit records for backup, restore, authorization denial, tenant mismatch, and recovery events without storing raw credentials or sensitive request bodies in logs.

This keeps the Merchant OS, the future Control Plane, and the future pickup-first marketplace aligned around explicit tenant-aware platform contracts rather than ad-hoc Firebase paths [4].

## Rollback and operational notes

If the provider integration must be rolled back, revert the focused PR rather than widening `CloudSyncPolicy` or re-enabling unauthenticated manual REST access. Background Firestore synchronization remains independently deployable. Any production rollout should verify authenticated backup, wrong-store rejection, malformed snapshot rejection, and preservation of local users and shop profile data on a staging store before enabling manual restore for operators.

## References

[1]: ../app/src/main/java/com/aistudio/shreeshyamstore/pqwzkb/utils/AuthenticatedBackupProvider.kt "Authenticated backup provider implementation"
[2]: ../app/src/main/java/com/aistudio/shreeshyamstore/pqwzkb/utils/CloudSyncPolicy.kt "Cloud business-table allowlist"
[3]: ../app/src/main/java/com/aistudio/shreeshyamstore/pqwzkb/data/ShopRepository.kt "Transactional cloud-restorable table replacement"
[4]: ../app/src/main/java/com/aistudio/shreeshyamstore/pqwzkb/commerce/PlatformContracts.kt "Shared tenant and platform contracts"
