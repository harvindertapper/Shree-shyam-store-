# Shree Shyam Store — Finish and Launch Plan

**Status:** controlled merchant pilot preparation  
**Baseline:** `main` at `b00ec95`  
**Owner:** 7Zen Labs / product owner  
**Target:** evidence-based staged Android launch, not a blind public release

## 1. Product boundary

The repository is an Android Merchant OS app. It is not a website or SaaS control plane. The immediate goal is to finish and launch the merchant app safely. A public marketing website and a server-authoritative control plane should be planned as separate deliverables after the pilot proves merchant value.

**Sharp promise:** Offline billing, stock, and Udhaar for small Indian retailers.

## 2. Definition of done

The product is launch-ready only when all of the following are true:

- A production application ID and Firebase project mapping are approved and documented.
- A signed, reproducible AAB is built by protected CI from an immutable tag.
- Artifact checksum, version, mapping file, source commit, and signing provenance are retained.
- Clean install, upgrade, offline sale, interrupted sync, sync recovery, authenticated backup, restore, recovery point, and rollback pass on real devices.
- No known P0 financial, credential, migration, authorization, or data-loss defect remains.
- Sync UI distinguishes last attempt, last success, pending, retry, conflict, and failure; scheduling work is never shown as completed synchronization.
- Privacy-safe crash monitoring, release health alerts, support ownership, and incident/rollback runbooks are active.
- Five to ten pilot merchants can complete setup and create a first bill without developer assistance.
- Privacy policy, terms/support route, store listing, screenshots, and pilot support process are ready.

## 3. Execution sequence

### Phase 0 — Baseline and working agreement (now)

- Keep `main` protected; use one focused branch and PR per slice.
- Treat the existing security, commerce, migration, backup, and restore contracts as non-negotiable.
- Review Dependabot updates separately from product changes.
- Decide whether the source repository is public, private, or public with a reduced surface area.

**Exit evidence:** approved product boundary, owner list, pilot tenant policy, and this plan checked into the repository.

### Phase 1 — Finish the product foundation

#### A. Release identity — P0

- Choose the 7Zen Labs package ID and final display name.
- Create/verify a production Firebase project whose Android client matches that package ID.
- Define versionCode/versionName policy and release branch/tag policy.
- Move signing to protected CI secrets; fail release builds when signing is absent.
- Retain checksums, R8 mapping, commit SHA, and build metadata as release artifacts.

#### B. Truthful sync and backup health — P0

- Separate sync from backup state.
- Expose: signed-in/configured, online, automatic sync enabled, last attempt, last success, pending, in-flight, retryable failure, dead-letter count, conflict count, and next action.
- Lifecycle-manage connectivity callbacks or make WorkManager constraints the single source of truth.
- Add stale-status, process-restart, offline/online, retry, dead-letter, and conflict tests.

#### C. Merchant UX polish — P1

- Keep the current warm, high-contrast visual system, but make dark mode coherent and reduce ambiguous cloud-status language.
- Review onboarding, Home, Billing, Products, Udhaar, Reports, and Settings on small and low-end devices.
- Preserve Hindi/English parity, 48dp touch targets, readable ₹ formatting, clear empty/loading/error states, and accessible labels.
- Capture before/after screenshots and use them in the pilot checklist.

#### D. Release and recovery rehearsal — P0

- Run the signed candidate on clean install and upgrade paths.
- Exercise first-launch setup, local login/app lock, product creation, opening stock, billing, returns/corrections, Udhaar, reports, offline use, sync interruption/recovery, backup, restore, recovery point, and rollback.
- Cover representative low-end hardware and older supported Android versions, not only API 35/36.

**Exit evidence:** release candidate checklist with device/build identifiers, logs, screenshots, pass/fail outcomes, and known limitations.

### Phase 2 — Controlled merchant pilot

Recruit 5–10 real shopkeepers with a non-production tenant policy and a named support owner.

Measure only privacy-safe milestones:

- Setup completed.
- First product saved.
- First bill generated.
- First Udhaar payment recorded.
- Backup configured.
- Return usage after 7 and 30 days.
- Support requests, task failures, and recovery outcomes.

Never export names, phone numbers, ledger data, credentials, tokens, or raw financial payloads. Use tenant-scoped counters or local/manual pilot notes until the telemetry boundary is reviewed.

**Pilot exit:** every merchant can complete setup and first bill unaided; P0/P1 failures have owners and deadlines; data-loss, sync-truth, restore, and crash issues block progression.

### Phase 3 — Staged launch

1. Internal distribution to the release owners.
2. Closed Play testing with the pilot cohort.
3. Small production percentage with monitoring and rollback owner on call.
4. Expand only after a defined observation window with no release-blocking regression.
5. Publish a public product story and support route after the pilot evidence is positive.

## 4. Workstream order

| Order | Workstream | Priority | Evidence |
| --- | --- | --- | --- |
| 1 | Release identity and protected signing | P0 | Signed AAB, tag, checksum, mapping, provenance |
| 2 | Truthful sync/backup health | P0 | Unit tests, UI states, stale/retry/conflict rehearsal |
| 3 | Credential and app-lock compatibility sunset | P0 | KDF migration and negative/migration tests |
| 4 | Release candidate and device rehearsal | P0 | Clean/upgrade/offline/recovery/rollback record |
| 5 | Monitoring, support, and incident operation | P0 | Alert test, ownership, support route, rollback runbook |
| 6 | Merchant UX and onboarding polish | P1 | Hindi/English screenshots and pilot task results |
| 7 | Privacy-safe activation and feedback | P2 | Event taxonomy, consent/disable policy, retention review |
| 8 | Public website/control plane | Future | Separate repo and architecture decision |

## 5. PR definition of done

Every implementation PR must:

- Start from updated `main` and stay focused.
- Include relevant unit/Robolectric/instrumentation coverage.
- Pass `git diff --check`, secret/artifact scan, lint, debug build, and stable tests.
- Preserve Room schema and add a migration plus migration test when needed.
- Document release, rollback, recovery, localization, and accessibility impact.
- Include screenshots for user-facing changes.
- State what could not be verified in CI or the local environment.

## 6. Launch decision record

**Go** only if every Definition of Done item is evidenced.  
**Pilot-only** if the app is useful but release, monitoring, or recovery evidence is incomplete.  
**No-go** for any unresolved P0 financial, credential, migration, authorization, sync-truth, or data-loss defect.

The owner should record the final decision against a specific tag, commit SHA, artifact checksum, pilot window, and rollback owner. A passing CI build alone is not a production launch.
