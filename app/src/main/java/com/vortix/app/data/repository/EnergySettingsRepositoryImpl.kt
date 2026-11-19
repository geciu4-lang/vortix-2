package com.vortix.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.vortix.app.domain.model.EnergyChangeMarker
import com.vortix.app.domain.repository.EnergySettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class EnergySettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : EnergySettingsRepository {
    override val monitoringEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[MONITORING_ENABLED] ?: false
    }

    override val chargeLimitEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[CHARGE_LIMIT_ENABLED] ?: false
    }

    override val chargeTargetPercent: Flow<Int> = dataStore.data.map { prefs ->
        (prefs[CHARGE_TARGET_PERCENT] ?: DEFAULT_CHARGE_TARGET).coerceIn(70, 100)
    }

    override val chargeTargetAlertSent: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[CHARGE_TARGET_ALERT_SENT] ?: false
    }

    override val lowBatteryAlertEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[LOW_BATTERY_ALERT_ENABLED] ?: false
    }

    override val lowBatteryThresholdPercent: Flow<Int> = dataStore.data.map { prefs ->
        (prefs[LOW_BATTERY_THRESHOLD_PERCENT] ?: DEFAULT_LOW_BATTERY_THRESHOLD).coerceIn(10, 40)
    }

    override val thermalAlertEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[THERMAL_ALERT_ENABLED] ?: false
    }

    override val thermalThresholdCelsius: Flow<Int> = dataStore.data.map { prefs ->
        (prefs[THERMAL_THRESHOLD_CELSIUS] ?: DEFAULT_THERMAL_THRESHOLD).coerceIn(36, 48)
    }

    override val thermalAlertSent: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[THERMAL_ALERT_SENT] ?: false
    }

    override val ecoGuardEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ECO_GUARD_ENABLED] ?: false
    }

    override val designCapacityMah: Flow<Int> = dataStore.data.map { prefs ->
        (prefs[DESIGN_CAPACITY_MAH] ?: 0).coerceIn(0, 30_000)
    }

    override val batteryBaselineAtEpochMillis: Flow<Long> = dataStore.data.map { prefs ->
        prefs[BATTERY_BASELINE_AT] ?: 0L
    }

    override val energyChangeMarkers: Flow<List<EnergyChangeMarker>> = dataStore.data.map { prefs ->
        prefs[ENERGY_CHANGE_MARKERS].orEmpty().mapNotNull { value ->
            val split = value.split('|', limit = 2)
            val at = split.firstOrNull()?.toLongOrNull() ?: return@mapNotNull null
            val title = split.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            EnergyChangeMarker(at, title)
        }.sortedByDescending { it.atEpochMillis }
    }

    override suspend fun setMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { it[MONITORING_ENABLED] = enabled }
    }

    override suspend fun setChargeLimitEnabled(enabled: Boolean) {
        dataStore.edit { it[CHARGE_LIMIT_ENABLED] = enabled }
    }

    override suspend fun setChargeTargetPercent(percent: Int) {
        dataStore.edit { it[CHARGE_TARGET_PERCENT] = percent.coerceIn(70, 100) }
    }

    override suspend fun setChargeTargetAlertSent(sent: Boolean) {
        dataStore.edit { it[CHARGE_TARGET_ALERT_SENT] = sent }
    }

    override suspend fun setLowBatteryAlertEnabled(enabled: Boolean) {
        dataStore.edit { it[LOW_BATTERY_ALERT_ENABLED] = enabled }
    }

    override suspend fun setLowBatteryThresholdPercent(percent: Int) {
        dataStore.edit { it[LOW_BATTERY_THRESHOLD_PERCENT] = percent.coerceIn(10, 40) }
    }

    override suspend fun setThermalAlertEnabled(enabled: Boolean) {
        dataStore.edit { it[THERMAL_ALERT_ENABLED] = enabled }
    }

    override suspend fun setThermalThresholdCelsius(celsius: Int) {
        dataStore.edit { it[THERMAL_THRESHOLD_CELSIUS] = celsius.coerceIn(36, 48) }
    }

    override suspend fun setThermalAlertSent(sent: Boolean) {
        dataStore.edit { it[THERMAL_ALERT_SENT] = sent }
    }

    override suspend fun setEcoGuardEnabled(enabled: Boolean) {
        dataStore.edit { it[ECO_GUARD_ENABLED] = enabled }
    }

    override suspend fun setDesignCapacityMah(capacityMah: Int) {
        dataStore.edit { it[DESIGN_CAPACITY_MAH] = capacityMah.coerceIn(0, 30_000) }
    }

    override suspend fun setBatteryBaselineAtEpochMillis(epochMillis: Long) {
        dataStore.edit { it[BATTERY_BASELINE_AT] = epochMillis.coerceAtLeast(0L) }
    }

    override suspend fun addEnergyChangeMarker(marker: EnergyChangeMarker) {
        val title = marker.title.trim().replace('|', ' ').take(80)
        if (title.isBlank()) return
        val value = "${marker.atEpochMillis}|$title"
        dataStore.edit { prefs ->
            val updated = (prefs[ENERGY_CHANGE_MARKERS].orEmpty() + value)
                .distinct()
                .sortedByDescending { it.substringBefore('|').toLongOrNull() ?: 0L }
                .take(50)
                .toSet()
            prefs[ENERGY_CHANGE_MARKERS] = updated
        }
    }

    private companion object {
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val CHARGE_LIMIT_ENABLED = booleanPreferencesKey("charge_limit_enabled")
        val CHARGE_TARGET_PERCENT = intPreferencesKey("charge_target_percent")
        val CHARGE_TARGET_ALERT_SENT = booleanPreferencesKey("charge_target_alert_sent")
        val LOW_BATTERY_ALERT_ENABLED = booleanPreferencesKey("low_battery_alert_enabled")
        val LOW_BATTERY_THRESHOLD_PERCENT = intPreferencesKey("low_battery_threshold_percent")
        val THERMAL_ALERT_ENABLED = booleanPreferencesKey("thermal_alert_enabled")
        val THERMAL_THRESHOLD_CELSIUS = intPreferencesKey("thermal_threshold_celsius")
        val THERMAL_ALERT_SENT = booleanPreferencesKey("thermal_alert_sent")
        val ECO_GUARD_ENABLED = booleanPreferencesKey("eco_guard_enabled")
        val DESIGN_CAPACITY_MAH = intPreferencesKey("design_capacity_mah")
        val ENERGY_CHANGE_MARKERS = stringSetPreferencesKey("energy_change_markers")
        val BATTERY_BASELINE_AT = longPreferencesKey("battery_baseline_at_epoch_millis")
        const val DEFAULT_CHARGE_TARGET = 80
        const val DEFAULT_LOW_BATTERY_THRESHOLD = 20
        const val DEFAULT_THERMAL_THRESHOLD = 42
    }
}
