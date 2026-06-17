# FR-B-ROOM-V2-RESET

## task_id

FR-B-ROOM-V2-RESET

## goal

Implement Room v2 reset schema for cloud-ready, unit-ready local storage before real inventory entry.

## scope_paths

- `app/src/main/java/com/example/data/Entities.kt`
- `app/src/main/java/com/example/data/Daos.kt`
- `app/src/main/java/com/example/data/AppDatabase.kt`
- `app/src/main/java/com/example/data/ShopRepository.kt` only for compile-safe entity API adaptation.
- `app/src/test/java/com/example/**`
- `docs/DATA_MODEL.md`

## dependencies

- `FR-A-V2-DATA-CALC-DESIGN`
- `FR-C-QUANTITY-PRICE-CALCULATOR`

## constraints

- Room v2 reset is allowed because no real shop inventory has been entered.
- Reset policy must be explicit and documented.
- Do not use broad production destructive migration as the final long-term policy.
- Do not add Firebase sync.

## acceptance_criteria

- Room schema has v2 fields for paise, base quantities, sync fields, soft delete, sale status, override snapshots, and purchase cost snapshots.
- App compiles after entity API adaptation.
- Existing seeded categories still work or are intentionally updated.
- Reset policy is documented.

## required_evidence

- Files changed.
- Build and unit test results.
- Room schema/reset note.
- Confirmation no real inventory migration was required.

## review_owner

Data reviewer plus security/governance.

## do_not_touch

- Product Add/Edit UI.
- Billing/cart UI.
- Firebase/Auth/Firestore config or code.
- Package/application id rename.
- Welcome sound/assets.

