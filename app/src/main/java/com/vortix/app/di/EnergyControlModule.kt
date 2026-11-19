package com.vortix.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vortix.app.data.battery.AndroidUsageStatsDataSource
import com.vortix.app.data.local.energyDataStore
import com.vortix.app.data.repository.EnergySettingsRepositoryImpl
import com.vortix.app.data.work.WorkManagerEnergyMonitorScheduler
import com.vortix.app.domain.repository.EnergyMonitorScheduler
import com.vortix.app.domain.repository.EnergySettingsRepository
import com.vortix.app.domain.repository.UsageStatsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EnergyControlBindings {
    @Binds
    @Singleton
    abstract fun bindEnergySettingsRepository(
        implementation: EnergySettingsRepositoryImpl
    ): EnergySettingsRepository

    @Binds
    @Singleton
    abstract fun bindEnergyMonitorScheduler(
        implementation: WorkManagerEnergyMonitorScheduler
    ): EnergyMonitorScheduler

    @Binds
    @Singleton
    abstract fun bindUsageStatsRepository(
        implementation: AndroidUsageStatsDataSource
    ): UsageStatsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object EnergyControlProviders {
    @Provides
    @Singleton
    fun provideEnergyDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.energyDataStore()
}
