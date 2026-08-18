# Identity and Session Model

**Status:** Implemented on `feat/identity-model-unification` for review

## Purpose

The application previously treated three independent signals as interchangeable: Firebase Auth state, the local Room `User` table, and a DataStore `isUserLoggedIn` flag with UID/email/username fallbacks. That design could display one account while routing sync, backup, or audit activity through another identity. This document defines the explicit authority contract used by the identity-unification slice.

> **One active session has one authority. Firebase and local credentials are never silently combined.**

## Source-of-truth matrix

| Concern | Authoritative source | Persisted representation | Offline behavior |
| --- | --- | --- | --- |
| Firebase authentication | Firebase Auth `currentUser.uid` | `IdentitySession(provider = FIREBASE, uid, email, username, role)` in DataStore | A Firebase session is invalidated locally when the matching Firebase account is unavailable |
| Local authentication | Device-local Room `User` credentials | `IdentitySession(provider = LOCAL, uid = local:<sha256(email)>, username, email, role)` in DataStore | Remains usable offline; local credentials never leave the device |
| Active session routing | Resolved `IdentitySession` | Explicit `identity_provider` plus session fields in DataStore | Startup, logout, ledger actors, sync, backup, and restore use the same session |
| Ledger actor | Resolved session plus DataStore audit device ID | `LedgerActor` fields on immutable events | Mutations remain attributable while offline |
| Sync namespace | `IdentitySession.shopUid` | Hashed namespace derived from the stable session UID | No UID/email/username fallback chain or `default_store` namespace |
| Cloud business restore | Resolved session namespace | Device-local users/profile are preserved by repository restore policy | A missing or mismatched session fails before remote replacement |

## Compatibility migration

Older DataStore sessions do not contain `identity_provider`. During read-time compatibility resolution only, a non-empty legacy UID is classified as Firebase-backed and a session with no UID but a local username/email is classified as local. Local sessions receive a deterministic `local:<sha256(normalized email)>` UID. The reconciler persists the explicit provider once the session is successfully resolved; it does not silently migrate credential material or upload local users.

Existing local `User` rows remain a device-local projection. Newly registered local users receive the deterministic local UID before insertion. Firebase sign-in creates or reuses a local projection with the Firebase UID, but the Room row is not an authentication authority and user credential fields remain outside cloud business payloads.

## Reconciliation rules

At application startup, `ShopViewModel.reconcileIdentitySession()` performs these checks in order:

1. An explicit local session is accepted as the offline authority. Any local-auth flow clears cached Firebase credential state before persisting the local session.
2. If Firebase exposes an account, the reconciler constructs a Firebase session from that account and persists it when the stored Firebase session is missing or belongs to another Firebase UID.
3. If a stored Firebase session has no matching Firebase account, the session is cleared rather than falling back to a local email or username.
4. A legacy local session is normalized to a stable local UID and persisted with `provider = LOCAL`.
5. No usable session produces no authenticated route, no sync, and no backup/restore namespace.

The top-level router navigates from the reconciled session, not from `firebaseUser != null || settings.isUserLoggedIn`. `SyncWorker` refuses to sync when there is no valid session or when a Firebase session UID differs from Firebase Auth’s current UID.

## Security boundaries

Local password verifiers, PIN verifiers, Firebase ID tokens, credential-manager state, and session secrets are device-only. They must never appear in Firestore documents, REST backup payloads, sync outbox JSON, logs, analytics, crash reports, or business-data restore snapshots. The identity provider is a DataStore session concern, not a cloud business entity.

This slice makes authority explicit but does not claim that local SHA-256 passwords or the legacy/default PIN policy are production-safe. Rate limiting, slow password KDF migration, app-lock hardening, and domain-level role/permission enforcement remain separate follow-up slices.

## Test and rollback expectations

`IdentitySessionTest` covers deterministic local identity, normalization, explicit-provider requirements, and logout-like empty state. The repository’s existing privacy, restore, ledger, and sync tests remain required CI gates. If identity reconciliation causes startup or offline risk, the focused PR can be reverted without changing the Room schema; the old DataStore fields remain available for compatibility and no destructive migration is involved.
