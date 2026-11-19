package com.vortix.app.data.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import com.vortix.app.domain.repository.EnergyMonitorScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerEnergyMonitorScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : EnergyMonitorScheduler {
    override fun start() {
        val request = PeriodicWorkRequestBuilder<BatteryMonitoringWorker>(15, TimeUnit.MINUTES)
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun stop() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "vortix_battery_monitor"
        const val WORK_TAG = "vortix_energy_monitor"
    }
}
