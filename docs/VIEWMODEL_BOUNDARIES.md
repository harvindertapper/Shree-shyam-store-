# ViewModel Boundaries

**Status:** Reporting and inventory extraction merged; billing cart state boundary implemented with compatibility delegates

## Current boundary

`ShopViewModel` remains the application navigation and compatibility facade for identity/session, settings, billing checkout, inventory, udhaar, sync/restore, and cross-screen actions. `ReportsViewModel` owns the reporting read model for sales history, sale-item queries, and sales CSV export. `BillingCartState` now owns ephemeral cart contents and cart-total derivation; `ShopViewModel` exposes compatibility delegates while billing screens migrate. Customer state remains on `ShopViewModel` because Udhaar screens still own customer mutations and balances. The extracted components use the existing manual-constructor-injection pattern and do not change the Room persistence contract.

Home, Billing, and catalog/stock screens continue to receive `ShopViewModel` during the compatibility period. `InventoryViewModel` owns catalog/product state and inventory edits, while `BillingCartState` owns only ephemeral cart state and `ShopViewModel` retains checkout, billing quick-add, navigation, exports, and sync triggers. This is a temporary compatibility seam, not a second source of truth: authoritative writes still flow through `ShopRepository` and Room transactions.

| Boundary | Owns now | Does not own |
| --- | --- | --- |
| `ReportsViewModel` | Sales report flow, sale-item query, sales CSV export | Customer/Udhaar state, navigation, identity/session, billing writes, invoice sharing, sync/restore |
| `InventoryViewModel` | Category/product flows, category mutations, product edits, audited stock adjustment delegation | Checkout/cart, billing quick-add, navigation, exports, sync orchestration |
| `BillingCartState` | Ephemeral product quantities, stock-aware cart mutations, paise cart-total derivation | Persistence, checkout authorization, sale writes, inventory deductions, sync, navigation |
| `ShopViewModel` | Existing cross-feature facade, checkout/payment orchestration, billing quick-add, navigation, identity/session, Udhaar, sync/restore, compatibility delegates | Report state/export, catalog/stock methods, and cart state ownership after call-site migration |
| `ShopRepository` | Room transactions, business writes, sync stamps, restore boundaries | Compose state or navigation |

## Invariants preserved

This is an ownership refactor only. Checkout, inventory underflow, money minor units, immutable udhaar events, stable sync/outbox semantics, restore atomicity, app-lock state, and cloud privacy boundaries are unchanged. Cart state remains ephemeral and stock-aware; authoritative checkout remains in the existing repository/DAO transaction. Report flows remain Room-backed and reactive. CSV export still derives the shop display name from local DataStore and exports only the caller-selected sales list.

## Extraction sequence

The next slices should complete billing/checkout migration, then extract udhaar/ledger, settings/identity, and sync/restore orchestration. Each extraction should first move read state, then move mutations behind the same repository boundary, then remove the corresponding compatibility methods from `ShopViewModel` only after screen and test call sites are migrated. Navigation migration is intentionally separate so state ownership can stabilize before routing changes.
