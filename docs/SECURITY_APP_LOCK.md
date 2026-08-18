# App-Lock Security Contract

**Status:** Implemented on `feat/security-app-lock-hardening` for review

## Security invariants

The app-lock gate is enforced at the ViewModel/DataStore boundary rather than only in Compose. PIN verification is serialized through one DataStore edit, so concurrent taps cannot race the attempt counter. Five failed attempts start a 30-second cooldown. A locked state rejects both correct and incorrect PIN attempts until the cooldown expires; a successful PIN or strong biometric unlock clears the failure counter and records the successful unlock time.

The inactivity timeout is 15 minutes from the last successful unlock. Returning from background routes to the PIN screen only when app lock is enabled and the timeout has expired. The timeout does not log out the Firebase or local identity; it gates access to the store UI while preserving the explicit identity session contract.

## PIN policy

New or changed four-digit PINs must not be the historical default `1234`, its reverse, a repeated digit, or an obvious ascending/descending sequence. First-run setup no longer pre-fills a default PIN. Existing installations retain a narrow compatibility path for legacy plaintext/blank PIN preferences so that an owner can open the app and replace the old value; all newly saved PINs are SHA-256 digests. A future credential migration should replace this compatibility path with a slow, salted password-specific verifier and an explicit recovery flow.

## Biometric policy

Biometric unlock is enabled only when `BiometricManager` reports `BIOMETRIC_STRONG` availability. The prompt explicitly requests strong biometrics, never silently falls back to a weak modality, and handles cancellation, recognition failure, hardware unavailability, and prompt errors without navigating to the store. The PIN remains an explicit fallback through the negative prompt action and keypad. A successful biometric unlock resets the same app-lock failure state and timeout clock as a successful PIN.

## Data boundary

Attempt counters, cooldown expiry, last successful unlock time, the PIN verifier, and biometric-enabled flag remain in device-local DataStore. None are included in Firestore business sync, sync outbox payloads, manual REST backup, logs, analytics, or crash reports. The identity session and app-lock state are separate concerns: locking the UI does not clear the authenticated Firebase/local session, while logout still clears the session through the existing identity flow.

## Tests and release limitations

`AppLockPolicyTest` covers the five-attempt cooldown, blocked attempts, cooldown expiry, reset on success, timeout boundary, and rejection of weak new PINs. The existing `SecurityUtilsTest` continues to cover hash verification and legacy compatibility. CI includes `AppLockPolicyTest` as a required stable security gate. This slice does not yet implement rate limiting for account registration/login, slow password-KDF migration, remote account recovery hardening, or domain-level role authorization; those remain separate production gates.
