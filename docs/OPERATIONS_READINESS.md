# Operations and Support Readiness

**Status:** Operations-readiness contract for the Merchant Android OS. This slice defines safe event and incident shapes and the operator runbooks that a later release-control or Control Plane system may consume. It does not add a telemetry backend, crash SDK, or cloud export path.

## Operating principle

The Merchant OS must remain supportable without turning diagnostics into a second customer-data export channel. Operational signals use stable categories, bounded values, opaque event IDs, and runbook references. They do not contain raw stack traces, payload JSON, customer names or phones, passwords, PIN verifiers, bearer tokens, or full payment details.

> Diagnostic usefulness comes from a stable category and a safe next action, not from copying sensitive application state into logs.

`OperationsEvent` and `OperationsIncident` in `utils/OperationsReadiness.kt` are local policy models. `OperationsTelemetryPolicy` validates them and produces a bounded summary for a future approved sink. The policy is intentionally not an uploader and must not be bypassed by a UI or background worker.

## Event taxonomy

| Category | Severity examples | First runbook |
|---|---|---|
| `APP_START` | `INFO` | Verify artifact version, build environment, and device/API record. |
| `AUTH_FAILURE` or `SECURITY_LOCKOUT` | `WARNING` or `ERROR` | `RUNBOOK_AUTH_LOCKOUT`. |
| `MIGRATION_FAILURE` | `CRITICAL` | `RUNBOOK_MIGRATION_FAILURE`; preserve the device and do not use destructive fallback. |
| `CHECKOUT_FAILURE` | `ERROR` | Checkout and ledger invariants; verify no partial sale, stock, or Udhaar writes. |
| `SYNC_HEALTH` or `SYNC_FAILURE` | `WARNING`, `ERROR`, or `CRITICAL` | `RUNBOOK_SYNC_HEALTH`; use redacted counts and typed categories. |
| `BACKUP_FAILURE` or `RESTORE_FAILURE` | `ERROR` or `CRITICAL` | `RUNBOOK_BACKUP_RESTORE`; preserve local state and reject malformed or wrong-tenant snapshots. |
| `RECOVERY_ROLLBACK` | `CRITICAL` | `RUNBOOK_BACKUP_RESTORE`; retain the pre-restore recovery point and investigate forward correction. |
| `RELEASE_GUARD` | `ERROR` or `CRITICAL` | `RUNBOOK_RELEASE_ROLLBACK`; verify artifact, signing, environment, and rollback compatibility. |

Allowed attributes are intentionally narrow: sync health counts/categories, attempt counts, retry timestamps, operation/stage names, API level, package name, source commit, and rollback decision. Attribute keys and values are bounded, and sensitive fragments are rejected. Correlation and incident IDs accept only safe opaque characters and are length-limited.

## Incident ownership and lifecycle

Every incident has an owner role, a stable category, a severity, a known runbook reference, an opening timestamp, a last event ID, and a status. The roles are deliberately coarse so the event does not carry personal contact details or user identity data.

| Owner role | Responsibility |
|---|---|
| `MERCHANT_OPERATOR` | Confirm the local store state, stop unsafe retries, preserve evidence, and follow sync/restore instructions. |
| `RELEASE_OWNER` | Decide whether an artifact is held, rolled back, or replaced by a forward-compatible build. |
| `SECURITY_OWNER` | Handle authentication, app-lock, credential-boundary, or suspected secret-exposure incidents. |
| `PLATFORM_OWNER` | Handle sync contract, tenant authorization, provider availability, and future Control Plane incidents. |

An incident moves from `OPEN` to `MITIGATED` only after the immediate risk is contained and the local data/recovery position is known. It moves to `CLOSED` only after the owner records the corrective release or operational action. Closing an incident must not delete evidence or recovery points.

## Runbook routing

`RUNBOOK_AUTH_LOCKOUT` covers local/Firebase authority separation, repeated lockout, and account recovery without importing credentials from cloud data. `RUNBOOK_MIGRATION_FAILURE` covers clean-device preservation, schema inspection, and forward recovery. `RUNBOOK_SYNC_HEALTH` covers redacted health categories, retryable outbox work, dead letters, conflicts, and monotonic cursor behavior. `RUNBOOK_BACKUP_RESTORE` covers authenticated provider failures, snapshot validation, recovery-point creation, rollback, and preservation of device-owned identity. `RUNBOOK_RELEASE_ROLLBACK` covers artifact compatibility, signing/configuration failures, staging hold, and last-known-good rollout decisions.

These references map to the existing repository contracts:

| Area | Authoritative repository document |
|---|---|
| Backup provider | [`BACKUP_PROVIDER.md`](BACKUP_PROVIDER.md) |
| Restore and rollback | [`RESTORE_RECOVERY.md`](RESTORE_RECOVERY.md) |
| Sync outbox and conflicts | [`STABLE_SYNC_OUTBOX.md`](STABLE_SYNC_OUTBOX.md) and [`SYNC_OBSERVABILITY.md`](SYNC_OBSERVABILITY.md) |
| Sync compatibility | [`SYNC_CONTRACT_V1.md`](SYNC_CONTRACT_V1.md) |
| Release build and signing | [`RELEASE_BUILD_SIGNING.md`](RELEASE_BUILD_SIGNING.md) |
| Staging rehearsal | [`STAGING_SMOKE_REHEARSAL.md`](STAGING_SMOKE_REHEARSAL.md) |

## Release and rollback criteria

A release must be held when migration is destructive, debug and production environments are mixed, signing inputs are incomplete, an artifact checksum cannot be reproduced, a credential boundary is violated, an outbox conflict silently overwrites financial history, a cursor advances after a failed pull, or restore changes local data before snapshot validation and recovery-point creation.

A rollback to the last known-good artifact is appropriate for a UI or logic regression when the database and cloud contracts remain compatible. A cloud-contract, tenant-authorization, or schema issue requires an owner-approved forward correction or a validated recovery procedure; blindly downgrading can make the local database or server contract unsafe. The decision must include the artifact version, source commit, checksum, environment, affected API levels, data impact, and mitigation, all redacted of personal data.

## Evidence and monitoring handoff

The current CI gate proves compilation, lint, stable tests, migration/schema checks, dependency review, and release/staging evidence-model validation. It does not prove that a crash-monitoring or telemetry backend is configured. Before production distribution, the release owner must choose an approved sink, configure retention/access policy, verify that raw logs are not uploaded, and rehearse incident retrieval using synthetic staging data.

A future private Control Plane may consume the redacted summary as a versioned event or status envelope. It must not import Room entities, accept client-provided authorization as authoritative, or request raw customer/credential payloads. Event retention, rate limiting, tenant isolation, audit history, and operator access belong to the server contract.

## Current limitations

This repository contains the policy, tests, and routing documentation, but no real crash-reporting connector and no live incident-management integration. The owner-operated staging rehearsal must still verify the selected monitoring sink, provider failure alerts, backup/restore response, and rollback decision on a non-production tenant before calling the release operationally ready.

## References

[1]: BACKUP_PROVIDER.md "Authenticated backup provider boundary"
[2]: RESTORE_RECOVERY.md "Restore snapshot and recovery contract"
[3]: STABLE_SYNC_OUTBOX.md "Stable outbox, retry, and conflict policy"
[4]: SYNC_OBSERVABILITY.md "Redacted sync health and dead-letter recovery"
[5]: SYNC_CONTRACT_V1.md "Versioned sync contract boundary"
[6]: RELEASE_BUILD_SIGNING.md "Release build and signing contract"
[7]: STAGING_SMOKE_REHEARSAL.md "Staging migration, smoke, and recovery rehearsal"
