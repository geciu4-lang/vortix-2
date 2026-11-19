package com.vortix.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortix.app.domain.analytics.BatteryAnalyticsEngine
import com.vortix.app.domain.model.EnergyChangeMarker
import com.vortix.app.domain.repository.BatteryRepository
import com.vortix.app.domain.repository.EnergySettingsRepository
import com.vortix.app.domain.repository.NetworkUsageRepository
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
class BatteryInsightsViewModel @Inject constructor(
    private val batteryRepository: BatteryRepository,
    private val settings: EnergySettingsRepository,
    private val networkUsageRepository: NetworkUsageRepository,
    private val usageStatsRepository: UsageStatsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BatteryInsightsUiState())
    val uiState = _uiState.asStateFlow()

    private val historyStart = System.currentTimeMillis() - HISTORY_WINDOW_MILLIS

    init {
        viewModelScope.launch {
            val historyAndConfiguration = combine(
                batteryRepository.observeHistory(historyStart),
                settings.designCapacityMah,
                settings.batteryBaselineAtEpochMillis
            ) { samples, designCapacityMah, baselineAt ->
                Triple(samples, designCapacityMah, baselineAt)
            }
            combine(historyAndConfiguration, settings.energyChangeMarkers) { configuration, markers ->
                val (allSamples, designCapacityMah, baselineAt) = configuration
                val samplesForCurrentBattery = if (baselineAt > 0L) {
                    allSamples.filter { it.capturedAtEpochMillis >= baselineAt }
                } else allSamples
                val designMicroAh = designCapacityMah.takeIf { it > 0 }?.times(1_000L)
                val comparisons = markers.associate { marker ->
                    marker.atEpochMillis to BatteryAnalyticsEngine.compareScreenOffDrainAround(
                        allSamples, marker.atEpochMillis
                    )
                }
                Triple(
                    Triple(allSamples, designCapacityMah, baselineAt),
                    BatteryAnalyticsEngine.summarize(samplesForCurrentBattery, designMicroAh),
                    Pair(markers, comparisons)
                )
            }.collect { (configuration, summary, markerData) ->
                _uiState.update {
                    it.copy(
                        history = configuration.first,
                        designCapacityMah = configuration.second,
                        batteryBaselineAtEpochMillis = configuration.third,
                        summary = summary,
                        changeMarkers = markerData.first,
                        changeComparisons = markerData.second
                    )
                }
            }
        }
    }

    fun setDesignCapacityMah(value: Int) {
        viewModelScope.launch {
            try {
                settings.setDesignCapacityMah(value.coerceIn(0, 30_000))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Tasarım kapasitesi kaydedilemedi.") }
            }
        }
    }


    fun markBatteryReplaced() {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            try {
                settings.setBatteryBaselineAtEpochMillis(timestamp)
                settings.addEnergyChangeMarker(
                    EnergyChangeMarker(timestamp, "Yeni pil takıldı — öğrenme başlangıcı")
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Yeni pil başlangıcı kaydedilemedi.") }
            }
        }
    }

    fun addChangeMarker(title: String) {
        viewModelScope.launch {
            try {
                settings.addEnergyChangeMarker(
                    EnergyChangeMarker(System.currentTimeMillis(), title)
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Değişiklik notu kaydedilemedi.") }
            }
        }
    }

    fun refreshNetworkUsage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingNetwork = true, networkErrorMessage = null) }
            try {
                val network = networkUsageRepository.loadLast24Hours()
                _uiState.update { it.copy(networkUsage = network, isLoadingNetwork = false, networkErrorMessage = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingNetwork = false, networkErrorMessage = "Ağ kullanım özeti okunamadı.") }
            }
        }
    }

    fun refreshDormantApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDormantApps = true, dormantAppsErrorMessage = null) }
            try {
                val allowed = usageStatsRepository.hasUsageAccess()
                val dormant = if (allowed) usageStatsRepository.loadDormantApps(14) else emptyList()
                _uiState.update {
                    it.copy(dormantApps = dormant, hasUsageAccess = allowed, isLoadingDormantApps = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingDormantApps = false, dormantAppsErrorMessage = "Kullanılmayan uygulamalar listelenemedi.") }
            }
        }
    }

    suspend fun loadCsv(): String {
        val samples = batteryRepository.loadHistory(historyStart)
        return BatteryHistoryCsv.build(samples)
    }

    companion object {
        const val HISTORY_WINDOW_MILLIS = 90L * 24L * 60L * 60L * 1000L
    }
}
