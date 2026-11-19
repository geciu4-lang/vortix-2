package com.vortix.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortix.app.data.battery.SystemDesignCapacityReader
import com.vortix.app.domain.repository.BatteryRepository
import com.vortix.app.domain.repository.EnergySettingsRepository
import com.vortix.app.domain.usecase.ObserveLatestBatterySnapshotUseCase
import com.vortix.app.domain.usecase.ObserveRecentBatterySnapshotsUseCase
import com.vortix.app.domain.usecase.RefreshBatterySnapshotUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeLatestSnapshot: ObserveLatestBatterySnapshotUseCase,
    private val observeRecentSnapshots: ObserveRecentBatterySnapshotsUseCase,
    private val refreshSnapshot: RefreshBatterySnapshotUseCase,
    private val batteryRepository: BatteryRepository,
    private val settingsRepository: EnergySettingsRepository,
    private val systemCapacityReader: SystemDesignCapacityReader
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Cihazın sistem PowerProfile değerini ilk açılışta kontrol et
            val systemCap = systemCapacityReader.readSystemDesignCapacityMah()
            if (systemCap != null) {
                val currentSaved = settingsRepository.designCapacityMah.first()
                if (currentSaved == 5000 && systemCap != 5000) {
                    settingsRepository.setDesignCapacityMah(systemCap)
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.designCapacityMah
                .catch { cause -> if (cause is CancellationException) throw cause }
                .collect { cap ->
                    _uiState.update { it.copy(designCapacityMah = cap) }
                }
        }

        viewModelScope.launch {
            observeLatestSnapshot()
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    _uiState.update { current ->
                        current.copy(errorMessage = "Kayıtlı pil verileri okunamadı.")
                    }
                }
                .collect { snapshot ->
                    if (snapshot != null) {
                        _uiState.update { current -> current.copy(snapshot = snapshot) }
                    }
                }
        }

        viewModelScope.launch {
            batteryRepository.observeLiveBattery()
                .catch { cause -> if (cause is CancellationException) throw cause }
                .collect { liveSnapshot ->
                    _uiState.update { current -> current.copy(snapshot = liveSnapshot) }
                }
        }

        viewModelScope.launch {
            observeRecentSnapshots(24)
                .catch { cause -> if (cause is CancellationException) throw cause }
                .collect { list ->
                    _uiState.update { current -> current.copy(recentSnapshots = list) }
                }
        }

        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val snapshot = refreshSnapshot()
                _uiState.update {
                    it.copy(snapshot = snapshot, isRefreshing = false, errorMessage = null)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = "Pil bilgisi şu anda okunamıyor. Tekrar deneyin."
                    )
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            runCatching { batteryRepository.clearHistory() }
        }
    }
}
