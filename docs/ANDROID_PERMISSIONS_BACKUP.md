# Android Permissions and Backup Policy

**Status:** Implemented on `chore/permissions-backup-policy` for review

## Permission inventory

| Capability | Manifest entry | Runtime behavior | Decision |
| --- | --- | --- | --- |
| Network access | `INTERNET` | Normal permission; used by Firebase/REST sync and manual backup | Retained because cloud operations are explicit app features |
| Network state | `ACCESS_NETWORK_STATE` | Normal permission; used by connectivity-aware WorkManager scheduling | Retained because sync must avoid blind network work |
| Barcode camera | `CAMERA` | Requested only when the barcode scanner dialog is opened; denial keeps the dialog in a safe explanation/retry state | Retained as an optional feature because the camera feature is not required for installation |
| Storage, location, contacts, phone, SMS, notifications | None | No runtime request or manifest declaration | Not requested; future features must justify each new permission separately |

The launcher activity is the only exported application component. The `FileProvider` is explicitly non-exported and grants read URIs only through temporary intent flags. Its configured paths are private cache/files directories, not arbitrary external storage.

## Automatic backup and data extraction

Automatic Android backup is disabled with `android:allowBackup="false"`. The legacy `full-backup-content` and Android 12+ `data-extraction-rules` resources also exclude the entire private root as defense in depth, covering Room, DataStore, cached exports, and future private files. The exclusion applies to both cloud backup and device transfer.

This is deliberate because the app stores PIN verifiers, session metadata, identity projections, customer/ledger records, and business data locally. The application’s manual cloud backup/restore flow remains the only supported business-data transfer path; it is governed by the repository’s validation, privacy, and atomic restore contracts rather than by Android’s opaque automatic backup service.

## Operational implications

Turning off automatic backup means uninstall/reinstall and device migration do not restore local store data automatically. Operators must use the explicit in-app backup process and confirm that the business snapshot is complete and recoverable before changing devices. Release smoke tests must verify that the app launches without automatic restore assumptions and that manual restore remains available after a clean install.

The policy does not add permissions for Firebase, Room, DataStore, FileProvider sharing, or the Android photo picker. The photo picker uses a system-mediated activity and does not require broad storage permissions.
