# Firebase local setup and automatic backup policy

## Current application identity

The current Merchant Android application uses this package identity:

```text
com.aistudio.shreeshyamstore.pqwzkb
```

A local `google-services.json` must contain an Android client registered for **this exact package name**. The historical file found in the developer Downloads folder belongs to the older client package `com.harrylabs.shreeshyamstore`. It must not be copied into the current app: changing the JSON text manually would not register the current package with Firebase and can break Google Sign-In and OAuth configuration.

## Obtaining the correct client file

In the Firebase Console, open project `shreeshyamstore`, add or select an Android app with package `com.aistudio.shreeshyamstore.pqwzkb`, enable the required Firebase Authentication providers, and download its client configuration. Place the downloaded file at:

```text
app/google-services.json
```

The file is intentionally ignored by Git. Never commit it, its API key, OAuth client secrets, service-account files, Firebase ID tokens, or any local customer data. The Google Services Gradle plugin is applied only when this local file exists, so CI and unconfigured checkouts remain buildable in offline mode while configured developer builds generate `google_app_id` and `default_web_client_id` resources for Google Sign-In.

The Firebase Realtime Database URL remains an environment-provided build value through `.env`/the Secrets Gradle Plugin. The example file contains only the non-secret project endpoint; authentication still requires a live Firebase session.

## Automatic sync and backup

When an authenticated store has automatic sync enabled, the app now schedules connected-network WorkManager jobs. Local mutations trigger an outbox sync and a latest-snapshot backup request. A periodic Firestore sync continues hourly, while an authenticated snapshot backup runs periodically every six hours. Snapshot uploads use the existing tenant-scoped authenticated provider and the existing restore envelope allowlist; device-local users, password verifiers, PIN material, bearer tokens, and raw credentials are not part of the snapshot.

If the user disables the automatic policy, the periodic sync, pending one-time sync, and automatic snapshot jobs are cancelled. Manual **Sync Now** and explicit **Backup Now** remain available to the operator when a valid Firebase session exists.

> Automatic restore is intentionally not enabled. A restore replaces cloud-owned local tables and can change merchant records; it remains an explicit, confirmed action after snapshot download, integrity validation, local recovery-point creation, and atomic replacement/rollback.

## Troubleshooting

| Symptom | Meaning | Correct action |
| --- | --- | --- |
| `Google Sign-In requires Firebase configuration.` | No Firebase app was initialized in the APK, normally because no matching local client file was supplied. | Download the client file for `com.aistudio.shreeshyamstore.pqwzkb`, place it at `app/google-services.json`, and rebuild. |
| `No matching client found for package name` during Gradle build | The supplied client file belongs to another Android package, such as the historical `com.harrylabs.shreeshyamstore` file. | Do not edit the JSON manually; download/register the correct Firebase Android client. |
| Product save reports a session or authorization message | Catalog writes require a reconciled local or Firebase identity actor. | Complete local setup or sign in again; the UI now waits for the actual Room result before showing success. |
| `Never Synced` remains visible | No authenticated sync or snapshot backup has completed successfully. | Sign in, enable automatic sync, keep the device online, or use the explicit manual action. |
