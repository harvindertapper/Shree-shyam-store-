# Merchant Sync Contract v1

**Status:** Contract and adapter-boundary slice for SYNC-37. The future Control Plane remains the server-authoritative implementation; this Android repository contains the versioned validation policy and reusable fixtures only.

## Scope and authority

The Merchant Android OS remains an offline-first client. Room is authoritative for local continuity and immediate commerce UX, but it is not treated as server authority. The future Control Plane must validate tenant scope, membership, device binding, mutation identity, replay, ordering, tombstone retention, and conflict outcomes before accepting a mutation.

> A client-supplied tenant claim is an input to validation, not proof of authorization.

`SyncContractV1` is intentionally a pure policy boundary. It validates a `SyncMutationEnvelope` against trusted local session context and returns stable identity metadata. It does not write Room state, call Firestore, create server records, or silently resolve financial conflicts.

## Versioned mutation envelope

The v1 envelope contains the following categories of fields:

| Category | Fields | Contract rule |
|---|---|---|
| Version and tenant | `contractVersion`, `tenant`, `appInstallationId` | Version must be supported. Organization, store, membership, device, and installation must match trusted context. |
| Actor binding | `actor`, `mutationDeviceId` | Actor and mutation device must be bound to the authorized tenant device and authenticated session role. |
| Stable identity | `tableName`, `globalId`, `mutationVersion`, `mutationDeviceId`, `idempotencyKey`, `clientEventId` | Table must be allowlisted. Mutation version must be positive. The idempotency key must equal `table/globalId/mutationVersion`. |
| Timing | `clientCreatedAt`, `updatedAt` | Timestamps must be positive and cannot be materially in the future. Server-side freshness policy remains authoritative. |
| Deletion | `tombstone`, `tombstoneAtEpochMs` | A tombstone carries no business payload. Its deletion timestamp must equal `updatedAt`. Live mutations cannot carry tombstone metadata. |
| Business payload | `payload` | The domain adapter owns an explicit allowlist. Credential, token, PIN, verifier, and secret-like field names are rejected by the shared boundary. |

The current allowlist is `categories`, `products`, `sales`, `sale_items`, `customers`, `udhaar_transactions`, and `stock_adjustments`, matching the existing Merchant OS sync surface. Changing it requires a separate review because it changes cloud data policy.

## Replay, ordering, and conflict semantics

The server or a future server adapter compares the incoming identity with the stored identity for the same tenant and global record. The comparison is deterministic: higher `mutationVersion` wins; an equal version is ordered by `mutationDeviceId` using the existing stable comparison helper.

| Incoming condition | Outcome | Required behavior |
|---|---|---|
| No stored identity | `ACCEPTED` or `TOMBSTONE_ACCEPTED` | Persist the mutation under the validated tenant scope. |
| Same idempotency key | `REPLAY` | Return the existing result or an idempotent acknowledgement. Never create a duplicate sale, payment, ledger event, or inventory movement. |
| Lower mutation ordering | `STALE_REJECTED` | Reject as `STALE_COMMAND`; do not overwrite the newer stored mutation. |
| Equal ordering with a different identity | `CONFLICT_REJECTED` | Return a typed `CONFLICT`; do not silently overwrite financial history. |
| Higher mutation ordering | `ACCEPTED` or `TOMBSTONE_ACCEPTED` | Apply only after server-side authorization and domain validation. |

Conflict resolution is not equivalent to “last write wins” for commerce history. A Control Plane implementation may record a winning projection, but it must retain an auditable conflict outcome and must not erase an already accepted financial event.

## Tombstones

A tombstone is a mutation identity plus deletion metadata, not an empty ordinary upsert. It must be retained long enough for offline devices to observe the deletion and must participate in replay and ordering comparisons. The v1 Android policy validates the shape and prevents payload-bearing tombstones; server retention duration and garbage collection belong to the Control Plane contract.

A deleted record must not be resurrected by a stale live mutation. A replayed tombstone must be idempotent, and a newer authorized live mutation can only replace it under the same identity/order and domain authorization rules as any other mutation.

## Cursor ownership and advancement

`SyncContractCursor` is tenant- and device-owned. `SyncCursorPolicy` rejects unsupported versions, negative values, mismatched tenant scope, and cursors owned by another device. A candidate cursor may advance the stored cursor but may never move it backward. Equal cursors are idempotent and preserve the current cursor instance.

The Android worker’s existing transient-failure behavior remains important: if downstream pull fails, the old cursor is retained. A cursor is advanced only after the corresponding pull has completed successfully. A future server API should bind cursors to the authenticated tenant and device rather than accepting an arbitrary client cursor.

## Typed error categories

`SyncContractErrorCategory` provides stable categories for adapter and Control Plane interoperability. Authorization, unsupported versions, invalid mutation/payload/tombstone, stale commands, conflicts, and cursor regressions are permanent failures until the command is corrected. Only `RETRYABLE_FAILURE` represents a transient transport or service condition; `PERMANENT_FAILURE` is reserved for validated non-retryable service outcomes.

The client may use these categories to decide between retryable outbox state and dead-letter state, but the category does not grant permission to replay a command. Replay must still pass idempotency and tenant validation.

## Security invariants

The following rules are release blockers for this contract:

1. The server must derive authoritative tenant scope from authenticated membership and device enrollment. It must not trust `tenant` merely because the client supplied it.
2. No sync envelope or contract fixture may contain bearer tokens, password hashes, PIN verifiers, local security PINs, or device-only secrets.
3. Domain payloads must remain explicit allowlists. The shared forbidden-field guard is a defense-in-depth check, not a replacement for per-table serialization.
4. Replay acknowledgement must not create a second financial event.
5. Conflict rejection must not silently overwrite accepted financial history.
6. Cursor advancement must be monotonic and tenant/device owned.
7. Tombstone retention and stale-delete protection must be enforced server-side before garbage collection.
8. Contract version changes must be additive or explicitly negotiated. An unsupported version is a typed permanent failure, not an implicit downgrade.

## Fixture reuse and future Control Plane handoff

`SyncContractTest` is deliberately pure JUnit and contains fixtures for accepted mutation, unauthorized tenant, stale command, replay, tombstone, conflict, cursor regression, cursor ownership, unsupported version, invalid idempotency, typed failures, and forbidden payload fields. The future private Control Plane repository should port these fixtures as contract tests rather than sharing Android Room models.

The current Firebase adapter still uses the existing Firestore path and field serializers for Merchant OS background sync. This PR does not pretend that Firebase is the future Control Plane or introduce client-side server authority. A later adapter migration may translate the existing outbox rows into `SyncMutationEnvelope`, but that change must be reviewed with authentication, tenancy, and deployment configuration together.
