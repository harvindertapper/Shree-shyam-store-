# FR-K-ROOM-UUID-PRIMARY-KEYS

## task_id

FR-K-ROOM-UUID-PRIMARY-KEYS

## goal

Refactor local Room database tables to use client-side generated UUID strings (`localUuid`) as primary keys and foreign keys for entity relationships, instead of SQLite auto-increment `Long` IDs, to ensure data sync and restore integrity across devices.

## scope_paths

- `app/build.gradle.kts`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/Entities.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/Daos.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/AppDatabase.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/data/ShopRepository.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/viewmodel/ShopViewModel.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/ProductsAndStockScreen.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/BillingAndPaymentScreen.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/UdhaarScreen.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/ui/screens/ReportsScreen.kt`
- `app/src/main/java/com/harrylabs/shreeshyamstore/MainActivity.kt`
- `app/src/test/java/com/harrylabs/shreeshyamstore/**`
- `docs/DATA_MODEL.md`

## dependencies

- `FR-B-ROOM-V2-RESET` (completed)

## constraints

- Room schema version must be incremented to 3. Destructive reset is allowed ONLY from versions 1 and 2 (using `fallbackToDestructiveMigrationFrom(true, 1, 2)`). Broad `fallbackToDestructiveMigration()` is forbidden.
- Enable `exportSchema = true` in `AppDatabase.kt` and configure `room.schemaLocation` in `app/build.gradle.kts` to save schema JSON files under `app/schemas`.
- All syncable entity `@Insert` methods in DAOs must return `Unit` (except `UserDao` which remains out-of-scope).
- All foreign keys (e.g., `Product.categoryId`, `SaleItem.saleId`, `SaleItem.productId`, `UdhaarTransaction.customerId`, `StockAdjustment.productId`, `Sale.customerId`) must be refactored to use `String` values representing the corresponding entity's `localUuid`.
- All DAOs and repository methods must update their queries to use String UUID lookups instead of Long IDs.
- The local `User` table (with `User.id: Long`) and local auth logic remain completely untouched in this packet (they will be refactored/removed in FR-G).


## acceptance_criteria

- Room schema version 3 is successfully implemented with String UUID primary keys for categories, products, sales, sale items, customers, udhaar transactions, and stock adjustments.
- Foreign key relations successfully use UUIDs.
- Code compiles, and all existing unit tests are updated and pass.
- Database schema tests verify destructive reset from older versions.

## required_evidence

- Code diffs of the changed entity and DAO files.
- Verification that the project compiles and unit tests pass.
- Room schema export files updated.

## review_owner

Data reviewer plus delivery manager.

## do_not_touch

- Firebase authentication logic (until next packet).
- Welcome sound/chant assets.
