# Signed release candidate workflow

The repository now contains a manual GitHub Actions workflow at `.github/workflows/release-candidate.yml`.

It is intentionally separate from pull-request CI. The workflow:

1. Runs only when a release owner manually dispatches it.
2. Uses the protected GitHub `release` environment.
3. Materializes the keystore outside the checked-out workspace.
4. Requires release signing inputs before Gradle runs.
5. Forces `REQUIRE_RELEASE_SIGNING=true` and enables the existing R8/minified release configuration.
6. Builds an AAB with an explicit version code and version name.
7. Uploads the AAB, R8 mapping file, provenance record, and SHA-256 checksums as one evidence bundle.

## Required GitHub environment secrets

Create these secrets in the repository's protected `release` environment:

- `RELEASE_KEYSTORE_BASE64`: base64-encoded production upload/release keystore.
- `RELEASE_STORE_PASSWORD`: keystore password.
- `RELEASE_KEY_ALIAS`: signing key alias.
- `RELEASE_KEY_PASSWORD`: signing key password.

The workflow never commits or prints these values. The keystore is written under the runner's temporary directory and is not included in the workspace artifact.

## Release procedure

1. Confirm the final package ID, Firebase Android client, version policy, and Play Console application are approved.
2. Create or update the protected `release` environment and require the designated release owner/reviewer.
3. Add the four secrets above through GitHub's environment secret UI.
4. Dispatch **Signed release candidate** from the exact immutable branch or tag being rehearsed.
5. Enter a new positive `versionCode` and reviewed `versionName`.
6. Download and retain the evidence bundle with the release ticket: AAB, mapping file, provenance, checksum, commit SHA, and tag/ref.
7. Verify the AAB on clean and upgraded pilot devices before any Play upload.

A successful workflow is artifact evidence, not permission to publish. Play rollout, Firebase rule verification, monitoring, support ownership, and rollback rehearsal remain required launch gates.
