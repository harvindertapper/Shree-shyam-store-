# ViewModel Boundaries

**Status:** First extraction implemented on `refactor/reports-viewmodel` for review

## Current boundary

`ShopViewModel` remains the application navigation and compatibility facade for identity/session, settings, billing, inventory, udhaar, sync/restore, and cross-screen actions. `ReportsViewModel` now owns the reporting read model for sales history, sale-item queries, and sales CSV export. Customer state remains on `ShopViewModel` because Udhaar screens still own customer mutations and balances. It receives the existing `ShopRepository` and `SettingsDataStore` through the same manual-constructor-injection pattern used by the app; no DI framework or persistence contract changed.

Home and Reports screens receive both ViewModels deliberately. They use `ShopViewModel` for navigation, settings, inventory dashboard state, customer lookup, and invoice sharing, while sales report state and sales export come from `ReportsViewModel`. This dual injection is a temporary compatibility seam, not a second source of truth: both ViewModels observe the same Room-backed repository flows.

| Boundary | Owns now | Does not own |
| --- | --- | --- |
| `ReportsViewModel` | Sales report flow, sale-item query, sales CSV export | Customer/Udhaar state, navigation, identity/session, billing writes, invoice sharing, sync/restore |
| `ShopViewModel` | Existing cross-feature facade and all not-yet-extracted domains | Report state/export after this slice |
| `ShopRepository` | Room transactions, business writes, sync stamps, restore boundaries | Compose state or navigation |

## Invariants preserved

This is an ownership refactor only. Checkout, inventory underflow, money minor units, immutable udhaar events, stable sync/outbox semantics, restore atomicity, app-lock state, and cloud privacy boundaries are unchanged. Report flows remain Room-backed and reactive. CSV export still derives the shop display name from local DataStore and exports only the caller-selected sales list.

## Extraction sequence

The next slices should extract one domain at a time: inventory/catalog, billing/cart, udhaar/ledger, settings/identity, and sync/restore orchestration. Each extraction should first move read state, then move mutations behind the same repository boundary, then remove the corresponding compatibility methods from `ShopViewModel` after screen and test call sites are migrated. Navigation migration is intentionally separate so state ownership can stabilize before routing changes.
