# FR-X-SOURCE-DOC-REPO-HYGIENE Acceptance

> Historical/Superseded as of 2026-07-02. This acceptance note recorded the post-`b7d92f2` Room v2/Firebase-not-yet-implemented cleanup state. Current source-of-truth status is Room v4, Firebase Auth as runtime owner identity direction, FR-G local implementation in progress, and professional delivery routing through `docs/IMPLEMENTATION_PLAN.md` plus `docs/superpowers/plans/2026-07-02-professional-delivery-plan.md`.

## Decision

PASS.

## Baseline

- Branch: `harry/phase-1-localization-baseline`
- HEAD: `b7d92f2a0217e2a70f812bae03725715d3d67a78`
- Baseline commit: `b7d92f2 build: rename app identity to harrylabs package`

## Current Source Of Truth

- Final application id, namespace, Kotlin package, Firebase Android app identity, and Play Store identity: `com.harrylabs.shreeshyamstore`.
- Completed checkpoints:
  - `FR-A`: `0bd189b`
  - `FR-C`: `f36d613`
  - `FR-B`: `4bb4927`
  - `FR-P`: `b7d92f2`
- Room is schema version 2.
- Room v1 may reset through `fallbackToDestructiveMigrationFrom(true, 1)` because no real inventory existed. Room v2 onward requires intentional migrations or a new explicit owner-approved reset.
- The hybrid architecture is accepted: Firestore becomes canonical over time while Room remains the local working cache/offline store.
- Firebase Auth, Credential Manager Sign in with Google, Firestore integration, cloud restore, rules deployment, and App Check are mandatory but not implemented.
- Remaining route: Firebase prerequisites; Auth; owner/shop/membership restore; rules/App Check/cost controls; Product UI; product/category/settings sync and inventory restore QA; weighted Billing UI; persistence and billing restore QA.
- The welcome sound remains a separate manual-QA backlog item.

## Files Modified

