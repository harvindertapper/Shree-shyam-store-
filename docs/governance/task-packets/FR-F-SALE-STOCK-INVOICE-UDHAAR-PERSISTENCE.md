# FR-F-SALE-STOCK-INVOICE-UDHAAR-PERSISTENCE

## task_id

FR-F-SALE-STOCK-INVOICE-UDHAAR-PERSISTENCE

## goal

Persist weighted/override sale lines, stock reductions, invoices, reports, and udhaar records in the v2 local model.

## scope_paths

- `app/src/main/java/**/ShopRepository.kt`
- `app/src/main/java/**/ShopViewModel.kt`
- `app/src/main/java/**/BillingAndPaymentScreen.kt`
- `app/src/main/java/**/ReportsScreen.kt`
- `app/src/main/java/**/UdhaarScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-hi/strings.xml`
- `app/src/test/**`
- `app/src/androidTest/**` if needed.

## dependencies

- `FR-E-BILLING-WEIGHTED-ENTRY-RATE-OVERRIDE`

## constraints

- UPI remains a manual payment record.
- Do not add real payment verification.
- Prevent negative tracked stock unless owner explicitly approves overselling.
- No sales/stock/udhaar cloud sync.

## acceptance_criteria

- Cash, UPI, and udhaar sales persist weighted/volume/piece line items.
- SaleItem snapshots original rate, effective rate, override flag, unit, quantity, base quantity, line total, and purchase cost snapshot.
- Stock reduces in base units.
- Invoice shows readable unit/quantity and effective billed rate.
- Reports and stock history show readable units and paise-based totals.
- Udhaar uses paise totals.

## required_evidence

- Unit tests for sale persistence, stock reduction, invoice text, reports, and udhaar.
- Manual proof for cash, UPI, and udhaar weighted bills.

## review_owner

Delivery manager plus QA.

## do_not_touch

- Cloud sync of sales, stock, customers, udhaar, invoices.
- Returns/refunds UI.
- Staff UI, barcode, GST, printer, PDF, WhatsApp share, bulk import, welcome sound/assets.

