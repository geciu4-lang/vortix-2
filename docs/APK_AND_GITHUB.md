# APK and GitHub CI — VORTIX 1.0.0-rc1

The repository contains a GitHub Actions workflow that runs Android unit tests and builds a Debug APK on the same source revision.

## Run locally

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Expected APK: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub workflow

Push the candidate source to the intended `main` branch (or run `workflow_dispatch`) and inspect the resulting Actions run. The workflow must finish both unit tests and assemble tasks. Download the `VORTIX-Ultimate-1.0.0-rc1-Debug` artifact only from a successful run for the exact commit under review.

The existing GitHub `main` tree was previously found to be an older 0.1 source revision; any earlier artifact/run is unrelated and must not be represented as the APK for this candidate.

## Signing and distribution

The Debug APK is signed with the standard debug key and is for testing. A public Release requires a separately configured release signing key/keystore, securely injected signing credentials (e.g. GitHub Secrets), versioning and a Release AAB build. Never commit the keystore or passwords to source control.

## Current state

No APK is included in this source ZIP. The build environment used for this review has no Android SDK/Android Studio. The Gradle 9.6.1 distribution download was blocked by DNS resolution; the wrapper is pinned to 9.6.1 with its official checksum, but the distribution could not be fetched in this environment. Consequently no local APK has been generated or signature-checked. See `BUILD_VALIDATION.md` and `docs/TEST_REPORT.md`.
