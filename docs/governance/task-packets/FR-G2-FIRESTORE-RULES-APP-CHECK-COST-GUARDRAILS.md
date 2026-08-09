# FR-G2-FIRESTORE-RULES-APP-CHECK-COST-GUARDRAILS

## task_id

FR-G2-FIRESTORE-RULES-APP-CHECK-COST-GUARDRAILS

## goal

Establish Firestore rules, App Check posture, and budget/cost guardrails before product/category/settings sync.

## scope_paths

- Firestore rules/test files if approved by repo structure.
- `docs/governance/05_SECURITY_PRIVACY_CHECKLIST.md`
- `docs/governance/FIREBASE_CLOUD_SYNC_ARCHITECTURE.md`
- `docs/DATA_MODEL.md`
- `app/src/test/**` or emulator rules tests if configured.

## dependencies

- `FR-G-FIREBASE-AUTH-SHOP-PROFILE`

## constraints

- No broad data sync until rules pass.
- Enforce signed-in membership on every `shops/{shopId}` path.
- Block signed-out and cross-shop access.
- Use the App Check debug provider only for emulator/development; Play Integrity is the production direction.
- Treat Firebase budget alerts as monitoring, not a hard spending cap.
- Do not store service-account keys in repo.

## acceptance_criteria

- Rules protect user/shop/member/profile/settings/category/product paths.
- Owner/member access works only within the authorized shop.
- App Check strategy and budget/cost guardrails are documented.
- Cost controls include usage monitoring, conservative query/write design, and owner-reviewed alert thresholds.
- Rules tests or emulator-backed proof exist.

## required_evidence

- Rules/test evidence.
- Security checklist update.
- Cost guardrail notes.

## review_owner

Security/governance plus owner.

## do_not_touch

- Product UI, Billing UI, sale persistence.
- Sales/stock/udhaar cloud sync.
- Package rename.
- Secrets/service-account keys.
