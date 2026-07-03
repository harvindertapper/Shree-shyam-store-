# Security and Privacy Checklist

Status: Governance source of truth. Review this before auth, data, payment, backup, API, permission, migration, or release work.

## Sensitive Data

- [ ] Shop name and owner phone are treated as sensitive business profile data.
- [ ] Customer names and phone numbers are treated as sensitive personal/business data.
- [ ] Sales, invoices, udhaar balances, and stock records are treated as sensitive business records.
- [ ] Purchase prices, profit reports, stock valuation, and category-wise stock value are treated as owner-private business data.
- [ ] QR image URIs are treated as user-selected local content.

## Credentials and Secrets

- [ ] No API keys, signing passwords, tokens, or real secrets are committed.
- [ ] `.env` is not read unless the user explicitly approves it.
- [ ] `.env.example` contains placeholders only.
- [ ] Password storage is not worsened.
- [ ] Production credential hardening is tracked before release.

## Local Storage

- [ ] DataStore stores preferences and non-secret session flags only.
- [ ] Room stores auditable business records.
- [ ] No tokens or secrets are stored in DataStore, Room, strings, or source files.
- [ ] Backup/export/import behavior is explicit and user-driven.

## Database and Migration

- [ ] Entity changes include a migration plan.
- [ ] Existing shop data is preserved.
- [ ] Destructive migration is not used for production schema changes.
- [ ] Migration tests or before/after evidence are provided.

## Payments and Invoices

- [ ] Cash is recorded as user-entered cash payment.
- [ ] UPI is recorded only as manual UPI payment unless verified integration exists.
- [ ] Udhaar sale creates a credit ledger transaction.
- [ ] Invoices reflect saved sale and sale-item snapshots.
- [ ] Share/export actions require explicit user action.
- [ ] Invoice generation is optional and user-triggered after sale.
- [ ] WhatsApp/share reminder messages for udhaar require explicit user action and do not require Contacts permission.

## Android Permissions

- [ ] New dangerous permissions have owner approval.
- [ ] Permission reason is documented in product/spec docs.
- [ ] User-denied permission path is handled.
- [ ] No Bluetooth printer work is added without approval.

## API and Network

- [ ] New network behavior has owner approval.
- [ ] API client pattern and error handling are documented.
- [ ] No sensitive data is sent without documented purpose and user expectation.
- [ ] AI/API features do not make business, financial, legal, tax, or payment-success claims.

## Firebase Auth And Firestore

- [ ] Firebase project ownership, environment separation, and Android app registration are documented.
- [ ] Firebase app registration, SHA-1/SHA-256 keys, Google Sign-In, and Play Store identity use final application id `com.harrylabs.shreeshyamstore`.
- [ ] Public client Firebase config is handled intentionally; service-account keys and secrets are never committed.
- [ ] Auth provider choice is owner-approved. Default planning assumption is Google Sign-In first; phone OTP is later/optional due to cost and abuse risk.
- [ ] Owner UID to shop membership mapping is documented and tested.
- [ ] Firestore security rules enforce per-shop ownership/membership on every collection.
- [ ] App Check, abuse controls, quota/cost monitoring, and rate-limit strategy are reviewed before production.
- [ ] App Check uses the debug provider only for emulator/development; Play Integrity is the production direction.
- [ ] Firebase budget alerts are configured as monitoring only and are not described as a hard spending cap.
- [ ] Android Firebase implementation uses the main `firebase-auth` and `firebase-firestore` modules.
- [ ] Sign in with Google uses Android Credential Manager rather than legacy Google Sign-In client APIs.
- [ ] Budget/cost guardrails are reviewed before product/category/settings sync.
- [ ] Firestore indexes, backup/export, retention, and delete/account recovery expectations are documented.
- [ ] No customer phone numbers, sales records, invoices, QR URIs, or udhaar balances are exposed in logs, analytics, screenshots, crash reports, or public documents.
- [ ] Offline writes, conflict handling, and restore flows are tested for clear storage, reinstall, and second device.
- [ ] Restore gates prevent the app from showing an empty shop as successful restore when cloud restore fails.
- [ ] Sync pending/error states are visible to the owner and do not expose sensitive data in logs.
- [ ] Firestore offline persistence is not treated as a complete conflict solution; sales, stock, and udhaar require idempotency, append-only records, and reconciliation rules.
- [ ] Security rules have automated or emulator-backed tests before cloud data sync is accepted.


## FR-G Firebase Auth/Profile Status - 2026-07-01

- Local Android implementation status: PASS.
- Firestore rules emulator status: PASS, 23 tests passing.
- Live Firebase status: Firestore `(default)` exists and rules are deployed. Manual Google Sign-In/shop-profile QA is pending.
- Live database setting observed: Standard edition, Mumbai `asia-south1`, delete protection disabled, point-in-time recovery disabled.
- Recommended follow-up: enable Firestore delete protection before broad sync/release.
- Manual Google Sign-In QA is still pending: old AVD had outdated Play Services; updated Play emulator reaches the login screen and opens Google Credential UI safely, but final success proof needs account selection/authorization on a stable emulator or physical phone with Firebase Authentication Google provider enabled.
- App Check and cost controls remain FR-G2 gates before product/category/settings sync.

## Release

- [ ] `android:allowBackup` policy is reviewed before release.
- [ ] Package/application id is owner-approved.
- [ ] Signing credentials are external to repo.
- [ ] Versioning and changelog are documented.
