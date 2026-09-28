# Firebase local setup and automatic backup policy

## Application identity

ZenMart's Android namespace and application ID are:

```text
com.sevenzenlabs.zenmart
```

The supplied `google-services.json` registers `com.harrylabs.shreeshyamstore`, so it does not configure this app. Do not copy or edit that file for ZenMart. Register `com.sevenzenlabs.zenmart` as an Android app in the intended Firebase project and download a fresh client configuration. Keep the matching file at `app/google-services.json` locally; Git ignores it, and it must never be committed.

The Google Services plugin is applied only when the local file exists, so an unconfigured checkout can still build and use offline features. A client configuration alone does not prove Firebase Authentication, Firestore/Realtime Database rules, backup endpoint or tenant authorization are deployed correctly.

## Isolated staging identity

The `staging` build type uses `com.sevenzenlabs.zenmart.staging` and a `-staging` version suffix. It is deliberately local-only for now (`CLOUD_SYNC_ENABLED=false`) and uses the debug signing key; this allows installation beside production without writing to the current Firestore path. CI verifies the built staging APK package name. It is **not** a cloud-enabled pilot build yet.

Before enabling staging cloud access, register this exact staging package in a separate Firebase Authentication project, provide its matching client configuration outside Git, deploy a separate Worker/D1 staging database, and wire the Android event adapter. Then add a build guard that refuses mismatched Firebase project/Worker combinations. Do not copy the production `google-services.json` into the staging variant or flip the flag while direct Firestore business sync remains active. See [#85](https://github.com/harvindertapper/Shree-shyam-store-/issues/85) and [#87](https://github.com/harvindertapper/Shree-shyam-store-/issues/87).

## Automatic sync and backup

When a signed-in store has automatic sync enabled, WorkManager schedules connected-network sync and authenticated snapshot backup. The sync outbox and snapshot backup serve different purposes. Device-local users, credential verifiers, app-lock state, bearer tokens and raw credentials must not be uploaded.

Automatic restore is intentionally disabled. Restore requires explicit confirmation, authenticated download, tenant and integrity validation, a verified local recovery point, and atomic replacement with rollback.

Cloud features require a correctly configured Firebase project, Authentication providers, tenant-scoped server rules and a trusted backup provider. Local billing, inventory, ledger and reports must remain usable without Firebase or network access. Do not promise unlimited or permanently free cloud service.

## Existing installations

Android treats a different application ID as a separate app. It will not carry a prior package's private Room database into ZenMart automatically. Before replacing any installed build, create an app-supported export from that installation and rehearse import/restore on a clean ZenMart installation. Do not uninstall an old app until its data is recovered.
