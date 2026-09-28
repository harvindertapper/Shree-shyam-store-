# ZenMart control plane

This Worker is an authenticated append-only sync API foundation for the v1 pilot, not a deployed or release-ready backend. It verifies Firebase ID-token signatures and claims, active store membership, enrolled devices, role-specific write access, and a strict per-table scalar payload allowlist before appending events to D1. Udhaar audit actor UID, role and device must match server-authenticated membership. Replaying the same `(store,eventId,idempotencyKey)` is safe, including an identical event repeated within one batch. The event log deliberately accepts valid offline sales without serializing or rejecting them on stock availability; stock projections and owner-reviewed deficit adjustments are a follow-up layer.

## Local checks

Run `node --test` from this directory (or `npm test`). Install Wrangler locally when needed, then create separate D1 databases from `wrangler.toml` (staging) and `wrangler.production.toml` (production), replace each placeholder database ID/project ID, and apply migrations remotely. Set the Firebase project ID for each environment. Do not add service-account private keys: ID tokens are verified against Google's published Secure Token signing keys.

`POST /v1/stores` lets a verified Firebase account create a store and its first device. The owner can issue a role-limited, one-use invitation to a verified email; redemption creates that member's device. Only the owner can revoke a member or device. Invite tokens are returned once and only their SHA-256 digest is stored. Keep staging and production Worker/D1 bindings separate, and do not add a billing account to the free-tier pilot projects.

## API

- `POST /v1/stores/{storeId}/events` accepts `{events:[{eventId,deviceId,schemaVersion:1,idempotencyKey,eventType,payload,createdAt}]}`.
- `GET /v1/stores/{storeId}/changes?after={cursor}&deviceId={deviceId}` returns an ordered page of at most 100 append-only events and a monotonic cursor. The requesting installation must identify an active enrolled device.
- `POST /v1/stores` creates the initial owner/store/device. `POST /v1/stores/{storeId}/invites` creates a manager/cashier invite; `POST /v1/stores/{storeId}/invites/redeem` enrolls the matching verified account and returns its device ID.
- `DELETE /v1/stores/{storeId}/members/{uid}` and `DELETE /v1/stores/{storeId}/devices/{deviceId}` revoke access. Revoked membership blocks every store route; revoked devices cannot upload or download.
- Every business route requires `Authorization: Bearer <Firebase ID token>`. Membership and device enrollment come from D1, never request headers.

Operational limitations: the current event payload mirrors Room rows and still contains local integer relationship IDs; these are **not safe cross-phone references**. The stable-reference contract, per-event rejected results, rate limiting/usage alerts, stock and Udhaar projections, owner-approved additional-device enrollment, remote D1 concurrency tests, and Android event adapter are not implemented or verified. The local contract tests use a D1-shaped in-memory fixture; they do not prove a deployed Worker or D1. Keep this service out of release traffic until those controls are completed and exercised on staging devices. Neither staging nor production placeholder configuration is deployable as-is.
