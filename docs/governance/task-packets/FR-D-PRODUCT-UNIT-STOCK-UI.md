# FR-D-PRODUCT-UNIT-STOCK-UI

## task_id

FR-D-PRODUCT-UNIT-STOCK-UI

## goal

Add Product Add/Edit unit/rate/stock setup, preview calculator, and inline category creation.

## scope_paths

- `app/src/main/java/**/ProductsAndStockScreen.kt`
- `app/src/main/java/**/ShopViewModel.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-hi/strings.xml`
- `app/src/test/**`
- `app/src/androidTest/**` if UI automation is added.

## dependencies

- `FR-B-ROOM-V2-RESET`
- `FR-C-QUANTITY-PRICE-CALCULATOR`

## constraints

- English and Hindi strings required.
- Firebase sync for categories/products happens in FR-H, not here.
- No billing UI or sale persistence changes.

## acceptance_criteria

- Owner can choose piece, weight, or volume product type.
- Owner can configure selling unit, stock unit, selling price per selected unit, purchase price per selected unit, opening stock, and low stock alert.
- Preview calculator supports quantity-to-amount and amount-to-quantity.
- Inline Add New Category is available from category selector.
- New category is validated, saved, and selected for the product.
- Duplicate category names are prevented case-insensitively where possible.

## required_evidence

- Unit/UI/manual proof for product setup and preview calculator.
- Category duplicate prevention evidence.
- English/Hindi string key parity evidence for new strings.

## review_owner

QA plus delivery manager.

## do_not_touch

- Firestore sync.
- Billing weighted entry.
- Sale/stock/udhaar persistence.
- Package rename.
- Welcome sound/assets.

