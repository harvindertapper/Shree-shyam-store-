# FR-I2-BILLING-RESTORE-QA

## task_id

FR-I2-BILLING-RESTORE-QA

## goal

Verify weighted billing, local persistence, and post-restart behavior after billing foundation is complete.

## scope_paths

- `docs/governance/MANUAL_QA_BACKLOG.md`
- `APP_BUILD_CHECKLIST.md`
- `app/src/androidTest/**` if automation is added.
- QA evidence artifacts if approved.

## dependencies

- `FR-F-SALE-STOCK-INVOICE-UDHAAR-PERSISTENCE`

## constraints

- QA only unless a narrow test automation update is needed.
- Do not implement sales/stock/udhaar cloud sync.

## acceptance_criteria

- Weighted billing remains correct after app restart.
- Sale history, stock adjustments, udhaar, invoices, and reports show correct values and readable units.
- Rate override snapshots remain visible and do not alter product master price.

## required_evidence

- Manual or connected test evidence.
- Cash, UPI, and udhaar scenarios.
- Pass/fail summary.
- Git status.

## review_owner

QA plus owner.

## do_not_touch

- Cloud sync implementation.
- New features outside QA.
- Welcome sound/assets.

