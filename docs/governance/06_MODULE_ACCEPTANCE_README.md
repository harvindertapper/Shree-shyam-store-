# Module Acceptance README

Status: Governance source of truth for closing module work.

## Acceptance Record Location

When a module is completed, create an acceptance note under:

```text
docs/governance/module-acceptance/
```

Use a filename like:

```text
M06-BILLING-INVOICE-001.md
```

## Acceptance Record Template

```text
# <Task ID> Acceptance

## Goal
- Original goal.

## Scope Paths
- Files allowed and files actually changed.

## Acceptance Criteria
- Criteria from the task packet.

## Evidence
- Commands run and results.
- Screenshots/manual proof links if relevant.
- Data/migration proof if relevant.

## Security/Privacy Review
- Checklist items reviewed.
- Residual risk.

## Source-of-Truth Updates
- Docs updated.
- New decisions added to decision log.

## Result
- Accepted, blocked, or accepted with follow-up.

## Follow-Ups
- Module-aligned next tasks.
```

## Acceptance Rules

- Do not mark a module accepted without evidence.
- UI modules need visual/manual proof when practical.
- Data modules need preservation or migration proof.
- Security-sensitive modules need checklist review.
- Follow-ups must be module-aligned and must not hide incomplete acceptance criteria.
