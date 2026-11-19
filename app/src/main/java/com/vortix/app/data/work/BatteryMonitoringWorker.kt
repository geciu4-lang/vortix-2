package com.vortix.app.data.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vortix.app.domain.model.BatteryStatus
import com.vortix.app.domain.repository.EnergySettingsRepository
import com.vortix.app.domain.repository.BatteryRepository
import com.vortix.app.domain.usecase.RefreshBatterySnapshotUseCase
import com.vortix.app.widget.VortixBatteryWidgetProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@HiltWorker
class BatteryMonitoringWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val refreshBatterySnapshot: RefreshBatterySnapshotUseCase,
    private val settings: EnergySettingsRepository,
    private val batteryRepository: BatteryRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val snapshot = refreshBatterySnapshot()
            VortixBatteryWidgetProvider.updateAll(applicationContext)
            // Keep the raw history bounded; 180 days is ample for trend analysis and local privacy.
            batteryRepository.pruneHistory(System.currentTimeMillis() - 180L * 24L * 60L * 60L * 1000L)
            val target = settings.chargeTargetPercent.first()
            val alertAlreadySent = settings.chargeTargetAlertSent.first()
            val level = snapshot.levelPercent
            val targetReached = snapshot.status == BatteryStatus.CHARGING &&
                level != null && level >= target

            if (targetReached && !alertAlreadySent && notifyChargeTargetReached(level!!, target)) {
                settings.setChargeTargetAlertSent(true)
            } else if (!targetReached && alertAlreadySent) {
                settings.setChargeTargetAlertSent(false)
            }

            val thermalAlertAlreadySent = settings.thermalAlertSent.first()
            val batteryTooHot = (snapshot.temperatureCelsius ?: Double.NEGATIVE_INFINITY) >= HIGH_BATTERY_TEMPERATURE_C ||
                (snapshot.thermalStatus ?: 0) >= SEVERE_THERMAL_STATUS
            val batteryCooled = (snapshot.temperatureCelsius?.let { it <= RESET_BATTERY_TEMPERATURE_C }
                ?: (snapshot.thermalStatus?.let { it < SEVERE_THERMAL_STATUS } ?: false)) &&
                (snapshot.thermalStatus?.let { it < SEVERE_THERMAL_STATUS } ?: true)
            if (batteryTooHot && !thermalAlertAlreadySent && notifyThermalWarning(snapshot.temperatureCelsius)) {
                settings.setThermalAlertSent(true)
            } else if (batteryCooled && thermalAlertAlreadySent) {
                settings.setThermalAlertSent(false)
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun notifyChargeTargetReached(level: Int, target: Int): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return false

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
            ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Şarj hedefi uyarıları",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Vortix şarj hedefi bildirimleri" }
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Vortix şarj hedefi")
            .setContentText("Pil %$level seviyesinde. Belirlediğin %$target hedefini geçti.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
        return true
    }


    private fun notifyThermalWarning(temperatureCelsius: Double?): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return false

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
            ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    THERMAL_CHANNEL_ID,
                    "Pil sıcaklığı uyarıları",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Vortix pil sıcaklığı ve termal durum uyarıları" }
            )
        }
        val detail = temperatureCelsius?.let { "Pil sıcaklığı ${"%.1f".format(java.util.Locale.US, it)}°C." }
            ?: "Android termal durumu ciddi seviyeye ulaştı."
        val notification = NotificationCompat.Builder(applicationContext, THERMAL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Vortix · Pil sıcaklığı yüksek")
            .setContentText("$detail Mümkünse ağır kullanımı durdurup cihazı serinlet.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(THERMAL_NOTIFICATION_ID, notification)
        return true
    }

    private companion object {
        const val CHANNEL_ID = "vortix_charge_target"
        const val NOTIFICATION_ID = 4101
        const val THERMAL_NOTIFICATION_ID = 4102
        const val THERMAL_CHANNEL_ID = "vortix_thermal_alerts"
        const val HIGH_BATTERY_TEMPERATURE_C = 42.0
        const val RESET_BATTERY_TEMPERATURE_C = 39.0
        const val SEVERE_THERMAL_STATUS = 3
    }
}
