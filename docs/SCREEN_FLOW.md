# Screen Flow

## Language Behavior

- Default app language: English.
- Hindi is the secondary supported language.
- Language selection should live in Settings.
- UI copy must not mix English and Hindi in the same string unless it is a proper product name or unavoidable business term.

## Main Flow

1. Welcome
2. Login or Register
3. First Launch Setup
4. Home Dashboard
5. Products and Stock
6. Billing
7. Payment
8. Bill Success / Invoice
9. Reports
10. Udhaar
11. Settings

## Welcome

Purpose:

- Show brand/store welcome.
- Route user based on session and setup state.

Expected behavior:

- If not logged in, go to Login.
- If logged in and setup is complete, go to Home.
- If logged in and setup is incomplete, go to First Launch Setup.

## Login / Register

Purpose:

- Allow local owner account access.

Expected behavior:

- Register validates username, email, password, and confirm password.
- Login accepts username or email.
- Successful auth saves local session.
- Failed auth shows clear error.

Future improvements:

- PIN/app lock.
- Password recovery/reset.
- Better password storage.

## First Launch Setup

Purpose:

- Capture core shop settings.

Expected behavior:

- Save shop name and owner phone.
- Mark setup complete.
- Navigate to Home.

## Home Dashboard

Purpose:

- Quick view of today business and common actions.

Expected behavior:

- Show today's sales summary.
- Show cash/UPI/udhaar breakdown.
- Show low-stock alerts.
- Provide shortcuts to billing, products, opening stock, and udhaar.

## Products and Stock

Purpose:

- Manage products, categories, and stock levels.

Expected behavior:

- Search products.
- Filter by category.
- Add/edit product.
- Add categories.
- Track active/inactive products.
- Show low stock indicators.
- Open stock adjustment screen.

## Billing

Purpose:

- Build a cart and create a sale.

Expected behavior:

- Search/filter products.
- Add products to cart.
- Change quantity.
- Remove items.
- Show cart total.
- Prevent invalid quantities.
- Prevent tracked stock from going below zero unless explicitly allowed later.

## Payment

Purpose:

- Choose payment mode and complete bill.

Expected behavior:

- Cash sale saves as paid.
- UPI sale saves as manually recorded UPI payment.
- Udhaar sale requires/selects customer.
- New customer can be created during udhaar billing.
- Completion creates sale, sale items, stock adjustment, and udhaar transaction if needed.

## Bill Success / Invoice

Purpose:

- Show completed bill and provide invoice actions.

Expected behavior:

- Show bill number, items, total, date, payment mode.
- Copy invoice text.
- Future: generate PDF.
- Future: share invoice through WhatsApp/share sheet.

## Reports

Purpose:

- Show useful business totals.

Expected behavior:

- Daily totals.
- Monthly totals.
- Payment-mode breakdown.
- Invoice count.
- Recent bill list.

Future improvements:

- Profit report.
- Customer-wise report.
- Product-wise sales.
- Export CSV/PDF.

## Udhaar

Purpose:

- Track customer credit and payments.

Expected behavior:

- Search customers.
- Show total outstanding.
- Filter debtors.
- View customer detail.
- Record payment.
- Show transaction history.

## Settings

Purpose:

- Manage app and shop settings.

Expected behavior:

- Edit shop name and phone.
- Upload static UPI/Paytm QR image.
- Toggle welcome chant.
- Switch language between English and Hindi.
- Logout.

Future improvements:

- Backup/restore.
- App lock/PIN.
- Release/about screen.
