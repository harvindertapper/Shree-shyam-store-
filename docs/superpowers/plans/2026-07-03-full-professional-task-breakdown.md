# Full Professional Delivery Task Breakdown - Shree Shyam Store

Date: 2026-07-03

This file converts `docs/superpowers/plans/2026-07-02-professional-delivery-plan.md` into executable task packets. Physical/manual QA is owner-led; code/build/unit verification remains agent-led.

## Implementation sequence

1. `PD-04A` Product entry stock-by-exception and category cleanup.
2. `PD-04B` Product unit/rate conversion hardening for piece, weight, and volume.
3. `PD-04C` Billing-created product cleanup flags and cleanup filter.
4. `PD-05A` One-tap cash/UPI save without forced customer or invoice.
5. `PD-05B` Loose item quantity-to-amount and amount-to-quantity billing UX.
6. `PD-05C` Per-line rate override and saved sale-item snapshots.
7. `PD-05D` Smart product quick-add during billing with later cleanup.
8. `PD-05E` Tracked stock validation and untracked stock bypass.
9. `PD-03C` Sales, sale items, stock adjustments, and invoice metadata sync verification.
10. `PD-06A` Quick udhaar credit/payment entry.
11. `PD-06B` WhatsApp/share reminder text workflow without Contacts permission.
12. `PD-07A` Owner Private Desk shell with private-only navigation.
13. `PD-07B` Stock value and category-wise stock value.
14. `PD-07C` Profit reports from sale-item cost snapshots.
15. `PD-08A` End-of-day Aaj ka hisaab summary.
16. `PD-08B` Shop-close summary snapshot and subtle success feedback.
17. `PD-09A` Optional invoice formatter from saved sale snapshots.
18. `PD-09B` PDF/share workflow through Android share sheet.
19. `PD-10A` App Check/cost/backup/privacy release checklist.
20. `PD-10B` Localization polish, release build posture, and final verification.

## Task packets

### PD-04A - Product stock-by-exception and category cleanup

- `goal`: Make product entry forgiving: inline category creation, duplicate prevention, and optional stock tracking by default.
- `scope_paths`: `ProductsAndStockScreen.kt`, `ShopViewModel.kt`, `Daos.kt`, `strings.xml`, `strings.xml` Hindi, `ShopViewModelTest.kt`.
- `dependencies`: PD-03 local sync foundation committed.
- `constraints`: No Room migration unless strictly required; all UI text in English and Hindi resources.
- `acceptance_criteria`: Case-insensitive category duplicates blocked; new product can be saved with stock tracking off; tracked products still validate stock fields.
- `required_evidence`: TDD ViewModel/unit tests, debug build, unit test suite.
- `review_owner`: delivery manager.

### PD-04B - Unit/rate conversion hardening

- `goal`: Ensure piece, kg/g, and litre/ml products store paise/base-unit fields correctly.
- `scope_paths`: `ProductsAndStockScreen.kt`, `ShopViewModel.kt`, calculator/utils tests.
- `dependencies`: PD-04A.
- `constraints`: Use integer paise/base units; no floating-point stored business totals for new canonical fields.
- `acceptance_criteria`: Sugar per kg and milk per litre create correct `pricePerUnitPaise`, `priceUnitBaseQty`, `stockQuantityBase`, and low-stock base values.
- `required_evidence`: Unit tests for weight and volume product save.
- `review_owner`: delivery manager.

### PD-04C - Quick-added product cleanup

- `goal`: Products created quickly during billing are marked for later cleanup and visible in Products cleanup filter.
- `scope_paths`: Product entity/Room migration only if existing fields are insufficient, product screen, billing quick-add, tests.
- `dependencies`: PD-04B.
- `constraints`: Intentional migration if schema changes; no destructive migration.
- `acceptance_criteria`: Quick-added products are findable for missing purchase price/category/stock-tracking review.
- `required_evidence`: Migration/repository tests if schema changes; UI compile.
- `review_owner`: delivery manager plus QA.

### PD-05A - One-tap sale save

- `goal`: Cash/UPI sale saves without customer and without invoice.
- `scope_paths`: Billing screen, ViewModel, repository tests.
- `dependencies`: PD-04.
- `constraints`: Udhaar still requires/selects customer; UPI remains manual record.
- `acceptance_criteria`: Cash sale can save directly; invoice is optional after sale.
- `required_evidence`: Unit tests for cash/UPI/udhaar flow.
- `review_owner`: delivery manager.

### PD-05B - Loose item calculator in billing

- `goal`: Amount-to-quantity and quantity-to-amount work at the counter for weight/volume.
- `scope_paths`: Billing screen, cart line model, calculator tests.
- `dependencies`: PD-05A.
- `constraints`: Piece items reject fractional quantity unless explicitly modeled.
- `acceptance_criteria`: `Rs. 50 sugar @ Rs. 47/kg` calculates quantity and line total exactly; quantity entry calculates amount.
- `required_evidence`: Unit tests for paise/base-unit math.
- `review_owner`: delivery manager.

