# Screen Flow

## Language Behavior

- Default app language: English.
- Hindi is the secondary supported language.
- Language selection should live in Settings.
- UI copy must not mix English and Hindi in the same string unless it is a proper product name or unavoidable business term.

## Main Flow

1. Welcome
2. Owner Login
3. First Launch Setup
4. Home Dashboard
5. Products and Stock
6. Billing
7. Payment
8. Sale Saved / Optional Invoice
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

## Owner Login

Purpose:

- Allow cloud-backed owner access through Firebase Auth and Android Credential Manager Sign in with Google.

Expected behavior:

- Shows a Sign in with Google button.
- Successful Google auth restores the owner profile and active shop from Firestore.
- If no shop profile exists yet, route to First Launch Setup.
- Failed or cancelled auth shows a clear localized error without storing tokens locally.

Future improvements:

- PIN/app lock.
- Account recovery and support workflow.
- Optional phone OTP only if owner approves cost and abuse controls.

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
- Show pending sync status once cloud sync is enabled.
- Show or link to end-of-day "Aaj ka hisaab".
- Provide shortcuts to billing, products, opening stock, and udhaar.

## Products and Stock

Purpose:

- Manage products, categories, and stock levels.

Expected behavior:

- Search products.
- Filter by category.
- Add/edit product.
- Add categories.
- Add a new category inline while adding/editing a product.
- Track active/inactive products.
- Support piece, weight, and volume setup.
- Allow stock tracking to be enabled/disabled per product.
- Show quick-added products that need later cleanup.
- Show low stock indicators.
- Open stock adjustment screen.

## Billing

Purpose:

- Build a cart and create a sale.

Expected behavior:

- Search/filter products.
- Add products to cart.
- Change quantity.
- For loose items, support quantity-to-amount and amount-to-quantity entry.
- Allow per-line rate override without changing product master price.
- Allow missing products to be quick-added with minimal fields so billing does not stop.
- Remove items.
- Show cart total.
- Prevent invalid quantities.
- Prevent tracked stock from going below zero unless explicitly allowed later.
- Do not require customer selection for ordinary cash/UPI sales.

## Payment

Purpose:

- Choose payment mode and complete bill.

Expected behavior:

- Cash sale saves as paid.
- UPI sale saves as manually recorded UPI payment.
- Cash/UPI sales can be saved without a customer.
- Udhaar sale requires/selects customer.
- New customer can be created during udhaar billing.
- Completion creates sale, sale items, stock adjustment, and udhaar transaction if needed.
- Owner can save the sale without generating an invoice.

## Sale Saved / Optional Invoice

Purpose:

- Confirm saved sale and provide invoice actions only when needed.

Expected behavior:

- Show bill number, items, total, date, payment mode.
- Show customer only when the sale has one.
- Copy invoice text.
- Generate PDF invoice.
- Share invoice through Android share sheet/WhatsApp when available.
- Handle missing WhatsApp gracefully.

## Reports

Purpose:

- Show useful business totals.

Expected behavior:

- Daily totals.
- Monthly totals.
- Payment-mode breakdown.
- Invoice count.
- Recent bill list.
- Private Owner Desk for stock value, category-wise stock value, and profit.
- Profit reports use sale-item purchase-cost snapshots.
- Customer-facing screens must not expose purchase cost, profit, or stock value.

Future improvements:

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
- Quick-add credit/payment from customer row or detail.
- Record payment.
- Show transaction history.
- Generate WhatsApp/share reminder message without requesting Contacts permission.

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
