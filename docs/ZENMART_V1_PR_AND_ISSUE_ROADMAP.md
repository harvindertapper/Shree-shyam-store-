# ZenMart v1 — issue and pull request roadmap

**Checked baseline:** GitHub `main` `e7dfa51d79258dc5fc7ce89ddb55d2400075a479` on 27 September 2026. PR #83 is merged and `com.sevenzenlabs.zenmart` is the application ID. This is a controlled-pilot plan, not a release-ready claim. See [current status](CURRENT_PRODUCTION_STATUS.md) and [tracker #44](https://github.com/harvindertapper/Shree-shyam-store-/issues/44).

## Product and cost decisions

Keep Kotlin, Compose, Room and WorkManager. Room's offline transactional checkout is valuable; a rewrite adds migration and reliability risk without measured benefit. Firebase Authentication remains for individual sign-in. A Worker + D1 event service is the planned server authority for membership, device access, idempotency and recovery history. Direct Firestore business sync must be replaced in a coordinated cutover, never mixed with the new protocol for one store.

Every enrolled phone may bill offline. A bill and its immutable upload event must be durable together. The server accepts two valid offline sales of the same last unit, retains both bills and flags negative stock for owner adjustment with a reason. Offline stock must be labelled potentially stale. Sync, server history and encrypted owner-controlled exports are separate recovery layers. A bill stored only on a lost phone before upload or export cannot be recovered.

Use separate staging and production Firebase and Cloudflare free-plan resources. Free tier has hard limits and provider terms can change. Measure requests, D1 rows/storage, pending-event age, export age and failures. Pause new enrollment before capacity is exhausted. Local billing continues through quota/outage with visible unsynced risk. Revenue-funded scaling is a later decision based on pilot retention, support cost and actual usage. Supplier purchasing, GST-specific invoicing, marketplace, AI features and calculated profit are outside v1.

## GitHub implementation backlog

