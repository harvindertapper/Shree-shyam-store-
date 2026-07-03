# Shree Shyam Store

Shree Shyam Store is a Kotlin Android shop-management app customized for a small Indian kiryana/general store.

The product goal is reliable daily shop use:

- Fast billing.
- Product and stock management.
- Cash, manual UPI, and udhaar sale records.
- Customer udhaar ledger.
- Basic reports.
- Private owner reports for stock value/profit are planned in the professional delivery route.
- Invoice generation and sharing.
- English default UI with Hindi as the second supported language.

Future work should follow the Shree Shyam Store kiryana-shop scope and native Kotlin Android stack.

## Stack

- Kotlin
- Jetpack Compose
- Room database
- DataStore preferences
- Gradle Kotlin DSL
- Min Android API 24

## Source Of Truth

Read these before implementation work:

- `AGENTS.md`
- `docs/PRODUCT_SPEC.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/superpowers/plans/2026-07-02-professional-delivery-plan.md`
- `docs/DELIVERY_WORKFLOW.md`
- `docs/SCREEN_FLOW.md`
- `docs/DATA_MODEL.md`
- `docs/governance/*.md`
- `APP_BUILD_CHECKLIST.md`

## Build

```powershell
.\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:testDebugUnitTest --stacktrace --console=plain --no-daemon
```

For Android/emulator-facing changes:

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest --stacktrace --console=plain --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --stacktrace --console=plain --no-daemon
```

## Notes

- The app is offline-capable today, but Firebase Auth/Firestore cloud recovery is mandatory professional foundation work before trusted real inventory and billing hardening.
- Current professional route prioritizes restore/sync trust, fast optional-customer billing, loose item calculation, quick udhaar, private stock/profit views, daily close summary, and optional invoice/share.
- UPI is only a manually recorded payment mode unless a future approved integration verifies payment.
- Do not commit secrets, signing passwords, real customer data, or production API keys.
- Legacy AI Studio/Gemini scaffolding may exist from project creation and should not define product scope.
