# Staging Migration, Smoke, and Recovery Rehearsal

**Status:** REL-39 evidence contract and deterministic rehearsal scaffolding. A real-device or emulator run against a non-production tenant remains an owner-operated release gate; this repository does not claim that live staging evidence exists merely because CI is green.

## Objective

The rehearsal proves that a reviewed Merchant OS artifact can be installed on a clean staging device, migrate a prior database through the supported schema chain, continue offline commerce, recover sync retries and conflicts, perform authenticated backup and restore validation, preserve a local recovery point, and make an explicit rollback decision.

> No live customer data, production tenant, production backup, production signing key, or production Firebase endpoint may be used for this rehearsal.

## Required environment record

Before installation, record the following values in the release evidence system or a protected review attachment. Do not put bearer tokens, passwords, PIN verifiers, raw customer exports, or full logs in the record.

| Field | Required value |
|---|---|
| Artifact checksum | SHA-256 of the exact APK under test. |
| Package identity | `com.aistudio.shreeshyamstore.pqwzkb`. |
| App version | Version code and version name from the reviewed artifact. |
| Source commit | Immutable commit or release tag used to build the artifact. |
| Device | Emulator/device model and Android API level, with API level at least 24. |
| Tenant | Dedicated non-production staging tenant and test membership/device enrollment. |
| Data policy | Explicit confirmation that `usesProductionData=false`. |
| Known limitations | Any provider, device, or test-tenant limitation. |
| Rollback decision | Keep, halt, or roll back, with the owner and reason. |

`StagingRehearsalEvidence` validates these fields and requires a unique passing entry for every stage. Its `redactedSummary()` excludes stage notes and evidence references because those may point to protected logs.

## Rehearsal matrix

| Stage | Deterministic CI coverage | Live staging action | Pass condition |
|---|---|---|---|
| Migration | `StableSyncMigrationTest`, `TenantDeviceContextMigrationTest`, and Room schema gate | Install the prior fixture, upgrade through v5→v9, reopen the app, and inspect the migrated store context | No destructive fallback, stable global IDs and tenant/device context preserved, schema opens successfully. |
| Offline commerce | `CheckoutInvariantsTest`, `CommerceValidationTest`, `PaymentStateMigrationTest`, and billing state tests | Disable network, create a product/customer, complete Cash and UPI flows, exercise Udhaar credit limit and valid payment, then close/reopen | Commerce remains usable offline; money remains integer paise; stock, sale, ledger, and validation invariants hold. |
| Sync retry | `SyncOutboxTest` and `SyncContractTest` | With the test provider temporarily unavailable, create an offline mutation, reconnect, and observe retry/backoff | Retryable work remains pending/retryable, cursor does not advance on failed pull, and replay does not duplicate financial events. |
| Sync conflict | `SyncContractTest` and existing sync policy documentation | Use two enrolled staging devices to submit controlled same-record mutations and a tombstone | Conflict is typed and visible, stale mutation cannot overwrite newer state, and financial history is not silently replaced. |
| Authenticated backup | `AuthenticatedBackupProviderTest` and tenant authorization tests | Upload a complete staging snapshot with the release build and inspect only redacted provider metadata | HTTPS/authentication/tenant path checks pass; credentials, device-only identity, and raw secrets are excluded. |
| Restore validation | `RestoreRecoveryEnvelopeTest` and restore security tests | Download the staging snapshot, validate checksum/schema/counts/references, and restore into the rehearsal device | Malformed, wrong-tenant, incomplete, or replayed snapshots are rejected before local replacement. |
| Recovery point | `RestoreRecoveryEnvelopeTest` and `RestoreRecoveryCoordinator` tests | Verify a local pre-restore recovery point exists and is readable before replacement | A failed replacement attempts rollback; the pre-restore state remains recoverable. |
| Rollback decision | Evidence contract and release runbook | Decide whether to keep or roll back the artifact after the complete matrix | Decision is explicit, owner-attributed, and based on observed data/sync/restore results. |

## Suggested execution sequence

Build the artifact from the reviewed release commit and compute its checksum before installation. Start with a clean emulator or device and a dedicated staging tenant. Capture the device/API/tenant record, install the artifact, and run migration before enabling any network-connected flow.

Next disable network connectivity and exercise product creation, Cash checkout, UPI validation, Udhaar credit-limit rejection, valid Udhaar payment, and process restart. Capture only test identifiers and redacted outcome categories. Re-enable connectivity and run the controlled retry and two-device conflict scenarios with synthetic records.

Finally perform authenticated staging backup, validate the downloaded snapshot, create a local recovery point, restore into a clean rehearsal state, and intentionally rehearse one rejected snapshot or rollback path. Preserve the exact artifact checksum and evidence references outside the APK repository. A passing unit suite cannot replace the real-provider and real-device portions of this sequence.

## Release and rollback rules

The rehearsal must stop if migration is destructive, a duplicate bill or ledger event appears, stock underflow is accepted, a sync cursor advances after a failed pull, a conflict silently overwrites financial history, a backup contains credentials, or restore changes local data before validation completes. The owner must choose the last known-good artifact or a forward corrective release based on cloud/schema compatibility; do not blindly downgrade a database or cloud-contract change.

The `StagingRehearsalEvidence` contract is intentionally not a telemetry uploader. It is a local/testable evidence shape that can later be serialized by the separate release-control workflow or SaaS Control Plane without importing Room models or raw customer data.

## Current evidence status

The repository-side evidence contract and deterministic tests are included in CI. The following live facts must still be populated by the release operator after PR #40 and PR #41 are merged and a reviewed release artifact is available: device/API record, staging tenant, exact checksum, migration result, offline commerce result, live retry/conflict result, authenticated backup/restore result, recovery-point result, and final rollback decision.
