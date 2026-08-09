# FR-E-BILLING-WEIGHTED-ENTRY-RATE-OVERRIDE

## task_id

FR-E-BILLING-WEIGHTED-ENTRY-RATE-OVERRIDE

## goal

Add weighted/volume billing entry and per-line rate override to cart behavior.

## scope_paths

- `app/src/main/java/**/BillingAndPaymentScreen.kt`
- `app/src/main/java/**/ShopViewModel.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-hi/strings.xml`
- `app/src/test/**`
- `app/src/androidTest/**` if UI automation is added.

## dependencies

- `FR-C-QUANTITY-PRICE-CALCULATOR`
- `FR-D-PRODUCT-UNIT-STOCK-UI`
- `FR-I1-INVENTORY-RESTORE-QA`

## constraints

- Override must not mutate product master price.
- Piece plus/minus flow must still work.
- No sale persistence/cloud sync changes except what is needed for cart state interfaces.

## acceptance_criteria

- Weight/volume products open a quantity/amount dialog or bottom sheet.
- Dialog supports quantity-to-amount and amount-to-quantity.
- Dialog supports effective rate override.
- Cart line stores original rate, effective rate, override flag, quantity/base unit, and line total.
- Cart clearly displays overrides, for example `Rs.22/kg, discounted from Rs.25/kg`.

## required_evidence

- Tests/manual proof for weighted entry and per-line override.
- English/Hindi string evidence.

## review_owner

QA plus delivery manager.

## do_not_touch

- Firestore sales sync.
- Room schema implementation.
- Product master price mutation.
- PDF, WhatsApp, printer, GST, barcode, welcome sound/assets.

