# Project Charter

Status: Draft governance source of truth.

## Product

Shree Shyam Store is a professional cloud-backed, offline-capable Kotlin Android shop register for a small Indian kiryana/general store.

## Objective

Deliver a reliable daily-use shop app that supports:

- Fast billing.
- Simple product and stock management.
- Accurate udhaar tracking.
- Useful reports.
- Easy invoice sharing.
- Firebase-backed owner access and shop-data recovery.
- English default UI with Hindi as a complete second language.
- A focused kiryana-shop workflow using the native Kotlin Android stack.

## Primary Users

- Shop owner.
- Trusted shop operator using the owner's device.

## Smallest Credible Release

The smallest credible release should include:

- Firebase Auth owner sign-in and shop ownership foundation.
- Firestore-backed shop profile/data recovery foundation.
- Local cache/offline behavior with approved conflict handling.
- First-launch shop setup.
- Product/category/stock management.
- Billing with stock validation.
- Cash, manual UPI, and udhaar records.
- Invoice text/PDF/share.
- Udhaar ledger.
- Basic reports.
- Verified English and Hindi resources.

## Explicit Exclusions

- Bluetooth thermal printer support.
- Multi-store or multi-branch support.
- Staff roles and permissions implementation. Future membership modeling is allowed in Firebase foundation planning.
- Real UPI payment confirmation.
- AI decisioning.
- Loan/credit scoring.
- GST/tax filing or legal compliance claims beyond owner-approved user-entered invoice fields.

## Release Gates

- Debug build passes.
- Relevant unit tests pass.
- Instrumented tests compile for Android-facing changes.
- Emulator launch proof exists for UI/navigation changes.
- Hindi and English resources are valid for touched screens.
- No destructive Room migration behavior is introduced.
- Security/privacy checklist is reviewed for auth, backup, payments, permissions, and release work.
- Firebase security rules, privacy, App Check, abuse/cost controls, and restore behavior are approved before billing hardening.

## Owner-Gated Decisions

- `TBD - owner decision required`: production credential hardening.
- `TBD - owner decision required`: Firebase project/config, Auth rollout details, Firestore region, App Check enforcement timing, and cost controls.
- `Accepted`: hybrid Firestore-canonical/Room-cache architecture and shop ownership boundary.
- `Accepted`: final app id and package are `com.harrylabs.shreeshyamstore`; release icon and signing setup remain owner-gated.
- `TBD - owner decision required`: backup policy and format.
- `TBD - owner decision required`: invoice tax/GST/legal fields.
- `TBD - owner decision required`: whether unused AI/API scaffolding should be removed.
