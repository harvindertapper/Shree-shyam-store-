# FR-K Room UUID Primary Keys Acceptance

## Goal

- Refactor local Room database tables to use client-side generated UUID strings (`localUuid`) as primary keys and foreign keys for entity relationships, instead of SQLite auto-increment `Long` IDs, to ensure data sync and restore integrity across devices.

## Scope Paths

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
- `app/src/test/java/com/harrylabs/shreeshyamstore/data/RoomV3UUIDSchemaTest.kt`
- `docs/DATA_MODEL.md`

## Acceptance Criteria

- Room database version 3 is successfully implemented with String UUID primary keys for categories, products, sales, sale items, customers, udhaar transactions, and stock adjustments.
- Foreign key relations successfully use UUIDs.
- Code compiles, and all existing unit tests are updated and pass.
- Database schema tests verify destructive reset from older versions.

## Evidence

- **Database version 3**: Verified Room database version 3 implementation in `AppDatabase.kt`.
- **Destructive Migration Tests**:
  - `RoomV3UUIDSchemaTest.kt` has `destructiveMigrationWorksFromV1` and `destructiveMigrationWorksFromV2` which create file-backed v1 and v2 sqlite databases, open them with Room with destructive fallback, and assert that legacy categories are deleted and version 3 categories are seeded.
- **Category Seeding**: Category seeding is updated to exclude legacy `id` and is safely executed in the `onOpen` callback if the `categories` table is empty.
- **Blank Category Checks**:
  - `AddEditProductScreen` automatically selects the first valid category upon load for new products and displays the appropriate localized toast (`product_category_error`) if saving is attempted with a blank category.
  - `OpeningStockScreen` fast-add checks if the category is blank and displays the `product_category_error` toast.
  - `BillingAndPaymentScreen` Quick Add dialog selects the first valid category upon load.
- **Verification Run Results**:
  - Debug compilation (`:app:assembleDebug`) succeeded.
  - Unit tests execution (`:app:testDebugUnitTest`) succeeded with **21 tests completed, 0 failures, 0 errors**.
  - Android Test compilation (`:app:assembleDebugAndroidTest`) succeeded.

## Security/Privacy Review

- Destructive reset policy is restricted to database versions 1 and 2 using `.fallbackToDestructiveMigrationFrom(true, 1, 2)`.
- Client-generated UUIDs prevent ID collisions but are not a security mechanism. Security rules on Firebase/Firestore will protect sync data.

## Source-of-Truth Updates

- `docs/DATA_MODEL.md` updated with version 3 schema details.
- `MainActivity.kt` explicitly included in task packet scope paths list.

## Result

- PASS.
- `FR-G Firebase Auth with Credential Manager Google Sign-In` may start.
