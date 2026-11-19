package com.vortix.app.domain.usecase

import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.repository.BatteryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRecentBatterySnapshotsUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    operator fun invoke(limit: Int = 24): Flow<List<BatterySnapshot>> =
        repository.observeRecentSnapshots(limit)
}
