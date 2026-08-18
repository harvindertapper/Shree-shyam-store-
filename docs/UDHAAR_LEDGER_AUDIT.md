# Immutable Udhaar Ledger and Audit-Actor Contract

**Status:** Proposed implementation contract for the next production-hardening slice

**Base revision:** `4f3465a` on `main`

## Objective

The udhaar ledger must become an append-only business event history. A credit or received payment must never be edited or physically deleted as a way to correct a mistake. Corrections must append a linked reversal or correction event, retain the original event, record the actor who performed the operation, and preserve a deterministic customer balance.

This slice deliberately keeps the existing offline-first Room architecture and current integer-paise money representation. It does not redesign Firebase identity, introduce a full role-administration UI, or solve cross-device global IDs; those are later roadmap slices. The local actor contract introduced here is designed so those future changes can replace the actor source without changing ledger semantics.

## Current problems being closed

| Area | Current behavior | Required behavior |
| --- | --- | --- |
| Ledger correction | A raw delete API exists for `UdhaarTransaction` | No business path may delete a ledger event; correction appends a linked event |
| Event provenance | Rows contain amount, type, note, and timestamps only | Rows contain immutable event identity, actor snapshot, and correction linkage |
| Actor source | Payment recording has no actor parameter | Repository commands require a non-empty authenticated actor snapshot |
| Authorization | UI is the only practical boundary | Repository/transaction boundary rejects missing or unauthorized actors |
| Balance | Aggregation uses `CREDIT` and `PAYMENT` only | Reversal/correction events have explicit signed semantics and are included deterministically |
| Sync/restore | Cloud rows are reconstructed using the old minimal shape | New fields are serialized with compatibility defaults; credentials remain device-local |

## Event model

Each `UdhaarTransaction` remains a Room row, but it is treated as an immutable event after insertion. The event receives a stable local `eventId` UUID, while the existing numeric Room `id` remains the local primary key for this slice. The event type becomes a typed compatibility-safe value with the following serialized values:

| Event type | Amount sign in balance | Purpose | Required links |
| --- | ---: | --- | --- |
| `CREDIT` | `+amount` | Original udhaar created by a bill | Optional `saleId`; no correction link |
| `PAYMENT` | `-amount` | Received payment | No correction link for a normal payment |
| `REVERSAL` | Opposite of original event | Cancels an earlier ledger event | `correctsEventId` required |
| `CORRECTION` | Explicit replacement effect | Records a corrected amount or business meaning | `correctsEventId` required; reason required |

The first implementation uses an explicit `balanceEffect` field in integer paise rather than deriving signs from free-form event strings. This makes balance aggregation auditable and allows a reversal to negate either a credit or a payment. `amount` remains non-negative magnitude; `balanceEffect` carries the signed effect. Existing v4 rows are migrated as `CREDIT -> +amount` and `PAYMENT -> -amount`.

A correction is append-only. The original row remains visible in the audit history, while the balance query includes the original effect plus the correction/reversal effect. A corrected event cannot be corrected a second time in the same command unless a future policy explicitly permits chained corrections; this slice rejects duplicate correction links to keep the history unambiguous.

## Actor contract

An actor snapshot is captured at the command boundary, not resolved later from mutable user records. The snapshot contains:

| Field | Rule |
| --- | --- |
| `actorUid` | Required, trimmed, non-empty. Derived from the current authenticated/session UID; never a PIN, password, or token. |
| `actorName` | Required display snapshot. Prefer the session username, then email, then UID. |
| `actorRole` | Required compatibility-safe role string. Existing sessions default to `OWNER`; future role administration can issue `MANAGER` or `CASHIER`. |
| `actorDeviceId` | Local device identifier for audit correlation only; it is not an authentication secret. |
|

The repository requires an actor for all new ledger writes. Recording a normal payment requires an authenticated actor with `OWNER`, `MANAGER`, or `CASHIER` role. Reversing or correcting an existing event requires `OWNER` or `MANAGER`. The UI cannot bypass these checks because authorization is enforced inside the repository transaction. The current session model has no role-management UI, so existing authenticated sessions receive the compatibility role `OWNER`; role administration is a separately tracked follow-up.

## Write invariants

Every ledger command must satisfy all of the following conditions:

1. The actor UID and actor name are non-empty and the role is recognized.
2. The customer exists, is active, and is not soft-deleted.
3. Amount magnitude is strictly positive and already represented in integer paise.
4. Event type and balance effect are compatible with the command.
5. Normal payment recording appends exactly one `PAYMENT` event.
6. Checkout appends exactly one `CREDIT` event in the same Room transaction as its sale, sale items, stock adjustment, and customer touch.
7. A correction or reversal references an existing active event for the same customer.
8. The target event is not already reversed or corrected by another active correction event.
9. A correction/reversal reason is non-empty and bounded to a safe length.
10. The original event is never updated or deleted; the new event is the only write for a correction.
11. The resulting balance is calculated from signed `balanceEffect` values and is never silently clamped.
12. The event and its sync state are committed atomically; the event is marked unsynced until the remote write is confirmed.

## Migration and compatibility

Room will advance from version 4 to version 5. The migration adds the new event and actor columns, generates a UUID for every existing row, and maps legacy `type` values to signed `balanceEffect` values. Existing rows receive an actor snapshot of `legacy-local` / `Legacy local record` / `OWNER` / local device ID and a null correction link. This is an explicit provenance marker, not a claim about the person who originally recorded the event.

Firestore and REST payloads will include the new fields with compatibility defaults. Pull mapping will accept missing fields from older cloud rows and derive `eventId` and `balanceEffect` from the legacy type/amount. No `User` table, password hash, PIN verifier, session secret, or authentication token will be added to the cloud contract.

## Verification matrix

| Test | Evidence required |
| --- | --- |
| v4 -> v5 migration | Existing credit/payment rows retain amount and receive correct signed effects and actor compatibility markers |
| Actor validation | Missing UID/name, unknown role, and unauthorized correction actor are rejected at the repository boundary |
| Payment append-only behavior | A payment creates one new row and does not mutate existing rows |
| Correction behavior | A correction creates one linked row, preserves the original row, and changes balance by the expected signed effect |
| Duplicate correction | A second correction against the same target is rejected and leaves all rows unchanged |
| Customer isolation | An actor cannot correct another customer’s event through a mismatched customer ID |
| Atomic failure | Invalid correction reason, missing target, or failed authorization leaves the ledger unchanged |
| Balance reconstruction | Mixed credit, payment, reversal, and correction events produce the documented balance |
| Privacy | Actor fields contain identity snapshots only; credential and PIN fields remain absent from all cloud payloads |

## Explicit non-goals

This slice does not introduce a production role-management screen, Firebase custom claims, globally unique sync IDs, conflict resolution, payment gateway references, automatic UPI settlement, or a full audit-log viewer separate from the existing customer ledger. Those remain subsequent roadmap work.

## References

[1]: ../docs/DECISIONS.md "Approved architecture decisions"
[2]: ../docs/P0_FINANCIAL_SAFETY.md "P0 financial-safety scope and deferred work"
[3]: ../app/src/main/java/com/example/data/Entities.kt "Current Room entities"
[4]: ../app/src/main/java/com/example/data/Daos.kt "Current DAO transaction and ledger queries"
[5]: ../app/src/main/java/com/example/data/ShopRepository.kt "Current repository ledger API"
[6]: ../app/src/main/java/com/example/data/SettingsDataStore.kt "Current session and settings persistence"
[7]: ../app/src/main/java/com/example/utils/FirebaseSyncService.kt "Cloud serialization and restore mapping"
