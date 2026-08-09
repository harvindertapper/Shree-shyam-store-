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
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/WelcomeScreen.kt` looks up a raw resource named `jai_shree_shyam_chant`.
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

- `AGENTS.md` states Firebase Auth is now the runtime owner identity direction and Room is the local cache/offline working store.
- `docs/DATA_MODEL.md` states Room remains the local database/cache and DataStore stores non-secret preferences/session markers.
- Android Clear storage wipes app-private local storage, including Room database files and DataStore preferences.
- FR-G local implementation exists, but final live owner restore and broader product/customer/sale/udhaar sync acceptance are still pending.

Classification:

- Expected Android behavior for local storage, but unacceptable for professional shop trust unless cloud restore/sync gates pass.
- Data safety, backup/restore, sync, and auth hardening concern.
- Do not work around this by hiding business data in unsafe external files or hardcoded defaults.
- Owner decisions on 2026-06-17 and 2026-07-02 make cloud auth/sync mandatory professional foundation before trusted real inventory and billing use.

Assigned phase/module:

- Primary: `PD-02` Firebase owner trust gate and `PD-03` professional sync/restore gate.
- Related: `PD-10` release hardening / Android backup policy.
- Related: Manual backup/export/import only if still needed after cloud recovery decisions.

Recommended product solution when scheduled:

- On first launch after fresh install/cleared storage, show Firebase sign-in and restore gate before Home.
- Restore settings, categories, products, customers, udhaar, sales, sale items, stock adjustments, and invoice metadata according to the accepted sync phase.
- Show visible pending/error sync state; do not silently open an empty shop as success.
- Decide whether Android Auto Backup should be disabled, configured, or excluded for sensitive shop data.
- Use the approved Firebase account/sync architecture, not an accidental local workaround.

Acceptance criteria when scheduled:

- Same Google account after clear storage restores the same owner/shop profile and accepted synced business domains.
- Products, customers, udhaar balances, sales, sale items, stock adjustments, and settings restore without duplicates after retry.
- Backup file format, encryption/password policy, and restore conflict behavior are owner-approved if manual backup remains.
- No secrets, signing data, or unsafe tokens are stored in Room, DataStore, strings, or source code.

Owner decisions needed:

- Confirm Credential Manager Sign in with Google as the first provider and decide any later provider rollout.
- Confirm live Google provider/SHA setup and final manual owner sign-in/shop-profile QA.
- Should manual encrypted backup/restore still exist after Firebase restore?
- Should Android Auto Backup be disabled or configured before release?
- What clear-storage/reinstall/second-device restore evidence is required for professional acceptance?

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

Accepted foundation and remaining decisions:

- Accepted: piece products use count, weight uses grams, volume uses ml, and permanent money uses `Long` paise.
- Accepted: weighted/volume amount-to-quantity rounds to the nearest gram/ml and fractional pieces are invalid.
- Remaining Product UI decision: whether packet, box, or custom units enter the first Product UI release.
- Remaining future decision: whether barcode/product-code later supports different units for the same product.
