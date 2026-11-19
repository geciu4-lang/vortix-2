package com.vortix.app.di

import android.content.Context
import androidx.room.Room
import com.vortix.app.data.local.VortixDatabase
import com.vortix.app.data.local.MIGRATION_1_2
import com.vortix.app.data.local.dao.BatterySampleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VortixDatabase =
        Room.databaseBuilder(
            context,
            VortixDatabase::class.java,
            "vortix-energy.db"
        ).addMigrations(MIGRATION_1_2).build()

    @Provides
    fun provideBatterySampleDao(database: VortixDatabase): BatterySampleDao =
        database.batterySampleDao()
}
