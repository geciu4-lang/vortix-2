# VORTIX — Energy Core 0.1.1 Control Preview

Kotlin, Jetpack Compose, Material 3, MVVM, Clean Architecture, Hilt ve Room ile geliştirilen Android pil izleme uygulamasının ilk kilometre taşı.

## 0.1.1 Control Preview kapsamı

- `BatteryManager` ve `ACTION_BATTERY_CHANGED` üzerinden gerçek pil anlık görüntüsünü okur; Room'a kaydeder.
- Kullanıcı anahtarıyla WorkManager üzerinden yaklaşık 15 dakikalık periyodik ölçüm planlar veya iptal eder. Android zamanlamayı erteleyebilir; bu gerçek zamanlı izleme değildir.
- DataStore ile izleme tercihini ve şarj hedefini kalıcı saklar.
- Şarj hedefi (70–100%) aşılır ve cihaz şarj oluyorsa şarj oturumu başına bir bildirim üretir (hedef altına inilince veya şarj kesilince tekrar kurulur). Bu bir donanım şarj kesicisi değildir; Android'in genel üçüncü taraf uygulamalara sunduğu evrensel bir şarj sınırı API'si bulunmadığından yalnızca uyarı verir.
- Kullanıcı Usage Access iznini sistem ayarlarından açarsa bugünkü ön plan kullanım süresine göre uygulamaları listeler. Bu veriler gerçek mAh/pil tüketimi değildir.
- Uygulama satırındaki Ayarlar eylemi ilgili Android Uygulama Bilgisi ekranını açar; kullanıcı arka plan kısıtlarını kendisi seçer.
- Android Pil Tasarrufu ayarlarını ve Vortix'in kendi uygulama ayarlarını açan eylemler sunar.
- Desteklenmeyen ölçümleri tahmin etmez.

> Bu 0.1.1 Control Preview, 0.3 Drain Radar ve 0.4 Adaptive Charging modüllerinin yalnızca güvenli/erişilebilir ilk parçalarını öne alır. Tam mAh bazlı drain analizi, wakelock adaptörü, Shizuku destekli tanılama, üreticiye özgü şarj kontrolü ve gerçek hibernation motoru tamamlanmış değildir. 0.2 Smart Health kilometre taşı da henüz başlatılmamıştır.

## Teknoloji tabanı

| Bileşen | Sürüm |
|---|---:|
| Android Gradle Plugin | 9.3.1 |
| Gradle | 9.6.0 |
| Kotlin / Compose Compiler | 2.4.21 |
| KSP | 2.3.12 |
| Hilt | 2.60.1 |
| Room | 2.8.5 |
| Compose BOM | 2026.09.00 |
| Lifecycle | 2.11.0 |
| Coroutines | 1.11.0 |
| WorkManager | 2.12.0 |
| AndroidX Hilt Work | 1.4.0 |
| Preferences DataStore | 1.2.1 |
| Compile / Target SDK | 36 |
| Minimum SDK | 26 |
| JVM target | 17 |

Kotlin 2.4.21'in yayımlanmış tam uyumluluk aralığı AGP 8.5.2–9.3.1 ve Gradle 7.6.3–9.7.0'dır. AGP 9.3.1 için Gradle 9.5 veya üzeri gerekir; bu proje Gradle 9.6.0 kullanır. KSP 2.3.12 ve Hilt 2.60.1, Dagger'ın belgelenen kararlı KSP desteğiyle uyumludur.

AGP 9'un yerleşik Kotlin derleyicisi, sürümü açıkça sabitlenmiş Kotlin Gradle Plugin 2.4.21 kullanılabilmesi için `android.builtInKotlin=false` ile kapatılmıştır. Kotlin Android eklentisi bu nedenle `app` modülüne açıkça uygulanır. Yeni AGP DSL etkin durumdadır.

## Proje yapısı

```text
com.vortix.app/
├── VortixApplication.kt
├── MainActivity.kt
├── di/
│   ├── BatteryRepositoryModule.kt
│   └── DatabaseModule.kt
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── data/
│   ├── battery/
│   │   └── mapper/
│   ├── local/
│   │   ├── dao/
│   │   └── entity/
│   └── repository/
├── feature/
│   ├── dashboard/
│   └── controls/
├── data/
│   └── work/
└── ui/
    └── theme/
```

## Yerel kurulum

1. Bu depoyu veya ZIP arşivini bilgisayarına al.
2. Android Studio'da `Vortix` kök klasörünü aç.
3. JDK 21, Android SDK Platform 36 ve Build Tools 36.0.0 kur.
4. Gradle senkronizasyonunu tamamla.
5. API 26 veya üzeri bir Android cihaz/emülatör seçerek `app` konfigürasyonunu çalıştır.

`gradlew` ve `gradlew.bat`, standart Wrapper JAR'ı kullanmayan özel bootstrap betikleridir; Gradle 9.6.0 dağıtımını ilk kullanımda indirip yayıncının SHA-256 özetiyle doğrularlar. İndirme öncesinde yayıncının resmî SHA-256 özetiyle doğrulama yaparlar. CI da aynı bootstrap betiğini çalıştırarak yerel ve uzak derleme yollarını eşleştirir.

