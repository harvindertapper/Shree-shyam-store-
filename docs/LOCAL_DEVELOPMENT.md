# Local Development and GitHub Workflow

## Source of truth and checkout location

GitHub is the source of truth for this project. Keep the primary laptop checkout in the Desktop directory, for example:

```text
<Desktop>/Shree-shyam-store-/
```

The path is intentionally shown with `<Desktop>` because Windows, macOS, and Linux use different absolute locations. The repository should be cloned once, then synchronized through Git rather than by copying files between folders.

```bash
git clone https://github.com/harvindertapper/Shree-shyam-store-.git <Desktop>/Shree-shyam-store-
cd <Desktop>/Shree-shyam-store-
git switch main
```

For normal development, do not commit directly to `main`. Start from an up-to-date branch:

```bash
git fetch --all --prune
git switch main
git pull --ff-only origin main
git switch -c feat/<issue-id>-<short-name>
```

Use `fix/<issue-id>-<short-name>` for a defect, `feat/<issue-id>-<short-name>` for a feature, and `chore/<short-name>` for tooling or documentation. Commit coherent slices with messages that explain the user or engineering outcome. Push the branch and open a pull request before merging.

## Toolchain

The project uses Java 21 in CI and Gradle 9.7.0. The repository includes the Gradle wrapper, so local commands should use `./gradlew` on macOS/Linux or `gradlew.bat` on Windows. Android Studio should be configured with an Android SDK that supports compile/target SDK 36. The app module compiles Kotlin/Java sources for JVM 11 while the build itself runs on Java 21.

The local environment must provide the values needed by the selected build tasks. Copy `.env.example` to `.env` only for local development, and never commit `.env`, keystores, Firebase service credentials, or personal data. The example file contains placeholders and a public-looking Firebase URL; production access rules and credentials are not part of this repository and must be configured through the deployment environment.

## Common commands

Run these commands from the repository root:

```bash
./gradlew assembleDebug
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew testDebugUnitTest --tests com.aistudio.shreeshyamstore.pqwzkb.SecurityUtilsTest --tests com.aistudio.shreeshyamstore.pqwzkb.RestoreSecurityTest
./gradlew clean assembleDebug
```

The screenshot test is intentionally not part of the stable headless CI gate because its native graphics mode can hang on a headless runner. It may be run from a graphics-capable development environment when UI changes require screenshot review.

## Agent and laptop synchronization

When Manus creates a branch, fetch it in the laptop checkout instead of copying changed files manually:

```bash
git fetch origin
 git switch --track origin/fix/security-sync-boundary
```

The extra leading space before `git switch` in the example above should be removed when running the command; the canonical form is:

```bash
git switch --track origin/fix/security-sync-boundary
```

When local edits exist, commit or stash them before switching branches or pulling. If the branch has diverged, stop and inspect the diff; do not use a force push or destructive reset unless the repository owner explicitly approves it.

## Pull-request definition of done

A pull request is ready when the implementation, tests, documentation, and migration implications are clear. The applicable local checks must pass, the diff must be limited to the intended change, and no secret or customer data may be present. Changes that affect auth, cloud sync, database schema, money, inventory, payment, or restore behavior require explicit regression tests and a rollback or migration note.

A merge to `main` should happen only after CI is green and the review confirms that local-only identity data is not being exported, destructive data operations are guarded, and offline behavior remains usable. Release work must be performed from a tagged, reviewed commit rather than from an arbitrary laptop working tree.