- `AGENTS.md`: corrected Room/auth state and current task routing.
- `APP_BUILD_CHECKLIST.md`: recorded final identity and implemented Room v2 state.
- `PROJECT_CONTEXT.md`: replaced stale package, source paths, Room version, migration, Firebase, and next-task claims.
- `docs/PRODUCT_SPEC.md`: separated accepted foundation decisions from genuine owner decisions.
- `docs/IMPLEMENTATION_PLAN.md`: marked completed FR checkpoints and established the remaining FR route.
- `docs/DATA_MODEL.md`: marked implemented Room v2 fields and accepted cloud/cache boundaries accurately.
- `docs/governance/01_PROJECT_CHARTER.md`: removed settled identity and architecture items from owner-gated decisions.
- `docs/governance/03_DECISION_LOG.md`: added checkpoint evidence and superseded old identity/migration entries without deleting history.
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`: added modern Firebase module, Credential Manager, App Check, budget-alert, and conflict requirements.
- `docs/governance/FOUNDATION_RESET_DM004.md`: recorded checkpoint commits and current foundation state.
- `docs/governance/FIREBASE_CLOUD_SYNC_ARCHITECTURE.md`: corrected app identity, accepted-state wording, implementation guidance, and unresolved decisions.
- `docs/governance/MANUAL_QA_BACKLOG.md`: corrected source path, local-auth wording, FR routing, and settled base-unit decisions.
- `docs/governance/task-packets/FR-G-FIREBASE-AUTH-SHOP-PROFILE.md`: routed Auth through Credential Manager and Firebase main modules.
- `docs/governance/task-packets/FR-G2-FIRESTORE-RULES-APP-CHECK-COST-GUARDRAILS.md`: clarified development/production App Check and cost monitoring.
- `docs/governance/task-packets/FR-A-V2-DATA-CALC-DESIGN.md`: marked completed at `0bd189b`.
- `docs/governance/task-packets/FR-C-QUANTITY-PRICE-CALCULATOR.md`: marked completed at `f36d613`.
- `docs/governance/task-packets/FR-B-ROOM-V2-RESET.md`: marked completed at `4bb4927`.
- `docs/governance/task-packets/FR-P-APP-IDENTITY-RENAME.md`: marked completed at `b7d92f2`.
- `docs/governance/module-acceptance/M01-LOCALIZATION-BASELINE-001.md`: marked historical and corrected current migration/routing notes.
- `docs/governance/review-checklists/M01-LOCALIZATION-BASELINE-001.md`: marked historical.
- `docs/governance/task-packets/M01-LOCALIZATION-BASELINE-001.md`: marked historical.
- `docs/governance/task-packets/M01-LOCALIZATION-BASELINE-001-REMEDIATION.md`: marked historical.
- `docs/governance/task-packets/M01-LOCALIZATION-BASELINE-001-CONNECTED-REMEDIATION.md`: marked historical.
- `docs/governance/task-packets/M02F-A-FIREBASE-ARCHITECTURE-DECISION-001.md` through `M02F-H-FIRST-IMPLEMENTATION-SLICE-001.md`: marked historical/superseded and routed to the FR sequence.
- This acceptance record was added.

## Historical Files

The M01 acceptance/checklist/packets and M02F-A through M02F-H packets are retained as historical evidence. Their prominent headers state that they must not be executed. Legacy package names and old command examples inside them are classified as historical, not current instructions.

Completed FR-A, FR-C, FR-B, and FR-P packets are retained and marked `Completed - do not rerun`. Pre-FR-P paths in FR-B and FR-C remain historical execution evidence.

## Deliberately Preserved Files

- `README.md`: pre-existing user change; inspected and already aligned with current product/Firebase state.
- `docs/DELIVERY_WORKFLOW.md`, `docs/SCREEN_FLOW.md`, `docs/governance/02_SCOPE_CONTROL.md`, `docs/governance/04_DEFINITION_OF_DONE.md`, and `docs/governance/06_MODULE_ACCEPTANCE_README.md`: inspected; no FR-X correction required.
- `app/build.gradle.kts`: unchanged. Its debug build type has no explicit signing assignment and therefore uses AGP default debug signing. The unused `debugConfig` declaration was deliberately left unchanged to avoid an unnecessary Gradle edit/build.
- `.gitignore`: inspected by scope; no misleading entry required correction.

## Pre-Existing Dirty And Untracked State

Not created or changed by FR-X:

- Modified: `README.md`, `gradle/libs.versions.toml`.
- Untracked: `.idea/**`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`.
- Untracked source/governance documents present before FR-X: `PROJECT_CONTEXT.md`, `docs/DELIVERY_WORKFLOW.md`, `docs/SCREEN_FLOW.md`, `docs/governance/01_PROJECT_CHARTER.md`, `02_SCOPE_CONTROL.md`, `04_DEFINITION_OF_DONE.md`, `06_MODULE_ACCEPTANCE_README.md`, the M01 review/task documents, the M02F task documents, and the FR-X task packet.

FR-X did not delete, stage, commit, or push any of these files.

## Scan Classification

- Legacy package scan:
  - Current valid instruction: the FR-X packet names legacy values only as cleanup targets and scan patterns.
  - Historical evidence: labelled M01/M02F documents, M01 acceptance, and completed FR-B/FR-C/FR-P packets.
  - Unresolved defects: none.
- Broad destructive-migration scan:
  - Current valid instruction: active docs prohibit broad production destructive migration.
  - Historical evidence: labelled M02F-F wording.
  - Unresolved defects: none; current implementation is the v1-only reset.
- Cloud-deferred/local-only scan:
  - Current valid instruction: accepted/superseded decision-log history and the rejected local-only architecture alternative.
  - Historical evidence: labelled M02F-F migration-state wording.
  - Unresolved defects: none.
- Deprecated Firebase/Auth scan:
  - Matches only the FR-X prohibition/verification command and this report's classification note.
  - No active guidance recommends deprecated KTX modules or legacy `GoogleSignInClient`.
- Mojibake scan:
  - Matches only the FR-X packet's explicit examples and verification pattern.
  - No touched source-of-truth prose contains unresolved mojibake.

## Verification

- Branch and HEAD matched the packet baseline.
- `app/build.gradle.kts` contains:
  - `namespace = "com.harrylabs.shreeshyamstore"`
  - `applicationId = "com.harrylabs.shreeshyamstore"`
- `GreetingScreenshotTest` uses Robolectric SDK 34 for Java 19 compatibility; compile/target SDK remain unchanged.
- `git diff -- app/src` returned no diff.
- `git diff -- app/build.gradle.kts` returned no diff.
- `git diff --check` passed; only line-ending conversion warnings were reported.
- Gradle was not run because FR-X changed documentation only.

## Dependency Impact

No dependency or plugin version was changed by FR-X. `gradle/libs.versions.toml` already contained a pre-existing AGP `9.1.1` to `9.2.1` diff and was not edited by this task.

## Security And Privacy Impact

- No Firebase config, SDK implementation, rules deployment, service-account key, signing key, credential, secret, customer data, or production data was added.
- No Room schema, app behavior, UI, resource, manifest, repository, ViewModel, DAO, test, or business-logic file was changed.

## Remaining Owner Decisions

- Firebase project id and dev/prod environment policy.
- Firestore location.
- SHA-1/SHA-256 registration.
- `google-services.json` handling.
- Auth provider rollout details beyond Credential Manager Sign in with Google first.
- App Check enforcement timing.
- Cost monitoring/budget guardrails.
- Production local-credential hardening, Android backup policy, retention/export policy, release icon/signing, and invoice legal/tax fields.

## Next Recommended Task

Execute the Firebase project/config prerequisites, then implement `FR-G-FIREBASE-AUTH-SHOP-PROFILE` with Android Credential Manager Sign in with Google. Do not create another docs-cleanup packet.
