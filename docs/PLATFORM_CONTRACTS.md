# Shared Platform Contracts

**Status:** P0 foundation contract, implemented on `feat/p0-platform-contracts` for review  
**Scope:** Merchant Android OS, future SaaS Control Plane, and future pickup marketplace

## Purpose

The Android app, Admin Panel, and Marketplace must not invent separate representations for tenant scope, actor identity, payment state, idempotency, or availability. This document defines the first shared contract layer. It deliberately introduces domain types and stable wire values before adding organization/store columns to Room, because the schema migration and server-authoritative tenant context require a separate, fully tested slice.

## Contract boundaries

| Contract | Current owner | Future consumer |
|---|---|---|
| `TenantScope` | Android/domain contract only; not yet persisted in all Room rows | Control Plane API authorization, PostgreSQL tenant keys, marketplace store scope |
| `PlatformActor` | Local actor/audit metadata | Admin audit events, server command actor, support actions |
| `CommandMetadata` | Integration metadata | API idempotency, outbox commands, webhook/retry correlation |
| `PaymentState` | Shared wire enum | Payment reconciliation, gateway callbacks, admin finance views |
| `MarketplaceAvailability` | Shared presentation/domain enum | Consumer catalogue and store readiness views |
| `PlatformEventType` | Shared event vocabulary | Sync, analytics, support, audit, and Control Plane workers |
| `PlatformEventMetadata` | Versioned event metadata | API/event envelope; payload remains domain-specific and allowlisted |

## Non-negotiable rules

Every future server command must include or derive tenant scope, actor identity, device/install identity, an idempotency key, a client event ID, and a schema version. The server must verify the effective organization and store from authenticated membership; it must never trust a client-provided role, entitlement, price, stock balance, or store scope.

The payload for a platform event is domain-specific and must be serialized through an explicit allowlist. `User.passwordHash`, local passwords, PIN verifiers, access tokens, biometric state, and provider secrets are never valid event or backup payload fields.

`MarketplaceAvailability` is intentionally not a direct mirror of `Product.currentStock`. A future marketplace must communicate freshness and certainty: an item may be available, likely available, require confirmation, or be temporarily unavailable. Reservation and order acceptance are server-side concerns and are not introduced in this Android-only contract slice.

## Versioning and compatibility

`PlatformEventMetadata.CURRENT_SCHEMA_VERSION` starts at `1`. New fields should be additive and nullable/defaulted during migration. Removing or renaming a field requires a compatibility window in which old Android versions and new Control Plane versions can coexist. Every cross-repository contract change needs an API/event version, migration note, backward-compatibility test, and rollback plan.

Local Room rows continue using their existing schema and stable sync metadata in this slice. Organization/store/device persistence will be introduced only through an explicit Room migration, migration fixture, tenant-context resolver, and negative authorization tests.

## Initial future event mapping

| Event | Meaning | Authoritative source |
|---|---|---|
| `PRODUCT_UPSERTED` | Product identity or catalogue data changed | Merchant repository/server command |
| `STOCK_MOVED` | Stock ledger movement or audited adjustment | Atomic inventory transaction |
| `SALE_COMMITTED` | Local/server sale committed | Checkout transaction/server receipt |
| `PAYMENT_RECORDED` | Payment state or reference recorded | Payment command/reconciliation |
| `UDHAAR_EVENT_RECORDED` | Immutable credit/payment/correction event appended | Ledger transaction |
| `SYNC_FAILED` | Sync command rejected or exhausted retries | Outbox/Control Plane response |
| `RESTORE_COMPLETED` | Validated replacement completed | Restore transaction and audit record |

The event name alone is not a financial source of truth. The authoritative transaction, event ID, idempotency receipt, actor, tenant scope, and reconciliation status must remain queryable.
