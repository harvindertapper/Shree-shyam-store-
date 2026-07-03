# FR-X-SOURCE-DOC-REPO-HYGIENE

> Historical/Superseded as of 2026-07-02. This packet described the post-`b7d92f2` cleanup state when Room v2 was current and Firebase Auth/Firestore were not yet implemented. Current source-of-truth status is Room v4 with Firebase Auth as the runtime owner identity direction, FR-G local implementation in progress, and the professional delivery route tracked in `docs/IMPLEMENTATION_PLAN.md` plus `docs/superpowers/plans/2026-07-02-professional-delivery-plan.md`.

## task_id

FR-X-SOURCE-DOC-REPO-HYGIENE

## goal

Perform one repository-wide, behavior-neutral cleanup pass so all current source-of-truth documents, task routing, commands, package references, Room status, Firebase status, and repository notes accurately reflect the accepted Shree Shyam Store foundation state after commit `b7d92f2`.

## current_baseline

- Branch at packet creation: `harry/phase-1-localization-baseline`.
- Baseline commit: `b7d92f2 build: rename app identity to harrylabs package`.
- Final Android `applicationId`, namespace, Kotlin package, Firebase app identity, and Play Store identity: `com.harrylabs.shreeshyamstore`.
- `FR-A`, `FR-C`, `FR-B`, and `FR-P` are implemented and checkpointed.
- Room is schema version 2.
- The Room v1-to-v2 reset is narrowly implemented through `fallbackToDestructiveMigrationFrom(true, 1)`.
- Broad `fallbackToDestructiveMigration()` is not the current implementation and must not be described as current.
- Firebase Auth, Google Sign-In, Firestore integration, cloud restore, Firestore rules, and App Check are not implemented yet.
- The hybrid architecture is accepted: Firestore becomes canonical over time while Room remains the local working cache/offline store.
- Firebase/cloud recovery is mandatory MVP foundation work.
- Weight/volume calculator and Room v2 data foundation exist, but Product and Billing UI integration is pending.
- Dependency/version upgrades are explicitly excluded from this packet.

## read_first

Read these before editing:

- `AGENTS.md`
- `APP_BUILD_CHECKLIST.md`
- `README.md`
- `PROJECT_CONTEXT.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/DELIVERY_WORKFLOW.md`
- `docs/SCREEN_FLOW.md`
- `docs/DATA_MODEL.md`
- `docs/governance/01_PROJECT_CHARTER.md`
- `docs/governance/02_SCOPE_CONTROL.md`
- `docs/governance/03_DECISION_LOG.md`
- `docs/governance/04_DEFINITION_OF_DONE.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`
- `docs/governance/FOUNDATION_RESET_DM004.md`
- `docs/governance/FIREBASE_CLOUD_SYNC_ARCHITECTURE.md`
- `docs/governance/MANUAL_QA_BACKLOG.md`
- `docs/governance/module-acceptance/**`
- `docs/governance/review-checklists/**`
- `docs/governance/task-packets/**`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- Current Room/database and package declarations under `app/src/**`

If a document named above is untracked, it is still part of the audit. Do not assume untracked means disposable.

## scope_paths

The worker may edit only:

- `AGENTS.md`
- `APP_BUILD_CHECKLIST.md`
- `README.md`
- `PROJECT_CONTEXT.md`
- `docs/**/*.md`
- `app/build.gradle.kts`, only for stale comments or removal of an unused `debugConfig` declaration after proving no build type references it
- `.gitignore`, only if a current comment or duplicate ignore entry is demonstrably misleading

The worker may create:

- One concise cleanup report under `docs/governance/module-acceptance/FR-X-SOURCE-DOC-REPO-HYGIENE.md`.

## dependencies

- `FR-P-APP-IDENTITY-RENAME` completed at `b7d92f2`.
- Accepted owner decisions in `docs/governance/03_DECISION_LOG.md`.
- Accepted foundation contract in `docs/governance/FOUNDATION_RESET_DM004.md`.
- Accepted Firebase architecture in `docs/governance/FIREBASE_CLOUD_SYNC_ARCHITECTURE.md`.

## mandatory_cleanup

### 1. Final app identity and paths

