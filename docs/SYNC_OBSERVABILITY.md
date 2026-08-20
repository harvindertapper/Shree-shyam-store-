# Sync Observability and Conflict Status

**Status:** Implemented in SYNC-36. This document describes the merchant-device contract for reporting local sync health without exposing business payloads or identity credentials.

## Purpose and boundary

The Merchant OS is offline-first. A store must be able to distinguish healthy local operation from pending work, temporary retry pressure, or work that requires operator review. Sync observability therefore reports a deliberately small projection of the local outbox and the last completed sync cursor.

> The operator-facing projection contains counts, timestamps, a bounded health category, and a redacted message. It does not contain payload JSON, error text, customer data, credentials, bearer tokens, or record identifiers.

The projection is implemented by `SyncHealthSnapshot` in `utils/SyncObservability.kt`. The persistence boundary is `ShopRepository.getSyncOutboxSummary()`, which aggregates state counts through `SyncOutboxDao` rather than returning outbox rows.

## Health classification

`SyncHealthSnapshot.from()` applies the following precedence. Higher-severity operational conditions win over the age of the last cursor because an outstanding failure is more actionable than a stale timestamp alone.

| Condition | Health | Operator meaning |
|---|---|---|
| One or more `DEAD_LETTER` entries exist | `BLOCKED` | Some local changes require review or an explicit recovery action. |
| No dead letters, but one or more `RETRYABLE` entries exist | `RETRYING` | Sync is recovering from a temporary failure; `nextRetryAtEpochMs` is the earliest retry timestamp when available. |
| No dead letters or retryable entries, but `PENDING` or `IN_FLIGHT` entries exist | `PENDING` | Local changes are waiting for the next sync cycle or are currently leased by a worker. |
| No outstanding entries and `lastSyncEpochMs` is zero or older than 24 hours | `NEVER_SYNCED` | There is no recent completed sync cursor available. This does not prevent offline commerce. |
| No outstanding entries and the last completed cursor is within 24 hours | `HEALTHY` | The local outbox is clear and a recent completed sync is recorded. |

The snapshot normalizes negative counts and negative timestamps to safe non-negative values. Its `totalOutstanding` convenience value is calculated from the four state counts and does not reveal individual records.

## Cursor semantics and compatibility

The existing settings store persists the sync cursor as the legacy human-readable value `lastSyncTime`. `SyncCursor.parse()` is now the canonical parser used both by `ShopViewModel` and `SyncWorker`. It accepts the following values:

| Input | Result |
|---|---|
| Epoch milliseconds | The non-negative epoch value. |
| `dd MMM yyyy, hh:mm:ss a` | Parsed epoch milliseconds using the English locale. |
| `dd MMM yyyy, hh:mm a` | Parsed epoch milliseconds using the English locale. |
| `Never Synced`, empty, or malformed text | `0L`. |

`SyncCursor.format()` writes the canonical seconds-inclusive legacy representation so existing settings screens and older installations remain compatible. A future schema migration may replace the human-readable field with a numeric cursor, but it must preserve this parser during the compatibility window.

## Conflict and dead-letter status

The outbox retains detailed failure information for the sync worker, but that detail is not returned by the observability projection. `conflictCount` is a subset count of dead-letter entries whose local error classification contains `conflict`; it is intentionally reported separately from `deadLetterCount` so the operator can distinguish remote conflict outcomes from other permanent failures without receiving the underlying error text.

The CloudSyncPolicy allowlist remains unchanged. Observability is read-only with respect to cloud policy and does not widen which entities or fields are eligible for synchronization.

## Operator recovery

`ShopViewModel.retrySyncDeadLetters()` is the explicit recovery action. It calls `ShopRepository.requeueSyncDeadLetters()`, which updates every `DEAD_LETTER` row to `PENDING`, resets `attemptCount` to zero, clears the retry timestamp and lease, and removes the persisted local error classification. The ViewModel then refreshes the redacted snapshot and schedules the existing instant sync path.

This action is intentionally broad and operator-controlled. It must not run automatically after every sync failure because dead-lettering exists to stop an unsafe or repeatedly failing mutation from being retried without acknowledgement. A later UI may add a confirmation dialog, reason capture, or an individually scoped recovery workflow; those additions must preserve the same authorization and audit requirements as other operator actions.

## Privacy and security invariants

The following invariants are release blockers for this surface:

1. `SyncHealthSnapshot` must not add `payloadJson`, `lastError`, `globalId`, local record IDs, credential fields, password hashes, PIN verifiers, bearer tokens, or customer-identifying fields.
2. Repository summary methods must return aggregate counts and timestamps only. Callers must not receive `SyncOutbox` rows merely to render a status badge.
3. Error messages displayed to an operator must remain bounded and redacted. Remote exception text must not be copied into the status snapshot.
4. Dead-letter recovery must not mutate commerce tables, ledger records, customer balances, or inventory. It only resets outbox delivery state.
5. Sync cursor advancement remains conditional on a completed downstream pull. The worker must not advance the cursor when the service returns the old cursor after a transient failure.
6. The sync observability slice must not change tenant authorization, identity reconciliation, backup authentication, or the CloudSyncPolicy allowlist.

## Test and CI gate

`SyncObservabilityTest` is a pure JUnit suite included in the required `testDebugUnitTest` allowlist. It covers recent healthy state, never-synced state, pending and retrying states, blocked state, conflict/dead-letter separation, all supported cursor formats, canonical formatting round-trip, and reflection-based absence of forbidden payload or credential fields.

The existing `SyncOutboxTest` remains the persistence regression suite for duplicate idempotency keys, exclusive leases, retry backoff, and dead-letter transitions. The two suites deliberately separate pure classification behavior from Room mutation behavior.

## Future Control Plane handoff

The SaaS Control Plane may consume an equivalent versioned aggregate in a later contract slice, but it must not require Merchant OS payloads to render health. The proposed handoff is a tenant-scoped, device-scoped status envelope containing a contract version, health category, aggregate counts, last cursor time, next retry time, and a redacted reason code. The Control Plane should treat the Merchant OS as the source of local operational facts while applying server-side authorization, retention, rate limiting, and audit policy.

Conflict resolution, replay protection, cursor ownership, tombstone semantics, and version negotiation belong to the subsequent SYNC-37 compatibility contract. This document intentionally defines only the local redacted projection and its operator recovery boundary.
