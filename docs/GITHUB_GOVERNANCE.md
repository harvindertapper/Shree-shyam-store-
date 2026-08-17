# GitHub Governance Baseline

**Repository:** `harvindertapper/Shree-shyam-store-`  
**Default branch:** `main`  
**Configured:** 17 August 2026

## Protected `main`

The `main` branch is protected with administrator enforcement. Direct force-pushes and branch deletion are disabled. Pull requests must pass the required status checks **Verify Android app** and **Review dependency changes**, and GitHub requires conversation resolution before merge. Stale reviews are dismissed when new commits are pushed. The required approval count is currently zero because this is a single-owner repository; once a second trusted maintainer is added, raise the approval requirement to at least one and enable code-owner review.

The protection rule uses strict status checks, so the pull request branch must be up to date with `main` before merge. The repository permits squash merges only, deletes merged branches automatically, and disables merge commits and rebase merges. This keeps the history reviewable and prevents accidental direct integration of unverified commits.

## Required checks

| Check | Source | Purpose |
| --- | --- | --- |
| Verify Android app | `.github/workflows/ci.yml` | Wrapper validation, whitespace check, debug APK assembly, lint, stable unit/Robolectric tests, and debug artifact upload |
| Review dependency changes | `.github/workflows/dependency-review.yml` | Blocks dependency changes when GitHub's dependency review identifies unsupported or vulnerable additions |
| Dependabot configuration | GitHub-managed check | Validates `.github/dependabot.yml` syntax and configuration |

## Security settings

The repository is public. GitHub Secret Scanning, Secret Scanning Push Protection, the Dependency Graph, Dependabot Alerts, and Dependabot Security Updates are enabled. Dependabot version-update policy is committed in `.github/dependabot.yml` for Gradle and GitHub Actions. GitHub's non-provider secret patterns and validity checks remain disabled because they are separate feature controls and are not required for the current baseline; enable them if the account plan supports them and the owner wants broader scanning.

The repository still needs a deliberate production security review for Firebase rules, authentication identity model, release signing, privacy disclosures, and data retention. GitHub settings reduce repository and supply-chain risk but do not replace application-level authorization, secure cloud rules, or recovery testing.

## Change procedure

Review the current rules with `gh api repos/harvindertapper/Shree-shyam-store-/branches/main/protection` and repository security settings with `gh api repos/harvindertapper/Shree-shyam-store-`. Modify rules only when the corresponding CI check name is stable and a rollback path is understood. Do not remove required checks to merge a failing feature; fix the workflow or use an explicitly documented emergency process.