## Test ve APK oluşturma

macOS/Linux:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Windows:

```bat
gradlew.bat :app:testDebugUnitTest
gradlew.bat :app:assembleDebug
```

Başarılı Debug derlemesinin APK yolu:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions ile otomatik APK

`.github/workflows/android-ci.yml` (`checkout@v7`, `setup-java@v6`, `setup-android@v4`, `setup-gradle@v6`, `upload-artifact@v7`), `main`/`master` dalına push, Pull Request veya `workflow_dispatch` tetiklendiğinde:

1. JDK 21 ve Android SDK 36 kurar.
2. `:app:testDebugUnitTest` testlerini çalıştırır.
3. `:app:assembleDebug` ile APK üretir.
4. APK'yı `Vortix-Energy-Core-0.1.1-control-preview-debug` adlı Actions Artifact olarak yayımlar.

GitHub deposunda **Actions → Android CI — Vortix → son başarılı çalıştırma → Artifacts** yoluyla APK indirilebilir.

Dashboard ViewModel için başlangıç yüklemesi ve yenileme hatası testleri de `:app:testDebugUnitTest` kapsamındadır. Bu çıktı **Debug APK**'dır ve Android'in debug anahtarıyla imzalanır. Cihazda test etmek içindir; Google Play dağıtımı veya herkese açık Release APK değildir. Play Store aşamasında ayrı imzalama anahtarı güvenli GitHub Secrets altında saklanarak Release AAB/APK süreci kurulmalıdır.

## İzinler ve platform sınırları

- `PACKAGE_USAGE_STATS` bir normal runtime izni değildir. Kullanıcı, Vortix'i Android'in Kullanım erişimi ayarlarında ayrıca etkinleştirmelidir.
- Android 13+ üzerinde şarj hedefi bildirimi için `POST_NOTIFICATIONS` izni istenir.
- WorkManager periyodik iş için 15 dakika platform alt sınırını kullanır. Cihaz boşta, Doze altında veya üretici pil yönetimindeyken çalışma daha ileri bir saate ertelenebilir.
- Normal üçüncü taraf uygulaması diğer uygulamaları sessizce force-stop edemez veya sistemin gerçek derin uyku/hibernation durumunu zorlayamaz. Uygulama ayrıntıları ekranına yönlendirme, kullanıcıya Android'in sunduğu kontrolleri seçme imkânı verir. Kurumsal cihaz yöneticisi ayrıcalıkları ve Shizuku/ADB/root, standart Play Store uygulama yetkilerinden farklı ve açık kullanıcı kurulumu/onayı gerektiren yollardır.
- Genel Android SDK'sı pil şarj akımını kesen veya her üreticide çalışan donanım şarj yüzdesi limiti sağlamaz. Vortix hedef yüzdesinde yalnızca uyarı verir; donanım şarjını durdurduğunu iddia etmez.

## Veri doğruluğu

- `BATTERY_PROPERTY_CHARGE_COUNTER` mikroamper-saat cinsindedir; tasarım kapasitesi olarak kullanılmaz.
- `BATTERY_PROPERTY_CURRENT_NOW` mikroamper cinsindedir; işaret korunur.
- Cycle count, Android 14+ üzerinde ve sistem bildirimi alanı mevcutsa okunur; yoksa `null` kalır.
- Sıcaklık, onda bir derece Celsius değerinden dönüştürülür.
- Voltaj milivolt cinsinden gösterilir.
- `EXTRA_PLUGGED` yoksa kaynak `UNKNOWN` kabul edilir; cihaz “pil üzerinde” diye varsayılmaz.
- `BATTERY_STATS` gibi ayrıcalıklı izinler istenmez.

## Sonraki aşamalar

- **0.2 Smart Health:** Yerel cycle count geçmişi, kapasite tahmini, Health Score ve şarj geçmişi grafikleri.
- **0.3 Vortex Radar:** UsageStats, drain analizi, wakelock adaptörü ve isteğe bağlı Shizuku tanılaması.
- **0.4 Adaptive Charging:** Termal takip, %80 hedef bildirimi ve şarj tahmini.
- **0.5 Hibernation Vault:** Kullanım geçmişi, beyaz liste ve kullanıcı kontrollü uyku önerileri.

## Doğrulama durumu

Kaynak dosyaları ve statik yapılandırma kontrolleri gözden geçirilmiştir. Bu çalışma ortamında Android SDK ve Gradle dağıtımı bulunmadığı, `services.gradle.org` DNS çözümlemesi başarısız olduğu için burada gerçek Gradle senkronizasyonu, Kotlin/KSP derlemesi, test çalıştırması veya APK üretimi doğrulanamamıştır. GitHub Actions iş akışı bu doğrulamayı ağ erişimi olan GitHub runner üzerinde gerçekleştirmek için eklenmiştir. 0.1, Actions çalışması başarılı olmadan ve gerçek cihaz kontrolü yapılmadan tamamlanmış kabul edilmez.
