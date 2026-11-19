package com.vortix.app.domain.repository

import com.vortix.app.domain.model.NetworkUsageSnapshot

interface NetworkUsageRepository {
    suspend fun loadLast24Hours(): NetworkUsageSnapshot
}