| Order | Issue | Acceptance focus |
| --- | --- | --- |
| 0 | [#57 signed release](https://github.com/harvindertapper/Shree-shyam-store-/issues/57), [#58 truthful status](https://github.com/harvindertapper/Shree-shyam-store-/issues/58) | Existing issues revised for post-#83 state; remove false status claims and define artifact evidence. |
| 1 | [#84 old-data migration](https://github.com/harvindertapper/Shree-shyam-store-/issues/84), [#85 environment isolation](https://github.com/harvindertapper/Shree-shyam-store-/issues/85) | Inspect real installs/signers/exports; provision distinct staging/production IDs and services. |
| 2 | [#86 event API](https://github.com/harvindertapper/Shree-shyam-store-/issues/86) | Harden the ported Worker foundation with stable references, additional-device enrollment, quota controls, projections, concurrent replay and deployed D1 proof. |
| 3 | [#87 Android event adapter](https://github.com/harvindertapper/Shree-shyam-store-/issues/87) | Sale plus queue transaction, stable references, Worker push/pull, no mixed Firestore protocol. |
| 4 | [#88 stock reconciliation](https://github.com/harvindertapper/Shree-shyam-store-/issues/88) | Both last-unit offline bills survive; shortage and owner resolution are auditable. |
| 5 | [#89 encrypted recovery](https://github.com/harvindertapper/Shree-shyam-store-/issues/89) | SAF folder, recovery phrase, read-back verification, seven daily/four weekly copies and clean-device restore. |
| 6 | [#90 returns](https://github.com/harvindertapper/Shree-shyam-store-/issues/90), [#91 reports](https://github.com/harvindertapper/Shree-shyam-store-/issues/91) | Linked partial/full returns, quantity caps, stock/refund/Udhaar effects; no unsupported profit claim. |
| 7 | [#93 capacity/operations](https://github.com/harvindertapper/Shree-shyam-store-/issues/93), [#94 startup/theme polish](https://github.com/harvindertapper/Shree-shyam-store-/issues/94), [#59 privacy-safe activation](https://github.com/harvindertapper/Shree-shyam-store-/issues/59) | Visible ages/failures and free-tier thresholds; port useful old-draft UI changes; measure pilot safely. |
| Gate | [#92 two-phone and five-shop pilot](https://github.com/harvindertapper/Shree-shyam-store-/issues/92) | API 24/current CI, physical-phone failure matrix, migration/rollback, signed APK and 14-day owner go/no-go. |

#84 discovery and #85 environment setup can proceed together. #89 design can start before sync, but final restore proof must use the release-candidate identity. Fix the unsupported report label and false backup claim early even before their full feature issues finish.

## Pending pull request disposition

| PR | Current-main assessment | Action |
| --- | --- | --- |
| [#80 Compose BOM](https://github.com/harvindertapper/Shree-shyam-store-/pull/80) | CI fails: proposed Compose libraries require compile SDK 37; app uses 36.1. | **Hold.** Validate SDK/toolchain and API device matrix, then update/rebase as a focused dependency PR. |
| [#79 Roborazzi](https://github.com/harvindertapper/Shree-shyam-store-/pull/79) | CI green, but screenshot test is excluded from CI. | **Review for merge.** Run screenshot/golden check and inspect artifacts first. |
| [#78 audit doc](https://github.com/harvindertapper/Shree-shyam-store-/pull/78) | Draft based on old package and `b00ec95`; status claims are stale. | **Supersede/close after this docs PR.** Move any valid observation to a current issue. |
| [#77 startup gate](https://github.com/harvindertapper/Shree-shyam-store-/pull/77) | Useful auth-screen flash fix, but old `com.aistudio.shreeshyamstore.pqwzkb` package. | **Port selectively** to ZenMart under [#94](https://github.com/harvindertapper/Shree-shyam-store-/issues/94); reproduce and add bounded UI tests. Close old draft after replacement merges. |
| [#76 release workflow](https://github.com/harvindertapper/Shree-shyam-store-/pull/76) | Signed-build foundation, but old baseline and AAB-only output miss direct APK pilot; workflow inputs need safe handling before shell use. | **Rework** under [#57](https://github.com/harvindertapper/Shree-shyam-store-/issues/57): immutable ref, protected secrets, signed APK, checksum/provenance and environment separation. Close old draft after replacement merges. |
| [#75 launch foundation](https://github.com/harvindertapper/Shree-shyam-store-/pull/75) | Theme and connectivity fixes may help; README/launch claims and package context are stale. | **Split/port** current-code theme and callback fixes under [#94](https://github.com/harvindertapper/Shree-shyam-store-/issues/94) and [#58](https://github.com/harvindertapper/Shree-shyam-store-/issues/58). Refresh launch copy separately; close draft after replacement merges. |
| [#69 Credentials alpha](https://github.com/harvindertapper/Shree-shyam-store-/pull/69) | Alpha dependency upgrade has failing old checks and no v1 need established. | **Defer/close** unless current security or compatibility need is demonstrated; use a fresh PR then. |
| [#11 Room 2.8.4](https://github.com/harvindertapper/Shree-shyam-store-/pull/11) | Main already has Room 2.8.5 via #81. | **Close as superseded**; do not merge a downgrade. |
| [#9 Play Services Location](https://github.com/harvindertapper/Shree-shyam-store-/pull/9) | Dependency is commented out and no location workflow is in v1. | **Close as unnecessary**; add afresh only for a justified feature. |

Do not merge old drafts wholesale across the package migration. #75/#76/#77 should yield small PRs from current main, citing the source draft with current tests. Do not close a draft before useful replacement code is reviewable. #79 is the only green focused dependency candidate, but its excluded screenshot check still needs manual verification.

## Suggested PR sequence

1. **Docs/status and truth fixes:** this roadmap, #58 backup/status wording and #91 profit label. Keep behavior changes separate from docs.
2. **Environment and migration discovery:** #85 configuration boundaries and #84 actual-install inventory; establish signed APK identity and recovery path before moving merchant data.
3. **Server contract:** complete the Worker/D1 foundation for #86. Verify signed tokens, cross-store denial, revocation, stable references, concurrent replay and staging D1 before app traffic.
4. **Android outbox/cutover:** #87 stable IDs, Room migrations, atomic sale+event and Worker adapter. Upgrade whole test store together; retire Firestore business writes after fresh-device sync succeeds.
5. **Reconciliation/accounting:** #88 owner deficits, #90 returns, #91 report reconciliation. Preserve append-only sale/ledger history.
6. **Independent recovery/operations:** #89 encrypted SAF copies/restore, #58/#93 honest status and capacity alerting; rehearse quota outage.
7. **Release/pilot:** #57 signed APK workflow and #92 physical-phone staging plus 14-day pilot. Public launch waits for owner go/no-go and distribution/privacy review.

Every schema PR needs forward migrations and tests from supported deployed versions. Every API PR needs contract/version tests. CI must cover debug and minified builds, lint, domain/Room/API tests and Android API 24 plus a current API. Staging must record two real phones selling the last unit offline, duplicate retry, token expiry, quota failure, app death during checkout, failed upload, returns, cross-store denial, clean-device recovery and rollback. Record commit, artifact checksum, version, device/API, test store, outcomes, pending/export age, free-tier usage, incidents and owner decision. Passing CI or merging an issue is not a substitute for this evidence.
