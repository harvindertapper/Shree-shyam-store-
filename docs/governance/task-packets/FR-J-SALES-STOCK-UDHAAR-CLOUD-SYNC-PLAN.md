# FR-J-SALES-STOCK-UDHAAR-CLOUD-SYNC-PLAN

## task_id

FR-J-SALES-STOCK-UDHAAR-CLOUD-SYNC-PLAN

## goal

Plan append-only cloud sync for sales, stock adjustments, customers, and udhaar after local billing is stable.

## scope_paths

- `docs/DATA_MODEL.md`
- `docs/governance/FIREBASE_CLOUD_SYNC_ARCHITECTURE.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/governance/task-packets/**`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`

## dependencies

- `FR-F-SALE-STOCK-INVOICE-UDHAAR-PERSISTENCE`
- `FR-I2-BILLING-RESTORE-QA`

## constraints

- Planning only.
- No live Firestore sync implementation.
- Sensitive business data needs security review before sync.

## acceptance_criteria

- Packets exist for sales, sale items, stock adjustments, customers, and udhaar sync.
- Conflict/idempotency/rules-test strategy is documented.
- Append-only behavior is preserved for sales, stock adjustments, and udhaar.

## required_evidence

- Docs changed.
- Security/privacy review notes.
- Open owner decisions.

## review_owner

Security/governance plus delivery manager.

## do_not_touch

- Live Firestore sync code.
- Billing UI/persistence implementation.
- Returns/refunds UI, staff UI, barcode, GST, printer, PDF, WhatsApp, bulk import, welcome sound/assets.

