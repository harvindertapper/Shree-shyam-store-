# Shree Shyam Store Build Checklist

## Product Target

Build a professional cloud-backed, offline-capable Android shop register app for small retail stores.

Default language: **English**  
Second language: **Hindi**

The app should help a shop owner:

- Manage products and stock.
- Create bills quickly.
- Track cash, UPI, and udhaar sales.
- Maintain customer udhaar ledger.
- Generate/share invoices.
- View useful daily/monthly reports.
- Back up important business data.
- Recover shop data after phone loss, clear storage, reinstall, or device change.

## Current App Inventory

- Local owner registration and login.
- Firebase Auth/Firestore foundation is now mandatory MVP work but is not implemented yet.
- Final app identity is `com.harrylabs.shreeshyamstore` after the approved identity rename packet.
- Room schema version 2 is implemented. Its approved v1-only reset uses `fallbackToDestructiveMigrationFrom(true, 1)`.
- No real shop inventory has been entered yet; migrations from Room v2 onward require an intentional migration or a new explicit owner-approved reset.
- First-launch shop setup.
- Room database for local storage.
- DataStore for settings/session.
- Product and category management.
- Opening stock and stock adjustment.
- Billing cart.
- Cash, UPI, and udhaar payment modes.
- Sale and sale-item records.
- Customer udhaar ledger.
- Udhaar payment entry.
- Basic reports.
- Shop settings with QR image selection.
- Debug APK build verified.
- Emulator launch verified.

## Immediate Fix Checklist

- [ ] Convert user-facing UI text to string resources.
- [ ] Add English default strings.
- [ ] Add Hindi translations in `values-hi`.
- [ ] Clean corrupted/mojibake Hindi text.
- [ ] Confirm app starts in English by default.
- [ ] Add language switch in settings.
- [ ] Save selected language preference.
- [ ] Apply language on app restart.

## MVP Feature Checklist

- [ ] Register and login work reliably.
- [ ] Firebase Auth owner sign-in works reliably.
- [x] App identity rename to `com.harrylabs.shreeshyamstore` is complete before Firebase setup (`b7d92f2`).
- [ ] Owner UID maps to one or more shop memberships.
- [ ] Firestore security rules protect shop data by owner/member access.
- [ ] App Check and cost/budget guardrails are reviewed before product sync.
- [ ] Cloud-backed shop profile restores after reinstall/clear storage.
- [ ] Product/category/settings sync and restore pass before real inventory entry.
- [ ] Product add/edit supports piece, weight, and volume unit setup.
- [ ] Product add/edit preview calculator works.
- [ ] Product add/edit supports inline category creation with duplicate prevention.
- [ ] Room/local cache strategy is documented and tested for offline use.
- [ ] First-launch setup stores shop details.
- [ ] Product add/edit works.
- [ ] Category add/rename works.
- [ ] Opening stock entry works.
- [ ] Stock adjustment history works.
- [ ] Billing cart quantity controls work.
- [ ] Billing prevents invalid/negative stock when tracking is enabled.
- [ ] Cash sale saves correctly.
- [ ] UPI sale saves correctly as a manual payment record.
- [ ] Udhaar sale creates customer credit transaction.
- [ ] Bill success screen shows correct invoice details.
- [ ] Invoice text copy works.
- [ ] Reports show correct daily/monthly totals.
- [ ] Low-stock dashboard is accurate.

## Invoice Sharing Checklist

- [ ] Define invoice data model used by receipt/PDF/share.
- [ ] Create clean invoice preview screen.
- [ ] Generate PDF invoice locally.
- [ ] Save PDF to app cache or user-selected location.
- [ ] Share invoice PDF through Android share sheet.
- [ ] Add direct WhatsApp share intent where available.
- [ ] Include shop name, phone, bill number, date, items, payment mode, and total.
- [ ] Support English and Hindi invoice labels.
- [ ] Handle missing WhatsApp gracefully.
- [ ] Add tests for invoice text/data formatting.

## Later Feature Checklist

- [ ] Manual backup/export/import if still required after cloud recovery.
- [ ] CSV export for products, sales, customers, and udhaar.
- [ ] Barcode/product-code support.
- [ ] Purchase entry and supplier module.
- [ ] Profit reports using purchase price.
- [ ] Discounts.
- [ ] Returns/refunds.
- [ ] Optional GST/tax fields.
- [ ] Customer phone/WhatsApp shortcuts.
- [ ] App lock/PIN.
- [ ] Password reset/recovery.
- [ ] App icon and brand polish.
- [ ] Release signing setup.
- [ ] Versioning and changelog.

## Deferred

- [ ] Bluetooth thermal printer support.
- [ ] Multi-store support.
- [ ] Staff roles and permissions implementation.
- [ ] Real UPI payment confirmation.

## QA Checklist

- [ ] Fresh install opens correctly.
- [ ] Register owner account.
- [ ] Login with saved owner account.
- [ ] Complete first-launch setup.
- [ ] Add category.
- [ ] Add product.
- [ ] Add opening stock.
- [ ] Create cash bill.
- [ ] Create UPI bill.
- [ ] Create udhaar bill.
- [ ] Add udhaar payment.
- [ ] Verify stock reduced after sale.
- [ ] Verify reports totals.
- [ ] Restart app and confirm session/settings persist.
- [ ] Clear storage and confirm cloud restore path is available after sign-in.
- [ ] Reinstall and confirm cloud restore path is available after sign-in.
- [ ] Second device sign-in restores owner/shop profile according to approved sync rules.
- [ ] Inventory restore gate restores categories, products, settings, unit fields, and rates before real inventory entry.
- [ ] Offline mode shows cached data according to approved conflict rules.
- [ ] Switch language to Hindi.
- [ ] Switch language back to English.

## Required Verification

```powershell
.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon
```

For Android/emulator-facing work:

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --stacktrace --console=plain --no-daemon
```
