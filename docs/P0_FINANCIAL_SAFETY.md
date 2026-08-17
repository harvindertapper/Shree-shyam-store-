# P0 Financial Safety Slice

**Branch:** `feat/p0-financial-safety`

**Base:** `main` at `f380bf09c9e68fa34d80a71e546fc041ea7e7f35`

**Status:** Implementation in progress; reviewable as a focused pull request after release gates pass.

## Approved business policies

| Policy | Decision |
| --- | --- |
| Tracked inventory | `REJECT_UNDERFLOW`; checkout cannot include more than current tracked stock |
| Udhaar credit | Hard projected-balance credit limit; a bill is rejected when current balance plus new credit exceeds the customer limit |
| Received payment | Validated at the repository boundary; payments may exceed outstanding balance and represent a customer credit until a dedicated credit-balance presentation is added |
| UPI | Manual settlement only for now; an actual gateway/reference workflow is a follow-up release boundary |

## Implemented in this slice

The checkout transaction now parses supported payment modes, rejects empty or unnamed bills, rejects duplicate bill numbers before any write, validates finite positive quantities and valid prices, verifies each line total against rounded unit-price multiplication, and verifies the authoritative rounded bill total against all lines.

Tracked stock is conditionally deducted only when sufficient non-negative stock exists. A failed deduction aborts the Room transaction, so sale rows, sale items, stock adjustments, and udhaar credits do not remain partially written. The ViewModel also prevents cart quantities above tracked stock and disables settlement controls while a checkout is in flight.

Udhaar checkout validates an active customer, validates the customer credit limit and current balance, and rejects a projected balance above the approved hard limit. New customers created from the payment screen are inserted in the same Room transaction as the bill, avoiding orphan customers when checkout fails.

Received udhaar payments now pass through a repository method that validates finite positive amounts, requires an active customer, rounds the stored amount, and uses the typed `PAYMENT` state. Balance aggregation in DAO, billing UI, udhaar UI, and PDF reporting recognizes only `CREDIT` and `PAYMENT`; unknown types are not silently treated as payments.

## Test coverage added

`CheckoutInvariantsTest` covers line-total mismatch, stock-underflow rollback, hard credit-limit rejection, duplicate bill-number replay, successful rounded checkout, and validated received-payment recording. `CommerceValidationTest` covers two-decimal HALF_UP rounding, line/bill calculation, and non-finite amount rejection. These suites are now part of the required stable Android CI test selection.

## Deliberately deferred

This slice does not claim complete enterprise financial readiness. The following remain required before production rollout: migration from `Double` to minor-unit money values, immutable ledger reversal/correction events, actor and authorization metadata, a separate customer-credit balance presentation, stable global sync IDs and conflict policy, non-destructive Room migration/recovery, payment reference reconciliation, production application identity/signing, and a staged release build.

## Verification commands

```bash
./gradlew --no-daemon --stacktrace compileDebugKotlin
./gradlew --no-daemon --stacktrace testDebugUnitTest --tests com.example.CheckoutInvariantsTest
./gradlew --no-daemon --stacktrace testDebugUnitTest --tests com.example.CommerceValidationTest
./gradlew --no-daemon --stacktrace lintDebug
./gradlew --no-daemon --stacktrace assembleDebug
```
