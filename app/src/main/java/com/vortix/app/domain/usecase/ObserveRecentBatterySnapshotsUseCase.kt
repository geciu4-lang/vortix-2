package com.vortix.app.domain.usecase

import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.repository.BatteryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveRecentBatterySnapshotsUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    operator fun invoke(
        limit: Int = 24,
        sinceEpochMillis: Long = System.currentTimeMillis() - DEFAULT_WINDOW_MILLIS
    ): Flow<List<BatterySnapshot>> {
        val safeLimit = limit.coerceAtLeast(1)
        return repository.observeHistory(sinceEpochMillis).map { list ->
            list.takeLast(safeLimit)
        }
    }

    companion object {
        private const val DEFAULT_WINDOW_MILLIS = 7L * 24L * 60L * 60L * 1000L
    }
}
