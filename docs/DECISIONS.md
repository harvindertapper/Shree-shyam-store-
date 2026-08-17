# Engineering Decisions

## ADR-001: Keep GitHub as source of truth and use reviewable branches

**Status:** Accepted  
**Decision:** Maintain a normal Desktop checkout for the user and an agent-accessible checkout synchronized through GitHub. Develop on focused branches and merge through pull requests; do not edit `main` directly.

**Reason:** This provides attribution, review, rollback, and a safe boundary between local unpushed edits and agent-created changes. The current repository has no protected branch, so protection should be enabled after the stable check name is confirmed.

## ADR-002: Modernize incrementally rather than rewrite immediately

**Status:** Accepted  
**Decision:** Preserve the Kotlin/Compose/Room foundation and extract domain boundaries gradually. Propose a rewrite only if measured security, testability, deployment, or data constraints cannot be addressed through staged changes.

**Reason:** The prototype already implements meaningful offline store workflows. A broad rewrite would increase regression and data-migration risk before the current behavior and data contract are fully documented.

## ADR-003: Treat user identity as device-local until one identity model is chosen

**Status:** Accepted for the stabilization branch  
**Decision:** Do not upload or restore local users, password hashes, PIN verifiers, or session identity through business-data sync or manual REST backup. Preserve device-local users and the shop profile during cloud business-data restore.

**Reason:** The former sync path serialized `passwordHash`, and the app mixes Firebase sign-in with local password auth. Until account ownership and linking are deliberately designed, local credential material must not cross the cloud business-data boundary.

## ADR-004: Make cloud restore a single transaction over cloud-owned tables

**Status:** Accepted for the stabilization branch  
**Decision:** Download and check for a non-empty business snapshot, then replace only cloud-owned tables through `ShopRepository.replaceCloudRestorableTables()` inside one Room transaction.

**Reason:** Separate clear and insert transactions could leave the device empty after a later insert failure. Users and shop profile are device-owned and are preserved. Future work must add snapshot schema/version, integrity metadata, completeness validation, and a local recovery point.

## ADR-005: Use the committed Gradle wrapper for reproducibility

**Status:** Accepted for the stabilization branch  
**Decision:** Commit the Gradle 9.7.0 wrapper and use `./gradlew` in local documentation and CI.

**Reason:** The repository previously depended on an externally installed Gradle version, which made local and CI behavior less reproducible. The wrapper does not contain secrets and downloads the pinned distribution when needed.

## ADR-006: Apply explicit P0 checkout and udhaar policies

**Status:** Accepted for the P0 financial-safety branch
**Decision:** Tracked inventory uses `REJECT_UNDERFLOW`; udhaar uses a hard projected-balance credit limit; received payments are validated at the repository boundary and may create a customer credit balance when they exceed the outstanding amount; UPI remains a manual settlement flow until a verified gateway/reference workflow is introduced.

**Reason:** These defaults make the current offline-first workflow deterministic without silently creating negative stock or bypassing the owner’s credit policy. Payment integration, credit-balance presentation, immutable reversals, actor metadata, and minor-unit money migration remain follow-up work rather than being implied by the prototype UI.

## Open decisions

The owner still needs to decide the production application ID/package identity, whether local password auth will be removed in favor of Firebase identity, the payment/UPI provider boundary, stable global ID and conflict strategy, backup provider authorization, release distribution channel, data-retention expectations, and production rollout date. The P0 branch accepts the inventory and UPI defaults above; remaining production blockers include minor-unit money migration, immutable ledger reversals, actor/audit metadata, multi-device identity/conflict semantics, and non-destructive Room migrations. These should be recorded here before implementing the corresponding production migrations.
