package com.vortix.app.feature.controls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortix.app.domain.repository.EnergyMonitorScheduler
import com.vortix.app.domain.repository.EnergySettingsRepository
import com.vortix.app.domain.repository.UsageStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EnergyControlsViewModel @Inject constructor(
    private val settings: EnergySettingsRepository,
    private val monitorScheduler: EnergyMonitorScheduler,
    private val usageStats: UsageStatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnergyControlsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val part1 = combine(
                settings.monitoringEnabled,
                settings.chargeLimitEnabled,
                settings.chargeTargetPercent,
                settings.lowBatteryAlertEnabled,
                settings.lowBatteryThresholdPercent
            ) { monitoring, chargeLimit, chargeTarget, lowBatAlert, lowBatThreshold ->
                Part1(monitoring, chargeLimit, chargeTarget, lowBatAlert, lowBatThreshold)
            }
            val part2 = combine(
                settings.thermalAlertEnabled,
                settings.thermalThresholdCelsius,
                settings.ecoGuardEnabled,
                settings.designCapacityMah
            ) { thermalAlert, thermalThreshold, ecoGuard, designCap ->
                Part2(thermalAlert, thermalThreshold, ecoGuard, designCap)
            }
            combine(part1, part2) { p1, p2 -> p1 to p2 }.collect { (p1, p2) ->
                _uiState.update {
                    it.copy(
                        isMonitoringEnabled = p1.monitoring,
                        isChargeLimitEnabled = p1.chargeLimit,
                        chargeTargetPercent = p1.chargeTarget,
                        isLowBatteryAlertEnabled = p1.lowBatAlert,
                        lowBatteryThresholdPercent = p1.lowBatThreshold,
                        isThermalAlertEnabled = p2.thermalAlert,
                        thermalThresholdCelsius = p2.thermalThreshold,
                        isEcoGuardEnabled = p2.ecoGuard,
                        designCapacityMah = p2.designCap
                    )
                }
            }
        }
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled) monitorScheduler.start() else monitorScheduler.stop()
                settings.setMonitoringEnabled(enabled)
                _uiState.update {
                    it.copy(message = if (enabled) "Arka plan koruması ve ölçüm başlatıldı." else "Arka plan ölçümü durduruldu.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(message = "Ölçüm planı güncellenemedi.") }
            }
        }
    }

    fun setChargeLimitEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settings.setChargeLimitEnabled(enabled)
                if (enabled && !_uiState.value.isMonitoringEnabled) {
                    monitorScheduler.start()
                    settings.setMonitoringEnabled(true)
                }
                _uiState.update {
                    it.copy(
                        message = if (enabled) "Şarj sınırı koruması aktif edildi."
                        else "Şarj sınırı koruması kapatıldı."
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(message = "Şarj sınırı ayarı kaydedilemedi.") }
            }
        }
    }

    fun setChargeTarget(percent: Int) {
        val safeTarget = percent.coerceIn(70, 100)
        viewModelScope.launch {
            try {
                settings.setChargeTargetPercent(safeTarget)
                _uiState.update {
                    it.copy(message = "Şarj sınırı %$safeTarget olarak güncellendi.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(message = "Şarj hedefi kaydedilemedi.") }
            }
        }
    }

    fun setLowBatteryAlertEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settings.setLowBatteryAlertEnabled(enabled)
                if (enabled && !_uiState.value.isMonitoringEnabled) {
                    monitorScheduler.start()
                    settings.setMonitoringEnabled(true)
                }
                _uiState.update {
                    it.copy(
                        message = if (enabled) "Düşük pil uyarısı açıldı."
                        else "Düşük pil uyarısı kapatıldı."
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(message = "Düşük pil ayarı kaydedilemedi.") }
            }
        }
    }

    fun setLowBatteryThreshold(percent: Int) {
        val safePercent = percent.coerceIn(10, 40)
        viewModelScope.launch {
            try {
                settings.setLowBatteryThresholdPercent(safePercent)
                _uiState.update {
                    it.copy(message = "Düşük pil uyarı eşiği %$safePercent olarak ayarlandı.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(message = "Düşük pil eşiği kaydedilemedi.") }
            }
        }
    }

    fun setThermalAlertEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settings.setThermalAlertEnabled(enabled)
                if (enabled && !_uiState.value.isMonitoringEnabled) {
                    monitorScheduler.start()
                    settings.setMonitoringEnabled(true)
                }
                _uiState.update {
                    it.copy(
                        message = if (enabled) "Termal koruma uyarısı açıldı."
                        else "Termal koruma uyarısı kapatıldı."
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(message = "Termal uyarı ayarı kaydedilemedi.") }
            }
        }
    }

    fun setThermalThreshold(celsius: Int) {
        val safeTemp = celsius.coerceIn(36, 48)
        viewModelScope.launch {
            try {
                settings.setThermalThresholdCelsius(safeTemp)
                _uiState.update {
                    it.copy(message = "Sıcaklık uyarı sınırı $safeTemp °C olarak ayarlandı.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(message = "Sıcaklık sınırı kaydedilemedi.") }
            }
        }
    }

    fun setEcoGuardEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settings.setEcoGuardEnabled(enabled)
                if (enabled && !_uiState.value.isMonitoringEnabled) {
                    monitorScheduler.start()
                    settings.setMonitoringEnabled(true)
                }
                _uiState.update {
                    it.copy(
                        message = if (enabled) "Eco Guard profili aktif edildi."
                        else "Eco Guard profili kapatıldı."
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(message = "Eco Guard ayarı güncellenemedi.") }
            }
        }
    }

    fun setDesignCapacityMah(capacityMah: Int) {
        val safeCap = capacityMah.coerceIn(1500, 12000)
        viewModelScope.launch {
            try {
                settings.setDesignCapacityMah(safeCap)
                _uiState.update {
                    it.copy(message = "Tasarım kapasitesi $safeCap mAh olarak kalibre edildi.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(message = "Kapasite ayarı kaydedilemedi.") }
            }
        }
    }

    fun refreshUsageApps() {
        viewModelScope.launch {
            try {
                val hasAccess = usageStats.hasUsageAccess()
                _uiState.update { it.copy(hasUsageAccess = hasAccess, isLoadingUsage = hasAccess) }
                if (!hasAccess) {
                    _uiState.update { it.copy(topApps = emptyList(), isLoadingUsage = false) }
                    return@launch
                }
                val apps = usageStats.loadTodayUsage()
                _uiState.update { it.copy(topApps = apps, isLoadingUsage = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoadingUsage = false, message = "Uygulama kullanım verileri okunamadı.")
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private data class Part1(
        val monitoring: Boolean,
        val chargeLimit: Boolean,
        val chargeTarget: Int,
        val lowBatAlert: Boolean,
        val lowBatThreshold: Int
    )

    private data class Part2(
        val thermalAlert: Boolean,
        val thermalThreshold: Int,
        val ecoGuard: Boolean,
        val designCap: Int
    )
}
