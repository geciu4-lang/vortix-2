package com.vortix.app.domain.analytics

import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.model.BatteryStatus
import com.vortix.app.domain.model.DormantAppItem
import com.vortix.app.domain.model.EvidenceConfidence
import com.vortix.app.domain.model.NetworkUsageSnapshot
import com.vortix.app.domain.model.PowerInsightSummary
import com.vortix.app.domain.model.PowerRecommendation
import com.vortix.app.domain.model.RecommendationPriority

/**
 * Evidence-first, rule-based guidance. It never attributes device drain to an app from
 * foreground time or network bytes, and never claims to control charging hardware.
 */
object PowerAdvisorEngine {
    fun evaluate(
        snapshot: BatterySnapshot?,
        summary: PowerInsightSummary?,
        dormantApps: List<DormantAppItem> = emptyList(),
        networkUsage: NetworkUsageSnapshot? = null
    ): List<PowerRecommendation> {
        val recommendations = mutableListOf<PowerRecommendation>()
        if (snapshot == null) {
            return listOf(
                PowerRecommendation(
                    id = "collecting_baseline",
                    priority = RecommendationPriority.INFO,
                    title = "Kişisel pil temel çizgisi oluşturuluyor",
                    explanation = "VORTIX güvenilir öneriler verebilmek için cihazdan ölçüm toplamalı. Arka plan ölçümünü aç ve birkaç gün veri biriktir.",
                    action = "Ölçüm ayarlarını kontrol et"
                )
            )
        }

        if (summary == null || summary.sampleCount < 2) {
            recommendations += PowerRecommendation(
                id = "collecting_history",
                priority = RecommendationPriority.INFO,
                title = "Kişisel kullanım örüntüsü oluşturuluyor",
                explanation = "VORTIX henüz güvenilir tüketim karşılaştırması için yeterli geçmişe sahip değil. Ölçüm açık kaldıkça ekran açık/kapalı tüketimi ve kapasite tahmini daha anlamlı hâle gelir."
            )
        }

        val batteryTemp = snapshot.temperatureCelsius
        val severeDeviceThermal = (snapshot.thermalStatus ?: 0) >= 3
        when {
            batteryTemp != null && batteryTemp >= 45.0 -> recommendations += PowerRecommendation(
                id = "critical_battery_temperature",
                priority = RecommendationPriority.CRITICAL,
                title = "Pil sıcaklığı kritik seviyede",
                explanation = "Cihazın bildirdiği pil sıcaklığı ${"%.1f".format(java.util.Locale.US, batteryTemp)}°C. Ağır kullanımı bırak, şarjı güvenli biçimde sonlandır ve cihazı doğrudan güneş/ısı kaynaklarından uzaklaştır.",
                action = "Sıcaklığı yeniden kontrol et"
            )
            batteryTemp != null && batteryTemp >= 42.0 -> recommendations += PowerRecommendation(
                id = "high_battery_temperature",
                priority = RecommendationPriority.HIGH,
                title = "Pil normalden fazla ısınıyor",
                explanation = "Ölçülen pil sıcaklığı ${"%.1f".format(java.util.Locale.US, batteryTemp)}°C. Özellikle şarj sırasında oyun, kamera ve ağır işlemlere ara verip sıcaklığın düşmesini bekle.",
                action = "Termal geçmişi incele"
            )
            severeDeviceThermal -> recommendations += PowerRecommendation(
                id = "severe_device_thermal",
                priority = RecommendationPriority.HIGH,
                title = "Android ciddi termal baskı bildiriyor",
                explanation = "Android cihazı ciddi termal kısıtlama durumunda bildiriyor. Bu sistem durumu pil sıcaklığıyla aynı ölçüm değildir; ağır iş yükünü azaltıp cihazın soğumasını sağla."
            )
            batteryTemp != null && batteryTemp >= 40.0 && snapshot.status == BatteryStatus.CHARGING -> recommendations += PowerRecommendation(
                id = "warm_charging",
                priority = RecommendationPriority.MEDIUM,
                title = "Şarj sırasında ısınma gözlendi",
                explanation = "Şarj devam ederken pil sıcaklığı 40°C veya üzerine çıktı. Şarj yüzeyini kapatma, yoğun kullanım ile şarjı aynı anda yapmaktan kaçın ve geçmiş oturumları takip et."
            )
        }

        val drain = summary?.drain
        val screenOffRate = drain?.screenOffPercentPerHour
        val anomaly = summary?.screenOffDrainAnomaly
        when {
            anomaly != null -> recommendations += PowerRecommendation(
                id = "personal_screen_off_drain_anomaly",
                priority = if (anomaly.increasePercent >= 150.0) RecommendationPriority.HIGH else RecommendationPriority.MEDIUM,
                title = "Ekran kapalı tüketim kişisel seviyenin üzerine çıktı",
                explanation = "Son 24 saatte gözlenen medyan ekran-kapalı tüketim ${"%.1f".format(java.util.Locale.US, anomaly.recentPercentPerHour)}%/saat; önceki 7 günlük kişisel taban çizgin ${"%.1f".format(java.util.Locale.US, anomaly.baselinePercentPerHour)}%/saat. Artış yaklaşık %${anomaly.increasePercent.toInt()}. Bu tüm cihazın toplam değişimidir; tek başına bir uygulamayı suçlamaz.",
                action = "Radar analizini incele"
            )
            screenOffRate != null && drain.screenOffIntervals >= 3 && screenOffRate >= 5.0 -> recommendations += PowerRecommendation(
                id = "elevated_screen_off_drain",
                priority = RecommendationPriority.HIGH,
                title = "Ekran kapalı tüketim çok yüksek görünüyor",
                explanation = "VORTIX yeterli örnekte ekran kapalı tüketimini ${"%.1f".format(java.util.Locale.US, screenOffRate)}%/saat ölçtü. Kişisel geçmiş henüz yeterli olmasa da bu mutlak hız dikkat gerektiriyor. Bu tüm cihazın toplam değişimidir; tek başına belirli bir uygulamayı suçlamaz.",
                action = "Radar analizini incele"
            )
        }

        val capacity = summary?.capacity
        val health = capacity?.healthPercent
        if (health != null && health < 80 && capacity.confidence in setOf(EvidenceConfidence.MEDIUM, EvidenceConfidence.HIGH)) {
            recommendations += PowerRecommendation(
                id = "low_estimated_capacity",
                priority = RecommendationPriority.HIGH,
                title = "Tahmini kapasite belirgin biçimde azalmış",
                explanation = "Tekrarlanan ölçümlerin tahmini kapasitesi tasarım değerinin %$health seviyesinde. Bu bir tanı değildir; pil durumunu üretici tanılama aracı veya yetkili servis ile doğrula.",
                action = "Sağlık ve kapasite geçmişini incele"
            )
        } else if (capacity != null && capacity.confidence in setOf(EvidenceConfidence.INSUFFICIENT, EvidenceConfidence.LEARNING)) {
            recommendations += PowerRecommendation(
                id = "capacity_learning",
                priority = RecommendationPriority.INFO,
                title = "Pil sağlığı ölçümü öğrenme aşamasında",
                explanation = "Güvenilir kapasite tahmini için daha fazla ve daha geniş şarj/deşarj aralığı gerekir. Şimdilik sağlık yüzdesi üretmiyoruz; kayıtlar biriktikçe güven seviyesi güncellenecek."
            )
        }

        if (summary != null && summary.sampleCount >= 8) {
            val coverage = summary.samplingCoveragePercent
            if (coverage != null && coverage < 60) {
                recommendations += PowerRecommendation(
                    id = "low_sampling_coverage",
                    priority = RecommendationPriority.MEDIUM,
                    title = "Pil geçmişinde uzun kayıt boşlukları var",
                    explanation = "Planlanan aralıkların yalnızca %$coverage kadarı 45 dakika içinde örneklendi. Android Doze veya üretici arka plan kısıtları ölçümleri geciktirebilir; VORTIX bu boşlukları tahminle doldurmaz.",
                    action = "VORTIX arka plan izinlerini kontrol et"
                )
            }
        }

        if ((snapshot.levelPercent ?: 100) <= 20 && snapshot.powerSaveMode == false) {
            recommendations += PowerRecommendation(
                id = "enable_power_saver",
                priority = RecommendationPriority.MEDIUM,
                title = "Düşük pil seviyesinde tasarruf modunu değerlendir",
                explanation = "Pil seviyesi %${snapshot.levelPercent}. Daha uzun kullanım gerekiyorsa Android'in kendi Pil Tasarrufu modunu açmayı düşünebilirsin.",
                action = "Android pil tasarrufu ayarları"
            )
        }

        if (dormantApps.size >= 3) {
            recommendations += PowerRecommendation(
                id = "review_dormant_apps",
                priority = RecommendationPriority.INFO,
                title = "Kullanılmayan uygulamaları gözden geçir",
                explanation = "Son kullanımına göre ${dormantApps.size} uygulama en az 14 gündür açılmamış. İstemediğin uygulamaları Android ayarlarından değerlendirebilirsin; VORTIX bunları otomatik durdurmaz.",
                action = "Hibernation Vault'u aç"
            )
        }

        // Network totals are context only: never turn data volume into a battery-drain claim.
        if (networkUsage?.hasUsageAccess == false && networkUsage.note.isNotBlank()) {
            recommendations += PowerRecommendation(
                id = "optional_usage_access",
                priority = RecommendationPriority.INFO,
                title = "Gelişmiş ağ ve uygulama bağlamı kapalı",
                explanation = "Kullanım erişimi isteğe bağlıdır. Açıldığında cihazın Wi-Fi/mobil veri toplamları ve uygulama kullanım süreleri görülebilir; bunlar mAh tüketimi değildir."
            )
        }

        return recommendations
            .distinctBy { it.id }
            .sortedBy { it.priority.ordinal }
            .take(5)
            .ifEmpty {
                listOf(
                    PowerRecommendation(
                        id = "no_actionable_anomaly",
                        priority = RecommendationPriority.INFO,
                        title = "Şu an belirgin bir sorun saptanmadı",
                        explanation = "Mevcut örneklerde VORTIX'in güvenilir eşiklerle tetiklediği bir uyarı yok. Ölçümleri sürdür; bu sonuç pilin kusursuz olduğu anlamına gelmez."
                    )
                )
            }
    }
}
