# Restore Snapshot Envelope and Recovery Point

## Purpose

PR #35 turns manual restore into a validated snapshot replacement workflow. The Merchant OS first downloads one complete snapshot envelope, validates its schema, tenant identity, completeness, counts, checksum, record identities, payment values, quantities, and foreign references, then writes a local recovery point before replacing cloud-owned Room tables. The existing repository transaction remains the only business-data replacement boundary [1].

The implementation is intentionally independent of the transport. `FirebaseSyncService` currently provides transitional snapshot upload/download primitives, while the authenticated provider from PR #34 can carry the same envelope once that branch is merged. The envelope is therefore a platform contract rather than a Firebase-specific payload shape [2].

## Envelope contract

`SnapshotEnvelope` contains the snapshot schema version, organization/store/membership identity, source device and installation metadata, creation time, exact table counts, SHA-256 content checksum, completeness marker, and the seven allowlisted cloud business tables.

| Field | Validation rule |
| --- | --- |
| `schemaVersion` | Must equal the supported version; unknown versions are rejected before replacement |
| Organization/store/membership | Must match the authenticated local tenant context |
| `createdAtEpochMs` | Must be positive and not materially in the future |
| Source device/install | Must be non-blank for audit and recovery provenance |
| `tableCounts` | Must contain exactly the seven required table names and equal decoded record counts |
| `checksumSha256` | Must match canonical table serialization, in fixed table order |
| `complete` | Must be true for cloud restore; incomplete snapshots are never imported |
| `snapshot` | Must contain only validated business records; device-owned identity is outside the envelope |

The checksum covers canonical JSON for categories, products, sales, sale items, customers, udhaar transactions, and stock adjustments. It does not include credentials, local users, app-lock state, PIN verifiers, Firebase tokens, or other device-owned data. The cloud table allowlist remains unchanged [3].

## Validation order

The restore flow is deliberately ordered so a bad response cannot clear local business tables:

1. The complete envelope is downloaded. An absent, empty, malformed, unavailable, or non-success response fails the operation.
2. The schema version, tenant identity, source metadata, completeness marker, table set, counts, and creation time are checked.
3. The checksum is recomputed from the decoded table payload.
4. Every table is checked for non-blank and duplicate stable global identity. Legacy blank identities are normalized only to the deterministic existing `legacy-<table>-<id>` form.
5. Record values and references are validated. Products must reference categories; sale items must reference products and sales; sales and ledger events must reference valid customers or sales; stock adjustments must reference products; monetary values and physical quantities must be non-negative and finite where applicable.
6. The current local cloud-owned snapshot is serialized, validated, and committed as the local recovery point.
7. Only then does `ShopRepository.replaceCloudRestorableTables()` perform the transactional replacement. Device-owned users and shop profile data remain outside the operation [1].

An all-empty cloud snapshot is rejected with `SnapshotEmptyException`. This prevents an unavailable or accidentally empty response from being interpreted as an instruction to clear a merchant database.

## Local recovery point

`LocalRecoveryPointStore` writes the verified pre-restore snapshot below the app’s private files directory. It writes a temporary file first, stages the previous recovery point as a backup, renames the complete temporary payload into place, and removes the backup only after the new file is committed. If the process stops between the staged rename and the final rename, the reader can still use the `.bak` recovery copy.

The recovery point contains only cloud-restorable business tables and the same tenant/source metadata. It never contains the `users` table, password hashes, app-lock settings, PIN verifiers, session secrets, or bearer tokens. The recovery point is retained after a successful restore so the last pre-restore state remains available for operator recovery and later release rehearsal.

If Room replacement throws after validation, the flow validates the retained recovery envelope again and attempts one compensating replacement from that local point. The original restore exception remains the primary failure and any rollback failure is attached as a suppressed error. A rollback attempt never imports device-owned identity data.

## Failure and rollback policy

| Failure category | Result |
| --- | --- |
| Empty or missing snapshot | Reject; local data remains untouched |
| Unsupported schema | Reject; no migration-by-guessing |
| Wrong organization/store/membership | Reject before record processing |
| Missing table, incomplete marker, or count mismatch | Reject as incomplete |
| Checksum mismatch | Reject as corrupt |
| Duplicate or blank stable identity | Reject as integrity failure |
| Invalid money, quantity, payment, or foreign reference | Reject as referential/data failure |
| Recovery-point write failure | Reject before replacement |
| Room replacement failure | Attempt one validated rollback from the retained local point |

The application does not retry a rejected snapshot as if it were a transient network error. Operators should preserve the rejected snapshot metadata and local recovery point, investigate the provider boundary, and use a forward corrective release rather than downgrading the database.

## Tests and acceptance evidence

`RestoreRecoveryEnvelopeTest` covers empty snapshots, missing tables, wrong tenant, checksum mismatch, invalid foreign references, duplicate global identities, unsupported schema, incomplete snapshots, malformed JSON, local recovery round trips, and empty recovery-point validation. Existing restore-security tests continue to prove that device-owned identity and shop-profile data are preserved [4].

The next integration step after PR #34 is merged is to make the authenticated backup provider upload and download this envelope as its typed snapshot payload. The server-side Control Plane should eventually treat the envelope metadata as untrusted client input, verify the bearer token, derive tenant scope from server membership, and record auditable backup/restore events.

## References

[1]: ../app/src/main/java/com/aistudio/shreeshyamstore/pqwzkb/data/ShopRepository.kt "Transactional cloud-restorable replacement"
[2]: ../app/src/main/java/com/aistudio/shreeshyamstore/pqwzkb/utils/FirebaseSyncService.kt "Transitional snapshot transport"
[3]: ../app/src/main/java/com/aistudio/shreeshyamstore/pqwzkb/utils/CloudSyncPolicy.kt "Cloud business-table allowlist"
[4]: ../app/src/test/java/com/aistudio/shreeshyamstore/pqwzkb/RestoreSecurityTest.kt "Restore privacy and device-owned data regression tests"
