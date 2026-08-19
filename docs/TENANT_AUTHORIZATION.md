# Tenant Authorization Boundary

## Purpose

The Merchant OS must not rely on Compose visibility or caller-provided identifiers to authorize business mutations. PR #33 places a pure command policy at the `ShopRepository` boundary and binds every protected mutation to the persisted local tenant context and authenticated session.

## Trusted inputs

A command contains `TenantScope`, `PlatformActor`, an idempotency key, a client event ID, and a client-created timestamp. The repository does not trust those values by themselves. It resolves the expected tenant and actor from `SettingsDataStore` and `TenantDeviceContext`, then compares the command against that trusted state before touching Room.

Local legacy mappings remain explicitly namespaced. They are local compatibility identities, not server enrollment claims and not substitutes for future Control Plane authorization.

## Capability matrix

| Capability | Allowed roles | Protected operations |
|---|---|---|
| `CATALOG_WRITE` | Owner, Manager | Categories, products, customers |
| `CHECKOUT` | Owner, Manager, Cashier | Cash, UPI, and Udhaar checkout |
| `PAYMENT_RECONCILIATION` | Owner, Manager | Payment lifecycle reconciliation |
| `LEDGER_RECORD` | Owner, Manager, Cashier | Udhaar payment records |
| `LEDGER_CORRECTION` | Owner, Manager | Udhaar reversal and correction |
| `INVENTORY_ADJUSTMENT` | Owner, Manager | Stock counts and stock audit writes |

## Rejection rules

The repository rejects wrong organization, wrong store, wrong membership, mismatched actor identity, mismatched actor/device scope, actor-role spoofing, unsupported roles, stale commands, and commands issued too far in the future. Authorization runs before the mutation transaction. Existing Room transactions remain responsible for atomic checkout and ledger behavior, so an authorization failure cannot create partial rows.

Commands older than five minutes are rejected, and client timestamps more than thirty seconds in the future are rejected. These values are local offline-first guardrails; the future Control Plane will add server-issued membership and policy decisions.

## Privacy boundary

Command metadata contains identity and scope metadata only. It must never contain passwords, PIN verifiers, session secrets, Firebase tokens, or raw payment details. Existing `CloudSyncPolicy` and backup allowlists remain unchanged.

## Follow-up

The next Control Plane integration should replace legacy local scope placeholders with authenticated organization membership and device enrollment, while retaining repository-level authorization as defense in depth.
