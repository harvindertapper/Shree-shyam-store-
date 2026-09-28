# ZenMart — current engineering and release status

**Reviewed:** 27 September 2026

**GitHub main:** `e7dfa51d79258dc5fc7ce89ddb55d2400075a479` (PR #83 merged)

**Application ID and namespace:** `com.sevenzenlabs.zenmart`

**Release assessment:** development build; controlled-pilot gates remain open.

This page describes checked source, not a deployed service or installed merchant release. The implementation backlog and pending-PR decisions are in [the v1 roadmap](ZENMART_V1_PR_AND_ISSUE_ROADMAP.md) and [tracker #44](https://github.com/harvindertapper/Shree-shyam-store-/issues/44).

## Present on main

- Kotlin/Compose app with Room-backed catalog, inventory, checkout, cash/UPI/Udhaar recording, customer balances, stock adjustment, basic reports, barcode scanning, PDF/CSV sharing, Hindi/English copy and local lock/authentication.
- Local transactional commerce, persistent sync outbox, WorkManager retry, direct Firestore business sync, and snapshot backup/restore code. Source capability is not multi-phone or recovery proof.
- Room schema v11, migrations and focused tests, Android CI, minified release configuration, and ZenMart package migration. The app has min SDK 24, compile SDK 36.1, release cloud-sync flag true and debug flag false.

## Missing or unverified

| Area | Current boundary | Issues |
| --- | --- | --- |
| Cloud environments | A `.staging` app ID exists for a local-only staging build. Matching separate Firebase configurations and deployed staging/production Worker + D1 are not evidenced. | [#85](https://github.com/harvindertapper/Shree-shyam-store-/issues/85) |
| Multi-phone authority | `FirebaseSyncService` still uses Firestore directly. Worker/D1 prototype `f13e636` is local, outside main, and Android is not wired to it. | [#86](https://github.com/harvindertapper/Shree-shyam-store-/issues/86), [#87](https://github.com/harvindertapper/Shree-shyam-store-/issues/87) |
| Offline conflict | Local checkout guards tracked stock against negative stock. Two-phone last-unit sale and owner shortage reconciliation are unproven. | [#88](https://github.com/harvindertapper/Shree-shyam-store-/issues/88) |
| Recovery | Existing sync/snapshot path is not an independent encrypted, read-back-verified SAF export or proven clean-device restore. | [#89](https://github.com/harvindertapper/Shree-shyam-store-/issues/89) |
| Old shop data | New app ID gets separate private storage; actual old installs, signatures, exports and balances have not been inventoried or migrated. | [#84](https://github.com/harvindertapper/Shree-shyam-store-/issues/84) |
| Returns/reports | Refund states exist, but linked quantity-limited returns do not. Partial refunds are reported gross; English subtitle still says “Sales & profit.” | [#90](https://github.com/harvindertapper/Shree-shyam-store-/issues/90), [#91](https://github.com/harvindertapper/Shree-shyam-store-/issues/91) |
| Truthful status/cost | Home can show “Cloud backup active” without a verified backup. Free-tier usage and enrollment thresholds are not operationally proven. | [#58](https://github.com/harvindertapper/Shree-shyam-store-/issues/58), [#93](https://github.com/harvindertapper/Shree-shyam-store-/issues/93) |
| Release evidence | No signed pilot APK, physical two-phone rehearsal, five-shop pilot or clean-device recovery decision is recorded. | [#57](https://github.com/harvindertapper/Shree-shyam-store-/issues/57), [#92](https://github.com/harvindertapper/Shree-shyam-store-/issues/92) |

The local Worker/D1 prototype has owner/store/device bootstrap, role and token checks, invitations/revocation, payload allowlists, deduplication and cursor reads with local tests. It still needs current-main review, staging deployment, concurrent D1 proof, stock/Udhaar projections, Android integration and operations. Do not call it a shipped backend.

Payment status is merchant-entered, not bank settlement verification. Calculated profit needs a sale-time cost basis and return accounting. A sale stored only on a lost phone before server upload or independent export cannot be recovered; the app must show that risk.

To change this status, record exact commit/CI run, signed APK checksum/signing fingerprint, version, staging services, physical device/API, test store, migration/restore counts and balances, failure results, free-tier usage, incidents and owner go/no-go. Merged PR or green CI alone does not establish production readiness.