### PD-05C - Per-line rate override

- `goal`: Owner can override rate for a cart line without changing product master.
- `scope_paths`: Billing screen, CartLine, SaleItem snapshots, tests.
- `dependencies`: PD-05B.
- `constraints`: Store original and effective rate snapshots.
- `acceptance_criteria`: Saved sale item shows rate override flag and old product price remains unchanged.
- `required_evidence`: Unit/repository tests.
- `review_owner`: delivery manager.

### PD-05D - Smart product quick-add during billing

- `goal`: Missing item does not stop billing; quick add requires only name and selling price.
- `scope_paths`: Billing screen, ViewModel quickAddProduct, product cleanup marker.
- `dependencies`: PD-04C, PD-05A.
- `constraints`: Quick-added products sync through normal product path.
- `acceptance_criteria`: Quick-add product appears in cart and later cleanup list.
- `required_evidence`: ViewModel tests and debug build.
- `review_owner`: delivery manager.

### PD-05E - Stock validation

- `goal`: Tracked products cannot accidentally oversell; untracked products do not block sale.
- `scope_paths`: ShopRepository, ShopViewModel, billing tests.
- `dependencies`: PD-05A.
- `constraints`: Oversell setting is deferred unless owner approves.
- `acceptance_criteria`: Tracked insufficient stock fails visibly; untracked product saves sale.
- `required_evidence`: Repository tests.
- `review_owner`: delivery manager.

### PD-06A - Quick udhaar

- `goal`: Add credit/payment quickly from customer row/detail.
- `scope_paths`: Udhaar screen, ViewModel, repository tests, strings.
- `dependencies`: PD-05.
- `constraints`: Ledger remains append-only.
- `acceptance_criteria`: Amount can be added quickly and balance updates from transactions.
- `required_evidence`: Balance tests.
- `review_owner`: delivery manager plus QA.

### PD-06B - WhatsApp/share reminder

- `goal`: Generate polite reminder message and share without Contacts permission.
- `scope_paths`: Udhaar screen/share helper, strings.
- `dependencies`: PD-06A.
- `constraints`: WhatsApp direct target optional; share sheet fallback required.
- `acceptance_criteria`: Reminder contains shop name, customer name, balance, and note.
- `required_evidence`: Formatting tests and manual share proof by owner.
- `review_owner`: delivery manager.

### PD-07A/B/C - Owner Private Desk

- `goal`: Keep stock value/profit private and useful.
- `scope_paths`: New/private screen or Reports tab, calculators, repository queries, strings.
- `dependencies`: PD-05 sale snapshots.
- `constraints`: Profit uses sale-item purchase-cost snapshots, not current product cost.
- `acceptance_criteria`: Stock value total/category and today/month profit visible only in private owner section.
- `required_evidence`: Calculator tests, privacy review.
- `review_owner`: delivery manager plus security/privacy.

### PD-08A/B - Aaj ka hisaab

- `goal`: End-of-day summary and shop-close snapshot.
- `scope_paths`: Home/day-close screen, summary utilities, DataStore/Room if needed.
- `dependencies`: PD-05, PD-06, PD-07.
- `constraints`: Subtle routine feedback only; no childish gamification.
- `acceptance_criteria`: Owner can see sales/payment/udhaar/low-stock/top-item/pending-sync summary.
- `required_evidence`: Summary tests.
- `review_owner`: delivery manager.

### PD-09A/B - Optional invoice PDF/share

- `goal`: Generate professional invoice only when owner asks.
- `scope_paths`: Invoice formatter, PDF/share helper, FileProvider if needed, strings.
- `dependencies`: PD-05 saved sale snapshots.
- `constraints`: No broad storage permission; no Bluetooth printer.
- `acceptance_criteria`: PDF/share uses saved sale data and falls back gracefully.
- `required_evidence`: Formatter tests and owner manual share proof.
- `review_owner`: delivery manager plus QA.

### PD-10A/B - Release hardening

- `goal`: Prepare release posture without secrets.
- `scope_paths`: Manifest backup policy, security checklist, release docs, localization.
- `dependencies`: PD-04 through PD-09.
- `constraints`: Do not commit signing secrets or service-account keys.
- `acceptance_criteria`: App Check/cost/backup/privacy/signing posture documented; build/test pass.
- `required_evidence`: Build/test output and security/privacy checklist.
- `review_owner`: delivery manager plus security/privacy.

## Known blockers and owner-led evidence

- Physical phone QA is owner-led.
- Live Firebase clear-storage restore acceptance is owner-led.
- Firestore emulator tests require compatible local Java/Firebase CLI environment.
- App Check enforcement, cost budget, Android backup policy, invoice legal/tax fields, and release signing secrets remain owner decisions.
