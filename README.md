# Shree Shyam Store

Offline-first billing, stock, and Udhaar for small Indian retailers.

> **Status: controlled merchant pilot preparation**
>
> This repository is not yet a public production release. The current Android app is a strong staging candidate, but release identity, signed artifact provenance, operational monitoring, and real-device rehearsal must be completed before a public rollout.

## What the app does

- Create bills while offline.
- Track inventory, stock adjustments, and low-stock alerts.
- Record cash, UPI, and Udhaar sales in integer paise.
- Manage customer balances and payment history.
- Export reports and preserve local data through authenticated backup and recovery flows.
- Use Hindi or English merchant-facing UI.

## Product promise

**Offline billing, stock, and Udhaar for small Indian retailers.**

The product is intentionally Android-first. A separate web control plane or public website is not part of this repository yet; this repository is the Merchant OS app.

## Current status

The app has extensive commerce, authorization, migration, backup, restore, and CI coverage. It is still a **pilot candidate**, not a production-distributable release.

Before public launch we must complete:

1. Production package identity, Firebase mapping, versioning, and protected signing.
2. Truthful sync and backup health states.
3. Clean-install, upgrade, offline, recovery, and rollback rehearsal on representative devices.
4. Privacy-safe monitoring, support ownership, and incident runbook.
5. Merchant onboarding, privacy policy, support route, Play listing, and pilot evidence.

See [`docs/LAUNCH_PLAN.md`](docs/LAUNCH_PLAN.md) for the execution sequence and go/no-go evidence matrix.

## Development

The project uses Kotlin, Jetpack Compose, Room, WorkManager, Firebase, and Gradle Kotlin DSL.

```bash
./gradlew assembleDebug
./gradlew lintDebug
./gradlew testDebugUnitTest
```

Firebase configuration, keystores, local properties, customer data, and release credentials are machine-local and must never be committed.

## Engineering guardrails

- Room remains the offline source of truth.
- Money is represented as integer paise.
- Checkout, stock, ledger, and audit mutations remain atomic and duplicate-submit safe.
- Tenant, actor, membership, device, capability, and freshness checks stay at the repository/domain boundary.
- Credentials, PIN verifiers, bearer tokens, and raw authentication material stay device-local.
- Every production change starts from updated `main`, uses a focused branch and pull request, and includes deterministic verification evidence.

## License

A commercial/open-source licensing decision is still pending. Do not redistribute production builds until the owner publishes the final license and legal policies.
