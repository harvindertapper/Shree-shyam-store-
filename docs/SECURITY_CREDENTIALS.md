# Local credential security boundary

## Scope

Local account passwords and app-lock PIN verifiers are **device-local security material**. They are not business records and must not be included in Firestore collections, REST backup JSON, sync outbox payloads, analytics, crash reports, logs, or exported diagnostics. The `users` Room table remains a local compatibility projection; its `passwordHash` column name is retained for schema compatibility, but new values are versioned credential records rather than raw hashes.

## Current credential format

New local credentials use a versioned, per-record salted PBKDF2-HMAC-SHA256 verifier:

```text
v2:<scope>:<iterations>:<salt-hex>:<derived-key-hex>
```

The current scope values are `local-account` and `app-lock`. A scope mismatch is rejected, so an app-lock verifier cannot authenticate a local account and vice versa. Each credential receives a random 16-byte salt and a 32-byte derived key. The iteration count is stored with the record so future releases can increase the work factor without misinterpreting an older record.

The legacy SHA-256 verifier is retained only as a migration reader. A successful legacy local-account login immediately attempts to replace it with a v2 record. If that local write fails, the user is still allowed to continue offline and the migration can be retried on a later login; no network operation is required for authentication. New registrations never create legacy SHA-256 records.

## App-lock migration and default-PIN sunset

The app-lock preference has an explicit format marker. Existing SHA-256 PINs and historical four-digit plaintext PINs are accepted only while the marker is legacy. A missing app-lock preference is treated as the pre-configuration compatibility window in which `1234` may unlock the app. On the first successful compatibility unlock, the entered PIN is immediately replaced with a v2 app-lock verifier and the marker is advanced to the current version. After that point, the blank/default fallback is no longer accepted.

A new PIN must satisfy the existing four-digit policy and must not be the historical default, a repeated digit, or an obvious ascending or descending sequence. Saving an empty value does not create a new weak credential; it explicitly records that the legacy migration marker remains active. This compatibility path is temporary and should be removed in a future release after the supported migration window has elapsed and an upgrade notice or recovery flow is available.

## Throttling

Local account password attempts have an independent DataStore-backed throttle, separate from the app-lock PIN lock. Five failed attempts produce a bounded 60-second lockout. A successful login resets only the local-login throttle; app-lock state is managed by its own policy. Failed login responses do not distinguish an unknown username from a wrong password.

## Release acceptance

Any future change that serializes `User`, extends `CloudSyncPolicy`, adds a backup table, or changes restore mapping must include a negative test proving that credential verifiers are still excluded. Any release that removes the legacy app-lock fallback must provide a tested recovery path for installations that have not opened the app during the migration window.