- Current instructions, commands, examples, file paths, package declarations, test class names, and launch commands must use `com.harrylabs.shreeshyamstore`.
- Remove current-state claims that namespace/application ID is `com.example` or `com.aistudio.shreeshyamstore.pqwzkb`.
- Use the current launch command:

```powershell
adb shell am start -n com.harrylabs.shreeshyamstore/com.harrylabs.shreeshyamstore.MainActivity
```

- Update `PROJECT_CONTEXT.md`; it is subordinate context but must not advertise the old identity or old source paths.

### 2. Historical evidence handling

- Do not falsify old commit reports, failure evidence, or historical task context merely to make a text scan empty.
- Old M01/M02F packets or acceptance notes containing legacy package names must either:
  - be updated where the text is an instruction intended to be executed now, or
  - receive a prominent `Historical/Superseded - do not execute` note explaining the current package and replacement packet/command.
- Historical command/output examples may retain the old value only when clearly labelled as historical evidence.
- No unlabelled legacy package reference may remain in current instructions.

### 3. Room v2 and migration truth

- Replace claims that current code broadly uses `fallbackToDestructiveMigration()`.
- State the actual implementation: Room version 2 with v1-only `fallbackToDestructiveMigrationFrom(true, 1)`, approved because no real inventory existed.
- State that Room migrations from v2 onward require intentional migrations or a new explicit owner-approved reset.
- Remove already-resolved blockers that still claim Room v2 or migration strategy has not been decided.
- Keep warnings against broad production destructive migration.

### 4. Firebase and authentication truth

- State consistently that Firebase Auth, Google Sign-In, Firestore, cloud restore, rules deployment, and App Check are mandatory but not yet implemented.
- Do not claim Firebase dependencies are fully integrated merely because a Firebase BoM entry exists.
- Mark the hybrid Firestore/Room architecture as accepted, not pending approval.
- Remove obsolete wording that cloud sync is deferred or that local-only storage is the intended final architecture.
- Current local Room/DataStore authentication may be described as the present implementation, but it must be clearly distinguished from the mandatory upcoming Firebase owner-auth flow.
- Align the next implementation route to:
  1. Firebase project/config prerequisites.
  2. Firebase Auth using modern Credential Manager Sign in with Google.
  3. Owner/shop/membership profile creation and restore.
  4. Firestore rules, App Check, and cost controls.
  5. Product/category/settings sync and inventory restore gate.
- Do not add Firebase implementation or `google-services.json` in this packet.

### 5. Modern Firebase guidance in docs

- Where current implementation guidance is documented, use Firebase main modules:
  - `firebase-auth`
  - `firebase-firestore`
- Do not recommend deprecated/end-of-maintenance `firebase-auth-ktx` or `firebase-firestore-ktx` modules.
- Use Android Credential Manager for Sign in with Google planning; do not introduce legacy `GoogleSignInClient` as the new implementation direction.
- Clarify that Firestore offline persistence does not by itself solve business-record conflicts; sales, stock, and udhaar still need idempotency/append-only/conflict rules.
- Clarify that App Check debug provider is for emulator/development and Play Integrity is the production direction.
- Clarify that budget alerts monitor spending but do not hard-cap charges.
- These are documentation corrections only. Do not change dependency versions in this packet.

### 6. Foundation-reset progress and routing

- Mark `FR-A`, `FR-C`, `FR-B`, and `FR-P` as completed with their checkpoint commits where the repo already has verified evidence:
  - `FR-A`: `0bd189b`
  - `FR-C`: `f36d613`
  - `FR-B`: `4bb4927`
  - `FR-P`: `b7d92f2`
- Ensure current plans do not route workers back into completed packets.
- Preserve the approved remaining product sequence:
  - Firebase foundation and restore.
  - Product unit/rate/stock UI with inline category creation.
  - Product/category/settings sync and inventory restore QA.
  - Weighted billing, bidirectional amount/quantity calculator, and per-line rate override.
  - Billing persistence and billing restore QA.
- Make clear that the welcome sound issue is tracked but not part of this cleanup or Firebase packet.
- Do not start Firebase, Product UI, Billing UI, sound/assets, or another feature from this packet.

