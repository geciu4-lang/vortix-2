# VORTIX 1.0.0-rc1 — Test Report

**Android test execution: BLOCKED / NOT RUN.** The presence of test source files is not a passing result. No test has been labelled PASS unless an actual test runner produced a result.

## Unit test source inventory (9 test classes)

| Test class | Focus | Source present | Executed in this environment |
|---|---|---:|---:|
| `BatterySampleMapperTest` | Domain snapshot ↔ Room entity mapping | Yes | No |
| `BatteryRepositoryImplTest` | Repository insert/observe/prune behavior | Yes | No |
| `RefreshBatterySnapshotUseCaseTest` | Refresh use case | Yes | No |
| `DashboardViewModelTest` | Dashboard initial load and failure handling | Yes | No |
| `EnergyControlsViewModelTest` | Monitor toggle, target bounds and UsageStats presentation | Yes | No |
| `BatteryAnalyticsEngineTest` | Capacity, drain, sessions, thermal exposure, 7-day personal screen-off baseline anomaly and insufficient-baseline suppression, sampling coverage and gap calculations | Yes | No |
| `BatterySnapshotTest` | Signed current × voltage power derivation and missing-field behavior | Yes | No |
| `BatteryHistoryCsvTest` | CSV formatting and escaping | Yes | No |
| `PowerAdvisorEngineTest` | Critical thermal, power saver, absolute and personal-baseline screen-off drain alerts and capacity confidence rules | Yes | No |

## Required commands

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Expected JUnit XML: `app/build/test-results/testDebugUnitTest/*.xml`  
Expected HTML: `app/build/reports/tests/testDebugUnitTest/index.html`

## Checks performed outside Android Gradle

- Pure Kotlin domain analytics and advisor sources compiled with the standalone `kotlinc` compiler into a local JAR. This is only a narrow Kotlin syntax/type check of the included pure Kotlin files; it does not compile Android/Compose/Room/Hilt sources. A separate executable smoke harness exercised the synthetic personal-baseline anomaly case, Power Advisor trigger, signed power-flow derivation and null handling; this is not a JUnit/Gradle test run.
- Static validation was performed for TOML version catalog parsing, AndroidManifest XML, workflow YAML, Gradle catalog aliases, duplicate Kotlin imports and shell script syntax.
- ZIP archive integrity and SHA-256 were checked for the delivered source archive.

These checks are not a substitute for Gradle test execution.

## Execution blocker

- Runtime: OpenJDK 21.0.12.1.
- Android SDK / Android Studio / `adb`: absent.
- Preinstalled Gradle / cached Gradle 9.6.1 distribution: absent.
- The `./gradlew --version` attempt for the pinned Gradle 9.6.1 distribution failed after retries because DNS could not resolve `services.gradle.org` (`curl: (6) Could not resolve host`). The distribution was not available locally and the environment has no external DNS.

Therefore the following are not executed: Android Studio sync, dependency resolution, full Kotlin/Compose compilation, KSP generation, Hilt components/worker factory generation, Room database implementation/schema generation, JUnit tests, Debug APK assembly, installation and runtime checks.

## Device test plan still required

- API 26 / API 29 / API 31 / API 33 / API 34 / API 36 device or emulator matrix.
- OEM comparison (at least Pixel/AOSP-like, Samsung One UI and Xiaomi/HyperOS class of background policy).
- BatteryManager values against Android system battery status; unsupported metrics remain absent.
- NetworkStats Usage Access off/on, mobile/Wi-Fi history availability, SIM/no-SIM and dual-SIM behavior.
- UsageStats opt-in and launcher app visibility.
- Notification permission denied/granted, target alert latch/reset, thermal alert latch/hysteresis.
- WorkManager start/stop, app process death/reboot, Doze and OEM background delay.
- Room v1→v2 migration with retained history and Room-generated schema validation.
- Widget and Quick Settings Tile add/update/launch behavior.
- Battery history CSV export and user-controlled sharing.

## Auditor status

**NOT RELEASE-APPROVED.** Test sources and static checks exist, but a release requires actual Android Gradle test/assemble results and device validation from a matching source revision.
