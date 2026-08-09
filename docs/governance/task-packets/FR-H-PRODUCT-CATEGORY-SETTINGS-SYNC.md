# FR-H-PRODUCT-CATEGORY-SETTINGS-SYNC

## task_id

FR-H-PRODUCT-CATEGORY-SETTINGS-SYNC

## goal

Sync product, category, and settings data to Firestore before real inventory entry.

## scope_paths

- `app/src/main/java/**`
- `app/src/test/**`
- `app/src/androidTest/**`
- Firestore rules/tests if updates are needed.
- `docs/DATA_MODEL.md`
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`

## dependencies

- `FR-G2-FIRESTORE-RULES-APP-CHECK-COST-GUARDRAILS`
- `FR-D-PRODUCT-UNIT-STOCK-UI`

## constraints

- Product/category/settings sync only.
- No sales, stock adjustment, customer, udhaar, or invoice sync.
- Unit/rate/sync-safe fields must round-trip.
- New categories created locally in FR-D sync here.

## acceptance_criteria

- Owner/shop settings restore after reinstall.
- Categories restore after reinstall.
- Products restore after reinstall with unit type, base unit, rates, stock quantity, low stock, sync fields, and active/deleted state.
- Cross-shop access remains blocked by rules.

## required_evidence

- Clear-storage/reinstall restore proof for products/categories/settings.
- Rules/security proof.
- Build/test evidence.

## review_owner

Security/governance plus QA.

## do_not_touch

- Billing weighted UI.
- Sale/stock/customer/udhaar cloud sync.
- PDF, WhatsApp, printer, GST, barcode, bulk import.
- Welcome sound/assets.

