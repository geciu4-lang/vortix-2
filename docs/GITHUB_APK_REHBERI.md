# Vortix GitHub Üzerinden APK Alma Rehberi

Bu proje GitHub Actions (`android-ci.yml`) ile otomatik olarak derlenecek şekilde yapılandırılmıştır.

## GitHub'da APK Nasıl Üretilir ve İndirilir?

1. Tüm proje dosyalarını GitHub deponuza yükleyin (`main` veya `master` dalına push yapın).
2. GitHub deponuzun üst menüsündeki **Actions** sekmesine tıklayın.
3. Çalışan **Android CI — Vortix** iş akışına girin.
4. İşlem başarıyla tamamlandığında (yeşil tik), sayfanın en altındaki **Artifacts** bölümünden **`VORTIX-Ultimate-1.0.0-rc1-Debug`** paketini indirin.
5. İndirdiğiniz ZIP dosyasını açınca içinden **`app-debug.apk`** dosyasını alabilir ve Android cihazınıza kurabilirsiniz.