### 7. Product and data-model consistency

- Remove stale `TBD` items already settled by owner decisions:
  - final application ID/package/namespace
  - hybrid Firestore/Room architecture
  - Room v2 reset allowance
  - `Long` paise/base-unit strategy
  - per-line billing rate override foundation
- Keep genuinely unresolved Firebase-console decisions as owner actions:
  - project ID/environment policy
  - Firestore location
  - SHA-1/SHA-256 registration
  - Firebase config handling
  - Auth provider rollout details
  - App Check enforcement timing
  - cost/budget guardrails
- Reconcile implemented Room v2 fields versus future target fields by checking actual entities. Do not label implemented fields as merely planned.
- Ensure the real-inventory and real-billing gates remain exactly aligned with `FOUNDATION_RESET_DM004.md`.

### 8. Build, signing, SDK, and test documentation

- Reflect that FR-P removed the debug build type's explicit `debugConfig` assignment and debug builds use the Android Gradle Plugin default debug signing behavior.
- If `signingConfigs.create("debugConfig")` remains unused, it may be removed from `app/build.gradle.kts` as hygiene, but no keystore may be generated or committed.
- Do not modify release signing behavior or secret handling.
- Document that `GreetingScreenshotTest` uses Robolectric SDK 34 for Java 19 compatibility; this does not lower app `compileSdk` or `targetSdk`.
- Remove or correct commands targeting old package/test names.
- Do not change Java, Kotlin, AGP, Compose, Room, Firebase BoM, KSP, Coil, or any other dependency/plugin version.

### 9. Language, encoding, and terminology

- Remove mojibake such as `â‚¹`, `�`, `Ã`, `Â`, or broken quote characters from touched documentation.
- Use clean UTF-8 `₹` or `Rs.` consistently.
- Final UI requirements remain English default and clean Hindi second language.
- Do not use Hinglish as proposed app UI copy.
- Hinglish is acceptable only in owner quotations or conversational records when retaining them is necessary.
- Use consistent product terms: `Shree Shyam Store`, `kiryana/general store`, `cloud-backed and offline-capable`, `manual UPI record`, and `real inventory gate`.

### 10. Untracked and duplicate governance files

- Inspect every untracked document before deciding whether it belongs in the current source-of-truth set.
- Keep useful governance/source documents and align them with current decisions.
- Do not delete files merely because they are untracked or old.
- If two files duplicate the same active instruction:
  - choose one current canonical file based on `AGENTS.md`,
  - mark the other as historical/superseded or consolidate it,
  - update inbound references.
- Do not include `.idea/**` or generated build output.
- Do not edit or commit Gradle wrapper files as part of this packet.

## constraints

- This is one combined repository/documentation hygiene pass, not a feature task.
- Do not split this cleanup into more planning packets.
- Do not implement Firebase, Auth, Firestore, App Check, sync, Product UI, Billing UI, welcome sound, assets, printer, PDF, WhatsApp, barcode, GST, staff roles, returns, or backup.
- Do not change Room entities, DAOs, repositories, ViewModels, Compose screens, resources, manifests, or tests except no files in those areas are allowed by `scope_paths`.
- Do not upgrade, downgrade, add, or remove dependency/plugin versions.
- Do not edit `gradle/libs.versions.toml`.
- Do not add `google-services.json`, service-account files, signing keys, credentials, real customer data, or Firebase secrets.
- Do not rewrite append-only decision history as though old decisions never existed. Add superseded/current status clearly.
- Do not run repeated emulator testing for docs-only changes.
- Do not commit or push unless the owner separately authorizes the checkpoint.
- Preserve unrelated dirty changes and use hunk-aware staging if later authorized.

## acceptance_criteria

