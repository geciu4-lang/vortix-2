package com.vortix.app.domain.model

enum class RecommendationPriority { CRITICAL, HIGH, MEDIUM, INFO }

data class PowerRecommendation(
    val id: String,
    val priority: RecommendationPriority,
    val title: String,
    val explanation: String,
    val action: String? = null
)
