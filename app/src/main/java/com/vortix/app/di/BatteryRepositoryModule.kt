package com.vortix.app.di

import com.vortix.app.data.battery.AndroidBatteryDataSource
import com.vortix.app.data.battery.BatteryDataSource
import com.vortix.app.data.battery.AndroidNetworkUsageDataSource
import com.vortix.app.domain.repository.NetworkUsageRepository
import com.vortix.app.data.repository.BatteryRepositoryImpl
import com.vortix.app.domain.repository.BatteryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BatteryRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindBatteryDataSource(
        implementation: AndroidBatteryDataSource
    ): BatteryDataSource

    @Binds
    @Singleton
    abstract fun bindNetworkUsageRepository(
        implementation: AndroidNetworkUsageDataSource
    ): NetworkUsageRepository

    @Binds
    @Singleton
    abstract fun bindBatteryRepository(
        implementation: BatteryRepositoryImpl
    ): BatteryRepository
}
