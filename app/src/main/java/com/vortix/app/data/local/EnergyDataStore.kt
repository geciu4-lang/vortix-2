package com.vortix.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.vortixDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "vortix_energy_settings"
)

fun Context.energyDataStore(): DataStore<Preferences> = vortixDataStore
