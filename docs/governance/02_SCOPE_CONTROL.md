# Scope Control

Status: Governance source of truth.

## Scope Rule

Every task must stay inside its named module and allowed paths. If the implementation reveals an adjacent issue, document it as a follow-up unless it blocks the assigned task.

## Allowed Change Classes

- Docs-only governance updates.
- Localized UI copy updates for affected screens.
- Focused business logic fixes.
- Focused tests for assigned module.
- Data migration work only when explicitly assigned.
- Firebase Cloud Sync Foundation planning and implementation only through approved Firebase task packets.

## Owner Approval Required

Get explicit approval before:

- Adding dependencies.
- Changing package/application id.
- Changing release signing or secrets handling.
- Adding backend/API calls outside the approved Firebase foundation sequence.
- Adding dangerous Android permissions.
- Adding Bluetooth printer, multi-store implementation, staff-role implementation, or real UPI confirmation.
- Changing Room schema.
- Changing backup/export/import behavior.
- Making loan/credit scoring, GST/tax filing, legal compliance, or payment-success claims.

## Forbidden Without Approval

- Broad refactors.
- Deleting existing user data behavior.
- Using destructive migrations for production schema changes.
- Storing tokens or secrets in source, Room, DataStore, or resources.
- Hardcoding real customer or owner personal data.
- Mixing English and Hindi in final UI copy.
- Implementing Firebase before architecture, config prerequisites, Auth model, Firestore model, security rules, migration strategy, offline/conflict policy, and QA plan are accepted.

## Conflict Handling

If docs disagree:

1. Stop the implementation.
2. Name the conflicting files and sections.
3. Recommend the smallest owner decision needed.
4. Do not edit code until the conflict is resolved or scoped around.
