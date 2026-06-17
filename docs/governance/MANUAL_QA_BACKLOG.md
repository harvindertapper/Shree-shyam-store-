# Manual QA Backlog

Purpose: Track issues found during owner/manual testing without pulling them into the wrong active phase.

Rules:

- Do not fix these items immediately unless the active task packet explicitly includes them.
- Keep each item assigned to the right phase/module so it is handled at the right time.
- If an item affects data integrity, payment trust, auth, backup, or release safety, escalate it to the delivery manager before implementation.

## MQA-001 - Welcome chant does not play on physical phone

Status: Deferred.

Observed by: Owner manual test on physical phone.

Observation:

- The welcome screen did not play the expected "Jai Shree Shyam" welcome sound.

Current evidence:

- `docs/SCREEN_FLOW.md` and `docs/PRODUCT_SPEC.md` describe a welcome chant setting.
- `app/src/main/java/com/example/ui/screens/WelcomeScreen.kt` looks up a raw resource named `jai_shree_shyam_chant`.
- `app/src/main/res/raw/` is currently missing, so no chant audio asset is packaged in the app.

Classification:

- Asset/content gap, not a Phase 2 billing feature.
- Treat as branding/polish unless the owner marks welcome sound as mandatory for demo or release.

Assigned phase/module:

- Primary: M12 Release Branding / Asset Pack.
- Optional earlier slot: post-M01 polish pass before a physical-phone demo build, if owner wants the sound in the next demo APK.

Acceptance criteria when scheduled:

- Add an owner-approved, copyright-safe audio asset at `app/src/main/res/raw/jai_shree_shyam_chant.*`.
- Verify the welcome chant toggle works on and off.
- Verify playback on a physical Android phone.
- Keep volume, duration, and religious/devotional tone respectful and non-intrusive.
- Do not add streaming/network audio.

Owner decisions needed:

- Approve whether welcome sound is mandatory for demo, public release, or optional.
- Approve the final chant/audio style and duration.
- Confirm whether a Khatu Shyam visual asset should also be added for the welcome screen.

## MQA-002 - Owner account disappears after Android Clear storage

Status: Routed to DM-004 Firebase foundation and FR-I1 inventory restore QA.

Observed by: Owner manual test on physical phone.

Observation:

- After using Android app info > Clear storage, the owner account/user details are removed and the app behaves like a fresh install.

Current evidence:

- `AGENTS.md` states auth/session is local only today: owner account is in Room and session flags are in DataStore.
- `docs/DATA_MODEL.md` states Room is the local database and DataStore stores settings/session.
- Android Clear storage wipes app-private local storage, including Room database files and DataStore preferences.

Classification:

- Expected Android behavior for the current local-only build, not a simple UI bug.
- Data safety, backup/restore, and auth hardening concern.
- Do not work around this by hiding business data in unsafe external files or hardcoded defaults.
- Owner decision on 2026-06-17 makes cloud auth/sync mandatory MVP foundation before Billing Phase 2.

Assigned phase/module:

- Primary: M02F Firebase Cloud Sync Foundation for owner account, shop ownership, clear-storage restore, reinstall restore, and future second-device recovery.
- Related: M10 Backup / Export / Import only if manual backup remains needed after cloud recovery decisions.
- Related: M03 Local Auth and Session Hardening.
- Related release gate: M12 Release Hardening / Android backup policy.

Recommended product solution when scheduled:

- On first launch after fresh install/cleared storage, show a Firebase sign-in/restore path before new setup once cloud foundation exists.
- Add owner-approved cloud-backed restore workflow for shop data and owner account recovery.
- Decide whether Android Auto Backup should be disabled, configured, or excluded for sensitive shop data.
- Use the approved Firebase account/sync architecture, not an accidental local workaround.

Acceptance criteria when scheduled:

- Clear storage behavior is documented as fresh install until Firebase restore is implemented.
- Backup file format, encryption/password policy, and restore conflict behavior are owner-approved.
- Restore flow is tested with products, sales, customers, udhaar, shop settings, and owner account/session behavior.
- No secrets, signing data, or unsafe tokens are stored in Room, DataStore, strings, or source code.

Owner decisions needed:

- Which Firebase Auth provider should be MVP default?
- What is the owner UID to shop membership model?
- Should manual encrypted backup/restore still exist after Firebase restore?
- Should Android Auto Backup be disabled or configured before release?
- What clear-storage/reinstall/second-device restore behavior is required for MVP acceptance?

## MQA-003 - Loose/weight-based products are not supported

Status: Routed to DM-004 foundation reset before real inventory entry and billing hardening.

Observed by: Owner manual review of current app behavior.

Observation:

- Product add/edit currently appears oriented around piece-count quantity.
- There is no clear option for loose products sold by weight or measure, such as items sold by kg, gram, litre, ml, or custom local units.
- Billing does not yet expose a clear flow for entering decimal/weighted quantities and calculating totals from weight-based units.

Classification:

- Core kiryana product and billing requirement, not a cosmetic issue.
- Affects product data model, stock tracking, cart quantity controls, invoice item snapshots, reports, and future Firestore sync schema.
- Should be designed before Billing Production Hardening so billing is not locked into piece-only assumptions.

Assigned phase/module:

- Primary: FR-D Product Add/Edit Unit/Stock UI for product unit/measurement setup and preview calculator.
- Primary: FR-E Billing Weighted Entry and Rate Override for decimal/weighted quantity entry.
- Primary: FR-F Sale/Stock/Invoice/Udhaar Persistence for base-unit stock reduction and sale item snapshots.
- Related: FR-H Product/Category/Settings Sync because cloud product documents must support unit-of-measure and decimal quantities before real inventory entry.

Recommended product solution when scheduled:

- Add product unit type, for example piece, kg, gram, litre, ml, packet, box, or custom unit.
- Allow decimal quantities for measured goods where appropriate.
- Keep piece products integer-only unless owner approves fractional pieces.
- Store sale item unit snapshot and quantity snapshot so old invoices remain readable after product unit edits.
- Decide whether stock is stored in base units, for example grams/ml, or display units, for example kg/litre.

Acceptance criteria when scheduled:

- Product add/edit lets the owner choose a selling/stock unit.
- Billing accepts valid decimal quantities for weight/measure products.
- Billing totals, stock reduction, invoices, reports, and udhaar sales handle measured quantities correctly.
- Validation prevents invalid quantities such as zero, negative, or unsupported decimal precision.
- English and Hindi UI strings are added for all new unit/quantity labels.

Owner decisions needed:

- Which units are required for MVP: piece, kg, gram, litre, ml, packet, box, custom?
- Should kg/litre products store stock internally as grams/ml for precision?
- How many decimal places should billing allow for weight/measure items?
- Should barcode/product-code later support different units for the same product?
