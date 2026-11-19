package com.vortix.app.domain.usecase

import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.repository.BatteryRepository
import javax.inject.Inject

class RefreshBatterySnapshotUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    suspend operator fun invoke(): BatterySnapshot = repository.refreshSnapshot()
}
