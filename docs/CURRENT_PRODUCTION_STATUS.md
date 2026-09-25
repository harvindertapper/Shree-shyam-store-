# ZenMart — current engineering and release status

**Reviewed:** 25 September 2026
**GitHub main reviewed:** `ae426706f29189ed705fac3bbf990c8b9f262d0e`
**Integration branch:** `codex/zenmart-sync` (local; not pushed)
**Android application ID and namespace:** `com.sevenzenlabs.zenmart`
**Launcher name:** ZenMart

This replaces the stale August status snapshot. GitHub main contains the latest home/theme refresh and dependency updates through PR #82. The integration branch carries those commits alongside the ZenMart package migration and updated production, test, and schema packages. The supplied Firebase client targets another package and has been removed from the build; a matching ZenMart client is not yet configured.

## Product that exists

The Android app is an offline-capable merchant product, not a screen-only mockup. It includes Room-backed catalog and inventory, barcode scanning, sales checkout, integer-paise money handling, cash/UPI/Udhaar recording, customer balances, stock adjustments, reports, PDF/CSV sharing, Hindi/English copy, app locking, and local authentication. Database migrations and focused commerce, security, synchronization, and recovery tests are present through Room schema version 11.

The app also contains an idempotent synchronization outbox, retry/conflict policies, Firestore client code, authenticated snapshot backup, validated restore and local recovery-point handling, release signing/R8 configuration, and Android CI. These prove implementation exists; they do not prove that production Firebase rules/provider are deployed, a signed release has been distributed, or a real shop has completed backup and restore.

Payment recording is not bank settlement verification. Profit reporting is not complete until each sale preserves a valid cost basis. Future multi-store administration and marketplace plans are not shipped capabilities.

## Integration changes in this branch

- Brings six GitHub main commits after the prior local base, including the latest home screen/theme and Room, Retrofit, Google Services, and GitHub Actions updates.
- Renames production, test and instrumentation packages and the Room schema directory to `com.sevenzenlabs.zenmart`.
- Sets the Android namespace and application ID to `com.sevenzenlabs.zenmart`; sets the launcher/project name to ZenMart.
- Updates CI selectors, schema checks, keep rules, and active Firebase/release documentation for the new identity.
- Removes the copied Firebase client because it targets `com.harrylabs.shreeshyamstore`. Register `com.sevenzenlabs.zenmart` in Firebase and download a matching client before enabling cloud sign-in.

Android treats a different application ID as a separate app and will not carry the earlier package's private Room database into this installation automatically. Before replacing any existing build, export its data using a supported app backup and rehearse restore on a clean device. Do not uninstall an older app until its data is recovered.

## Release gates still requiring external evidence

1. Assemble and inspect debug and minified release artifacts under the ZenMart ID; verify signing policy.
2. Run the configured Android CI suite, including API 35/36 device journeys, after package migration.
3. Verify Firebase Authentication providers, tenant-scoped database rules, trusted backup endpoint, project region and budget controls in the actual Firebase/hosting consoles.
4. Rehearse old-package data export, clean installation, restore, and recovery on test data and a separate staging tenant.
5. Verify offline billing, duplicate-submit safety, process death, low-end hardware, sync retries/conflicts, cloud backup and restore end to end.
6. Assign support/incident ownership, confirm privacy disclosures and retention, and document a release rollback decision.

Until these gates have evidence, describe the app as a development/staging candidate, not a production-proven service. Core shop operation should remain available without Firebase or network access. Managed cloud has provider and support costs; do not promise it is permanently free or unlimited.

## Source of truth

Use current source and CI on GitHub main for merged changes, then review `codex/zenmart-sync` for package and local workspace integration. August audits are historical snapshots. Tie future status updates to a commit SHA and mark staging facts verified only after the operator records them.
