# Release and Recovery Runbook

## Release posture

Only reviewed, tagged commits should produce release artifacts. Debug APKs are for development and internal verification. A production release requires an approved application identity, managed signing credentials, environment-specific Firebase configuration, privacy/security review, a tested migration path, and a rollback or recovery plan.

## Pre-release checklist

| Area | Check |
| --- | --- |
| Source | Pull request is reviewed, CI is green, working tree is clean, and the commit is identified by tag or immutable SHA |
| Build | Gradle wrapper, Java, SDK, version code/name, and release configuration are reproducible |
| Data | Room migrations are tested; destructive fallback is not relied on for production; backup ownership and restore rehearsal are documented |
| Security | No credentials leave device-local boundaries; no secrets are committed; auth/authorization negatives and dependency findings are reviewed |
| Product | Critical browse, billing, inventory, udhaar, settings, and error journeys pass on a clean environment |
| Operations | Crash/error monitoring, sync status, backup schedule, support owner, and incident contact are known |

## Staging sequence

Build from the reviewed commit with the target environment supplied outside Git. Install the artifact into a clean staging device or emulator, run database migration checks, exercise offline billing and inventory, verify sync retry behavior with an isolated test store, and rehearse restore using a non-production snapshot. Record the artifact checksum, test device/API levels, environment identifier, and known limitations.

Do not use live customer data for validation. Do not point a debug build at a production database. If a migration or restore operation is irreversible, take and verify a recovery copy before the staging drill.

## Production sequence

Create a release tag only after staging evidence is complete. Build the signed artifact in the controlled release environment, verify the checksum and package identity, publish through the chosen distribution channel, and use a small-scope rollout when the platform supports it. Monitor crashes, authentication failures, sync error rates, checkout failures, and inventory/ledger anomalies after release.

The first production launch should not include unrelated refactors or untested schema changes. If a release changes cloud payloads, identity, conflict semantics, or database schema, coordinate the client rollout and compatibility window explicitly.

## Rollback and recovery

For a UI or logic regression, halt rollout and publish the last known-good artifact if the data schema and cloud contract remain compatible. For a schema or cloud-contract issue, do not blindly downgrade the app; first preserve logs and local/cloud snapshots, determine whether forward migration is safer, and follow the owner-approved recovery procedure.

For cloud restore, stop repeated retries if the snapshot is incomplete or validation fails. Preserve the local device state and recovery copy, report the failure without clearing business tables, and investigate the snapshot/provider boundary. Never recover by importing user credentials or password hashes from cloud data.

## Incident evidence

Record the release SHA/tag, artifact checksum, app version, affected device/API levels, environment, timestamps, user-visible symptom, relevant request/sync identifiers, data impact, mitigation, and follow-up action. Redact personal data, tokens, passwords, PINs, and full customer records from incident notes.
