package com.mutepoint.app.domain.model

enum class PracticeCardCategory {
    WORKPLACE,
    SOCIAL,
    HIGH_CONFLICT,
}

data class PracticeCard(
    val id: Long = 0,
    val category: PracticeCardCategory,
    val title: String,
    val script: String,
)