- Current source-of-truth documents agree on app identity, completed packets, Room v2 state, Firebase-not-yet-implemented state, accepted hybrid architecture, inventory gate, and remaining implementation order.
- `PROJECT_CONTEXT.md` no longer presents old package/source paths or broad destructive migration as current.
- Current executable commands contain no old application ID, namespace, package, activity, or test class.
- Historical legacy references are retained only when explicitly labelled historical/superseded and non-executable.
- No active document says cloud sync is deferred or local-only is the intended final product.
- No active document says the hybrid Firebase architecture still needs owner approval.
- No active document says broad `fallbackToDestructiveMigration()` is the current Room implementation.
- No settled owner decision remains incorrectly listed as `TBD`.
- FR-A, FR-C, FR-B, and FR-P completion status and commit evidence are consistent.
- Remaining Firebase prerequisites and genuinely unresolved owner decisions are explicit.
- No dependency version changed.
- No application behavior, UI, Room schema, Firebase implementation, or secret handling changed.
- No mojibake remains in touched documentation.
- The cleanup acceptance report lists every modified, added, superseded, or deliberately preserved historical file.

## verification_commands

Capture the baseline first:

```powershell
git branch --show-current
git rev-parse HEAD
git status --short --untracked-files=all
```

Run repository scans after cleanup:

```powershell
rg -n --hidden -g '!/.git/**' -g '!app/build/**' -g '!build/**' "com\.aistudio|pqwzkb|package com\.example|import com\.example|app/src/.*/com/example"
rg -n --hidden -g '!/.git/**' -g '!app/build/**' -g '!build/**' "fallbackToDestructiveMigration\(\)"
rg -n --hidden -g '!/.git/**' -g '!app/build/**' -g '!build/**' "cloud sync.*defer|Firebase.*defer|local-only|local only"
rg -n --hidden -g '!/.git/**' -g '!app/build/**' -g '!build/**' "firebase-auth-ktx|firebase-firestore-ktx|GoogleSignInClient"
rg -n --hidden -g '!/.git/**' -g '!app/build/**' -g '!build/**' "â‚¹|�|Ã|Â|â€"
```

For every remaining match, classify it in the acceptance report as:

- valid current instruction,
- explicitly labelled historical evidence,
- or unresolved defect.

Verify app identity and dependency non-change:

```powershell
rg -n 'namespace = "com\.harrylabs\.shreeshyamstore"|applicationId = "com\.harrylabs\.shreeshyamstore"' app/build.gradle.kts
git diff -- gradle/libs.versions.toml
git diff -- app/src
```

Expected:

- `gradle/libs.versions.toml` has no worker-created diff.
- `app/src` has no worker-created diff.
- No emulator or connected Android test is required because this packet is behavior-neutral.
- Run `.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon` only if `app/build.gradle.kts` was changed.
- If only Markdown files changed, use scans and diff review; do not run Gradle merely for ceremony.

Final diff review:

```powershell
git diff --check
git diff --stat
git status --short --untracked-files=all
```

## required_evidence

- Branch and HEAD captured before edits.
- Pre-existing dirty/untracked files listed separately from worker changes.
- Exact files changed and purpose of each.
- Results of every required `rg` scan, including classification of intentional historical matches.
- Evidence that `gradle/libs.versions.toml` and `app/src/**` were not changed by the worker.
- Build result only if `app/build.gradle.kts` changed.
- Explicit confirmation that Firebase, dependencies, Room schema, UI, and business logic were not implemented or changed.
- Explicit list of files marked historical/superseded versus current.
- Acceptance report at `docs/governance/module-acceptance/FR-X-SOURCE-DOC-REPO-HYGIENE.md`.

## review_owner

Delivery manager or independent documentation/governance auditor.

## completion_response

Return:

```text
## Summary
- What was normalized and why.

## Files changed
- Exact path and purpose.

## Current source-of-truth
- Final app identity.
- Completed FR checkpoints.
- Current Room state.
- Current Firebase implementation state.
- Remaining implementation route.

## Historical files
- Files retained as historical/superseded and why.

## Repo findings
- Remaining contradictions, duplicates, or intentionally deferred cleanup.

## Security/privacy impact
- Confirmation that no secrets, Firebase config, customer data, signing keys, or app behavior changed.

## Verification
- Commands and results.
- Classification of remaining legacy/stale-wording scan hits.

## Dependency impact
- Confirm no dependency/plugin versions changed.

## Remaining owner decisions
- Only genuinely unresolved decisions.

## Acceptance decision
- PASS or BLOCK.

## Next recommended task
- Firebase project/config and Auth implementation route; do not create another docs-cleanup packet.
```
