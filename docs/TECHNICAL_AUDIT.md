# VORTIX 1.0.0-rc1 — Technical Audit

**Outcome: expanded source candidate reviewed; Android build not certified.**

## Architecture retained

- Kotlin, Jetpack Compose and Material 3.
- MVVM presentation and Clean Architecture domain/data boundaries.
- Hilt for dependency injection and assisted Worker creation.
- Room database (v2 with a v1→v2 additive migration) for battery samples.
- Preferences DataStore for monitoring settings, design capacity, battery baseline, change markers and notification latches.
- WorkManager for deferrable periodic sampling, not real-time capture.

## Integrated module inventory

- **Energy Core:** BatteryManager/PowerManager fields, battery-side derived power-flow readout, dashboard, Room history, chart, widget, Quick Settings Tile.
- **Smart Health:** conservative capacity estimation, confidence, optional design-capacity comparison, battery replacement baseline, charge/discharge sessions and CSV export.
- **VORTIX Radar:** screen-on/off drain, dated discharge endurance diary, observed usage window, 24-hour sampled battery-temperature timeline, thermal exposure, foreground usage, mobile/Wi-Fi 24h summaries and separate Android bucket-history charts, prior-7-day personal-baseline screen-off anomaly detection, before/after change experiments.
- **Adaptive Charging Assistant:** user target alerts, thermal warnings, power advisor and native-settings handoffs. It is not a hardware charge controller.
- **Hibernation Vault:** dormant launcher app review and user-directed App Info handoff; no silent force-stop/hibernation.
- **Power Advisor:** pure Kotlin evidence-threshold recommendation engine with unit test sources.

## Build-tool compatibility decision

This project pins AGP 9.3.1 and Kotlin Gradle Plugin 2.4.21. Kotlin's published compatibility table includes AGP 9.3.1 and Gradle through 9.7.0; AGP 9.3 requires at least Gradle 9.5. The wrapper therefore uses the patched Gradle 9.6.1 line. As this project still applies the external Kotlin Android plugin, `gradle.properties` explicitly opts out of AGP 9 built-in Kotlin and the new DSL (`android.builtInKotlin=false`, `android.newDsl=false`). This is a controlled compatibility bridge, not a completed AGP built-in Kotlin migration; the build must confirm these plugin and DSL choices.

## Privacy posture

- The manifest declares no `INTERNET` permission; app backup is disabled. Battery records and configuration remain on-device. `PACKAGE_USAGE_STATS` is a special optional user-granted access for local UsageStats/NetworkStats views.

## Important implementation decisions

1. Battery capacity is estimated only from charge-counter and state-of-charge changes with plausibility, interval and outlier checks; health ratio is withheld until a design capacity reference and at least medium-confidence evidence are available, and is capped at 100%.
2. Screen-on/off drain is calculated only from paired samples within configured interval bounds, while unplugged duration is explicitly described as an observed window. A personal anomaly requires at least five baseline and three recent valid intervals, plus both absolute (+1 percentage point/hour) and relative (1.75×) increase gates.
3. Thermal exposure uses only observed intervals with a bounded gap. Android thermal status is separately represented from battery temperature.
4. Network summaries come from `querySummaryForDevice`; network history comes from `queryDetails` and is aggregated per Android-provided start/end bucket. Android documents those buckets are hour-scale, non-interpolated and profile-scoped for details. No minute-level chart claim is made.
5. UsageStats foreground duration and network byte counts are context, not energy measurement; neither is converted into per-app mAh.
6. New battery baseline and user change markers retain the older history; only current-battery learning is filtered after the baseline.
7. WorkManager interval is 15 minutes minimum and deferrable. UI and docs do not claim real-time collection.
8. Vault uses package visibility for launcher apps and takes users to native system screens instead of attempting privileged controls.
9. Battery-side derived power is current × terminal voltage using Android's signed-current convention; it is not reported as USB charger input wattage.

## Source corrections / enhancements in this candidate

- Duplicate Compose `getValue` import removed; `gradlew` executable bit restored; home-screen widget receives a 30-minute periodic update request instead of being configured never to refresh.
- Dashboard and Energy Controls ViewModel test sources retained.
- Pure Kotlin analytics and evidence-first recommendation engine added with unit test source.
- NetworkStats history buckets and separate mobile/Wi-Fi timelines added.
- Test fixture for sampling coverage corrected to use one qualifying interval and one known gap.
- CI artifact label aligned to `VORTIX-Ultimate-1.0.0-rc1-Debug` and application version set to `1.0.0-rc1` / versionCode 3.

## Known product limitations

- No generic hardware charge cutoff/charging current control.
- No standard permission for silent force-stop, app hibernation or package suspension.
- No universally reliable per-app mAh measurement via UsageStats/NetworkStats.
- No root/ADB/Shizuku privileged telemetry bridge in this candidate; those need separate explicit setup/security and compatibility design.
- Android background policy can defer sampling; gaps are disclosed.
- Network history buckets may be missing, manufacturer-limited or unavailable without Usage Access.

## Validation blockers

The environment lacks Android SDK, Android Studio, adb and preinstalled Gradle. The pinned Gradle 9.6.1 distribution could not be fetched because this environment cannot resolve `services.gradle.org`; its official checksum is recorded in the wrapper properties, but the distribution could not be executed. Therefore full Android compilation, KSP/Hilt/Room generation, Gradle unit tests, APK creation and device testing remain unverified.

## Auditor decision

**Not approved as a production final release yet.** Source-level product integration and static checks are packaged as candidate work. Approval requires successful `:app:testDebugUnitTest`, `:app:assembleDebug`, schema/migration checks, CI on this exact revision and device/OEM validation.
