# FR-I1-INVENTORY-RESTORE-QA

## task_id

FR-I1-INVENTORY-RESTORE-QA

## goal

Verify inventory restore readiness before the owner starts entering real shop inventory.

## scope_paths

- `docs/governance/MANUAL_QA_BACKLOG.md`
- `APP_BUILD_CHECKLIST.md`
- `app/src/androidTest/**` if automation is added.
- QA evidence artifacts if approved.

## dependencies

- `FR-H-PRODUCT-CATEGORY-SETTINGS-SYNC`

## constraints

- QA only unless a narrow test automation update is needed.
- No new features.
- No billing implementation.

## acceptance_criteria

- Clear storage restore recovers owner, shop, categories, products, settings, unit fields, and rates.
- Reinstall restore recovers the same data.
- Second-device restore recovers the same data under the same owner account.
- Cross-account isolation is verified.

## required_evidence

- Manual or connected test evidence.
- Device/emulator details.
- Pass/fail summary.
- Git status.

## review_owner

QA plus owner.

## do_not_touch

- Billing implementation.
- Sales/stock/udhaar cloud sync.
- Product schema or Firebase rules except if reporting a blocker.
- Welcome sound/assets.

