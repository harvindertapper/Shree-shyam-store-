# Stable Sync Identity and Offline Outbox Contract

**Status:** Implementation contract for the stable-sync slice

**Base revision:** `078e852` on `main`

## Objective

The current sync layer uses device-local numeric Room IDs as Firestore document IDs and treats `isSynced` as the complete queue state. That is unsafe when two devices create records independently, when a write is retried after an ambiguous network response, or when a deleted row must be propagated. This slice introduces stable global record IDs, an explicit outbox state machine, idempotency keys, tombstones, and a deterministic conflict policy while preserving the existing offline-first Room architecture.

## Scope

The seven cloud business tables remain the sync scope: categories, products, sales, sale items, customers, udhaar transactions, and stock adjustments. Account and credential tables remain excluded by the existing allowlist. Manual full backup/restore remains a separate snapshot workflow; it will continue to preserve device-local identity and will not be treated as an outbox acknowledgement.

## Stable identity

Every cloud business row receives a `globalId` UUID generated once on local creation. It is never regenerated during pull, restore, retry, or conflict resolution. Existing rows receive a deterministic migration ID in the form `legacy-<table>-<id>` so the first rollout does not create duplicate cloud records. New rows use random UUIDs. Room numeric IDs remain local compatibility keys for the current UI and foreign-key-like relationships; they are not used as cloud document IDs after this slice.

For child records, the global ID is independent and the existing local relationship fields remain unchanged for this slice. The cloud payload additionally carries `parentGlobalId` where it is available in the local mapping. A later relational-identity slice may replace local foreign keys with global references after two-device migration evidence is complete.

## Outbox record and lifecycle

Every local business mutation enqueues one logical outbox item using the tuple `(tableName, globalId, mutationVersion)`. The mutation version is the row's `updatedAt` timestamp plus the device ID tie-breaker; it is not allowed to move backward. The idempotency key is deterministic: `tableName/globalId/mutationVersion`. Re-running the same outbox item must overwrite the same cloud document and must not create a duplicate.

| State | Meaning | Next action |
| --- | --- | --- |
| `PENDING` | Local mutation has not completed a remote acknowledgement | Claim and send when network is available |
| `IN_FLIGHT` | A worker claimed the item | Resume after lease expiry; do not create a second logical mutation |
| `ACKED` | Remote write succeeded and local row was acknowledged for the same version | Retain history briefly or compact in a later maintenance slice |
| `RETRYABLE` | Remote request failed transiently or returned an ambiguous result | Retry after bounded exponential backoff |
| `DEAD_LETTER` | Maximum attempts or validation failure reached | Stop automatic retries and surface sync health for operator action |

The outbox stores table name, global ID, local row ID, mutation version, idempotency key, serialized payload, tombstone flag, attempt count, next-attempt time, lease time, last error, and state. A worker claims only eligible rows whose lease is absent or expired. Claims are short-lived and are released by acknowledgement or retry transition.

## Tombstones

Deletion is represented as a cloud-safe tombstone (`isDeleted = true`) with the row's stable global ID, mutation version, updated timestamp, and deletion metadata. The row is not physically removed by a business delete command. Pull never resurrects a local row when a newer tombstone exists. Tombstones must remain remotely available longer than the maximum offline interval plus conflict-recovery window; automatic compaction is explicitly deferred until retention policy and two-device tests exist.

## Conflict policy

The first production-safe policy is **last-writer-wins by `(updatedAt, deviceId)`**, where the larger timestamp wins and the device UUID breaks exact timestamp ties. A stale pull must not overwrite a newer local mutation. A stale push must not overwrite a newer remote document; the service must mark the outbox item retryable or dead-letter it with a conflict reason and pull the winning document. Financial ledger events are append-only and therefore never merged by overwriting an existing event: a duplicate global ID with a different payload is a conflict requiring dead-letter review.

The policy is intentionally conservative. It does not silently merge product stock, sale totals, or customer balances across devices. Those domains require command/event reconciliation in later slices. For the current release, the winning version is preserved, the loser remains in the local outbox/dead-letter record, and the sync health surface must make the conflict visible.

## Migration compatibility

Room advances from version 5 to version 6. Each business entity gains `globalId`, `lastMutationDeviceId`, and `mutationVersion` fields with legacy deterministic defaults. A new `sync_outbox` table is created. Existing unsynced rows are discovered and enqueued lazily by the first worker run; the migration must not perform network I/O. Existing `isSynced` fields remain during this slice for UI compatibility, but the worker treats the outbox state as authoritative for outbound delivery.

Old Firestore documents without `globalId` are read using `legacy-<table>-<id>` and are rewritten with the stable ID on the next successful push. REST snapshots accept missing metadata and derive the same deterministic legacy ID. Account tables and credential fields remain blocked by `CloudSyncPolicy`.

## Acceptance tests

| Test | Required evidence |
| --- | --- |
| Legacy migration | v5 rows receive deterministic global IDs and a v6 outbox table is present |
| Local creation | A new mutation receives one global ID and one deterministic outbox key |
| Idempotent retry | Replaying the same outbox item produces one cloud document identity and one acknowledgement |
| Lease recovery | An expired `IN_FLIGHT` item can be reclaimed; a live lease cannot be double-claimed |
| Tombstone propagation | A deleted row pushes as a tombstone and a pull does not resurrect it |
| Stale pull protection | A local newer mutation is not overwritten by an older cloud payload |
| Tie-break conflict | Equal timestamps resolve deterministically by device ID |
| Dead-letter behavior | Exhausted or conflicting items stop automatic retry and retain an actionable error |
| Two-device ID isolation | Two independent devices creating the same logical product/customer do not collide by local numeric ID |
| Privacy boundary | Outbox and cloud payloads contain business fields only; credential tables remain excluded |

## Explicit non-goals

This slice does not redesign all Room relationships around global foreign keys, implement server-side transactional conflict resolution, compact tombstones, add automatic UPI settlement, or replace manual backup/restore with a versioned snapshot protocol. Those remain later release boundaries.

## References

[1]: ../docs/DECISIONS.md "Approved architecture decisions"
[2]: ../docs/BASELINE_AUDIT.md "Baseline risk register"
[3]: ../app/src/main/java/com/example/data/Entities.kt "Current Room entities"
[4]: ../app/src/main/java/com/example/data/AppDatabase.kt "Current Room migration chain"
[5]: ../app/src/main/java/com/example/utils/FirebaseSyncService.kt "Current Firestore and REST synchronization"
[6]: ../app/src/main/java/com/example/utils/SyncWorker.kt "Current background sync worker"
[7]: ../app/src/main/java/com/example/utils/CloudSyncPolicy.kt "Cloud business-table allowlist"
