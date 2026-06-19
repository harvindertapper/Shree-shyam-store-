# FR-C-QUANTITY-PRICE-CALCULATOR

> **Completed - do not rerun.** Checkpoint: `f36d613`. Old `com.example` scope paths below record the pre-FR-P execution state; current package is `com.harrylabs.shreeshyamstore`.

## task_id

FR-C-QUANTITY-PRICE-CALCULATOR

## goal

Implement a pure Kotlin quantity/price calculator for piece, weight, volume, and per-line rate override.

## scope_paths

- `app/src/main/java/com/example/utils/**`
- `app/src/test/java/com/example/**`

## dependencies

- `FR-A-V2-DATA-CALC-DESIGN`

## constraints

- No Room, Compose, Firebase, or Android context dependency.
- Money is `Long` paise.
- Quantity is `Long` base units.
- Fractional pieces are invalid.
- Use effective billed rate when override is present.

## acceptance_criteria

- `Rs.47/kg` and `160g` returns `752` paise.
- `Rs.47/kg` and `Rs.30` returns about `638g`.
- Override example `Rs.25/kg` original and `Rs.22/kg` effective calculates from `Rs.22/kg`.
- Invalid zero, negative, malformed, and unsupported precision inputs are rejected.

## required_evidence

- Unit tests for piece, weight, volume, amount-to-quantity, quantity-to-amount, rounding, and override.
- Build/unit test command results.

## review_owner

QA plus data reviewer.

## do_not_touch

- Room entities, DAOs, database version, migrations.
- Product UI, Billing UI.
- Firebase/Auth/Firestore config or code.
- Package/application id rename.
