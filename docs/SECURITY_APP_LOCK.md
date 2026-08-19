# App-Lock Security Contract

**Status:** Implemented and merged; current behavior is governed by `SecurityUtils`, `SettingsDataStore`, and the required app-lock tests.

## Security invariants

The app-lock gate is enforced at the ViewModel/DataStore boundary rather than only in Compose. PIN verification is serialized through one DataStore edit, so concurrent taps cannot race the attempt counter. Five failed attempts start a 30-second cooldown. A locked state rejects both correct and incorrect PIN attempts until the cooldown expires; a successful PIN or strong biometric unlock clears the failure counter and records the successful unlock time.

The inactivity timeout is 15 minutes from the last successful unlock. Returning from background routes to the PIN screen only when app lock is enabled and the timeout has expired. The timeout does not log out the Firebase or local identity; it gates access to the store UI while preserving the explicit identity session contract.

## PIN policy

New or changed four-digit PINs must not be the historical default `1234`, its reverse, a repeated digit, or an obvious ascending/descending sequence. First-run setup no longer pre-fills a default PIN. New or changed PINs are stored as versioned, per-record salted PBKDF2-HMAC-SHA256 verifiers with an explicit app-lock scope. Existing installations retain a narrow compatibility path for legacy plaintext, legacy SHA-256, or blank/default PIN preferences so an owner can unlock and replace the old value. This fallback is migration-only, is never available to local account passwords, and must be removed only through a release with tested recovery and support evidence.

## Biometric policy

Biometric unlock is enabled only when `BiometricManager` reports `BIOMETRIC_STRONG` availability. The prompt explicitly requests strong biometrics, never silently falls back to a weak modality, and handles cancellation, recognition failure, hardware unavailability, and prompt errors without navigating to the store. The PIN remains an explicit fallback through the negative prompt action and keypad. A successful biometric unlock resets the same app-lock failure state and timeout clock as a successful PIN.

## Data boundary

Attempt counters, cooldown expiry, last successful unlock time, the PIN verifier, and biometric-enabled flag remain in device-local DataStore. None are included in Firestore business sync, sync outbox payloads, manual REST backup, logs, analytics, or crash reports. The identity session and app-lock state are separate concerns: locking the UI does not clear the authenticated Firebase/local session, while logout still clears the session through the existing identity flow.

## Tests and release limitations

`AppLockPolicyTest` covers the five-attempt cooldown, blocked attempts, cooldown expiry, reset on success, timeout boundary, and rejection of weak new PINs. `SecurityUtilsTest` and `CredentialMigrationTest` cover scoped PBKDF2 records, legacy SHA-256 migration, cross-scope rejection, default-PIN compatibility, credential privacy, and local-login throttling. CI includes these tests as required stable security gates. The remaining release decision is when and how to remove the temporary app-lock fallback without stranding legacy offline users; remote account recovery hardening and domain-level role authorization remain separate production gates.
