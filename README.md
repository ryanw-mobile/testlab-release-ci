# Android CI/CD Pipeline Template Repository <br/>[![Renovate enabled](https://img.shields.io/badge/renovate-enabled-brightgreen.svg)](https://renovatebot.com/)

This repository provides a template setup for Android apps with a fully working GitHub Actions workflow for CI/CD. It builds both APK and AAB outputs, and can optionally sign and release them via GitHub Releases.

## How to Build

### Without Keystore (Debug Builds)

By default, debug builds do not require a keystore. You can run:

```bash
./gradlew assembleDebug
```

or build the debug AAB:

```bash
./gradlew bundleDebug
```

No signing config is required unless you explicitly build a release variant.

### With Keystore (Release Builds)

Signing is intentionally **disabled by default**, even for release builds.

A release build (including R8, shrinking, and obfuscation) can be run **without signing**:

```bash
./gradlew assembleRelease
```

Signing is only enabled when you explicitly opt in using a Gradle property:

```bash
./gradlew assembleRelease -PreleaseSigning=true
```

There are two ways to supply the keystore:

#### 1. Environment Variables (For CI)

Note: Providing keystore environment variables alone does **not** enable signing.
Signing must be explicitly enabled via `-PreleaseSigning=true`.

Provide the following environment variables (e.g. in GitHub Secrets):

```
KEYSTORE_LOCATION=./keystore.jks
CI_ANDROID_KEYSTORE_ALIAS=yourAlias
CI_ANDROID_KEYSTORE_PASSWORD=yourKeystorePassword
CI_ANDROID_KEYSTORE_PRIVATE_KEY_PASSWORD=yourPrivateKeyPassword
```

#### 2. `keystore.properties` File (For Local Builds)

Create a `keystore.properties` file at the root:

```properties
alias=yourAlias
pass=yourPrivateKeyPassword
store=path/to/keystore.jks
storePass=yourKeystorePassword
```

Then build:

```bash
./gradlew bundleRelease -PreleaseSigning=true
```

### Output Format

Release builds are timestamped using the format:

```
<app-name>-<buildType>-<versionName>-<yyyyMMdd-HHmmss>.apk
```

This applies to both APK and AAB artifacts.

Because outputs include a timestamp, CI workflows must locate APK/AAB files dynamically (for example, using `find`), rather than assuming fixed filenames.

## Workflow Overview

This repository uses multiple GitHub Actions workflows with distinct responsibilities:

- `pr_check.yml`: runs lint and unit tests for human-authored pull requests.
- `renovate_pr.yml`: runs lint, unit tests, and emulator tests for Renovate dependency updates.
- `main_build.yml`: performs a release build preflight (R8 enabled, no signing) after merges to `main`.
- `tag_create_release.yml`: performs signed release builds and publishes APK/AAB artifacts to GitHub Releases.

Trigger the workflow by tagging a commit with the format: `release/*`, e.g. `release/1.2.3`.

### Usage

To trigger the CI pipeline:

```bash
git tag release/1.0.0
git push origin release/1.0.0
```

GitHub Actions will build the release and publish it with assets.

### Configuration Summary

- JDK 21 and Kotlin configured via toolchain
- Code style enforced via Detekt, Kover, and Kotlinter
- Compose support with BOM and previews
- Managed device test runner for CI

### Personal Access Token (PAT)

The workflow uses a GitHub token with `repo` scope to publish releases. Store it as a repository secret:

```
PAT_FOR_RELEASES
```

## License

Apache 2.0

## Acknowledgments

- GitHub Actions for providing the automation platform.
- All contributors who help maintain and improve this project.

## Support

For support or questions about using this workflow, please [open an issue](https://github.com/ryanw-mobile/testlab-release-ci/issues) in this repository.
