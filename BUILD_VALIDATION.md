# VORTIX 0.1.1 — Build validation record

## Static checks completed in this workspace

- [x] Android Studio project structure and Kotlin source paths reviewed.
- [x] Version catalog aliases aligned with root and app plugin/dependency references.
- [x] AGP 9.3.1 / Gradle 9.6.0 / Kotlin 2.4.21 compatibility checked against official compatibility tables.
- [x] KSP 2.3.12 and Hilt 2.60.1 checked against Dagger's KSP support requirements.
- [x] Gradle 9.6.0 distribution SHA-256 added from the official Gradle checksum reference.
- [x] AGP 9 explicit Kotlin compiler configuration retained with `android.builtInKotlin=false`; obsolete `android.newDsl=false` removed.
- [x] API 33+ sticky battery intent read uses `Context.RECEIVER_NOT_EXPORTED`.
- [x] Hilt Application, Activity, ViewModel, repository bindings and Room providers are connected in source.
- [x] Room schema export path is configured.
- [x] Missing battery fields remain nullable and are surfaced as unavailable.
- [x] Unexpected Room Flow failures are surfaced in Dashboard state.
- [x] Dashboard ViewModel tests added for initial snapshot loading and refresh failure handling.
- [x] GitHub Actions workflow configured to execute the checked-in Gradle bootstrap script, unit tests and Debug APK artifact creation (`checkout@v7`, `setup-java@v6`, `upload-artifact@v7`).
- [x] Original ZIP archive integrity and supplied SHA-256 sidecar verified.
- [x] Updated project TOML, AndroidManifest XML, GitHub Actions YAML and Gradle bootstrap shell syntax parsed successfully.
- [x] Version-catalog plugin and dependency aliases checked against Gradle build-script references.
- [x] WorkManager 2.12.0, AndroidX Hilt 1.4.0 and Preferences DataStore 1.2.1 catalog entries added from current stable release notes.
- [x] User-controlled WorkManager schedule, DataStore preferences, UsageStats access flow and charge-target notification code added.
- [x] Energy Controls ViewModel tests authored for enable/disable, target clamping and usage data.
- [x] UI separated into Batarya and Kontrol bottom-navigation tabs.

## Not verified here

- [ ] Gradle dependency resolution / Android Studio Gradle sync.
- [ ] Kotlin compilation and KSP code generation.
- [ ] Hilt generated component compilation.
- [ ] Room schema generation.
- [ ] `:app:testDebugUnitTest` execution, including Dashboard and Energy Controls ViewModel tests.
- [ ] `:app:assembleDebug` / APK output.
- [ ] Hilt Worker factory generation / WorkManager startup.
- [ ] Real-device Usage Access, notification permission and background scheduling tests.
- [ ] Installation and runtime behavior on API 26+ physical Android devices.
- [ ] System Settings battery percentage comparison on a physical device.

## Environment blocker

The workspace has Java 21 but no installed Gradle distribution, Android SDK, Android Studio or `adb`. A local `./gradlew --version` attempt failed because this environment cannot resolve `services.gradle.org` (`Could not resolve host`). Therefore no APK was generated in this workspace and no build success is claimed.

## Reproducible verification path

The `.github/workflows/android-ci.yml` workflow provisions JDK 21, Android SDK 36 and Gradle 9.6.0 on GitHub-hosted runners, runs unit tests and assembles the Debug APK, then publishes the APK as a workflow artifact. A successful workflow confirms CI compilation, tests and APK creation; a physical-device check is still required for battery reporting correctness.


## 0.1.1 Control Preview doğrulama notları

Yeni doğrulama kapsamı:

- `EnergyControlsViewModelTest`: arka plan ölçümünü başlatma/durdurma, hedef değerini sınırlandırma ve Usage Access verisi akışı.
- `BatteryMonitoringWorker`: WorkManager periyodik işi içinden pil anlık görüntüsünü yenileme ve şarj hedefi aşıldığında izin varsa bildirim üretme.
- WorkManager periyodik aralık alt sınırı 15 dakikadır; kesin tetiklenme zamanı garanti edilmez.
- Bildirim testi gerçek cihazda API 33+ bildirim izni verilerek yapılmalıdır.
- Usage Stats testi gerçek cihazda Android Kullanım erişimi ayarı açılarak yapılmalıdır.
- Şarj uyarısı donanım şarjını kesmez; üreticiye özel şarj koruma özellikleri Android sistem ayarlarında ayrıca bulunabilir.

Bu revizyon için Gradle/Android SDK derlemesi, KSP/Hilt kod üretimi ve test yürütmesi henüz bu çalışma ortamında doğrulanamadı.


**0.1.1 Control Preview note:** source code and tests were added, but Gradle sync, Hilt/KSP generation and test execution remain unverified in this environment. The charge target is notification-only; no hardware charging cutoff is implemented.
