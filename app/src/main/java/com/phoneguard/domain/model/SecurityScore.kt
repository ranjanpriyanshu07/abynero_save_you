package com.phoneguard.domain.model

/** The four parts that make up the overall security score. */
enum class ScoreCategory(val weight: Double) {
    APPS(0.40),
    DEVICE(0.35),
    NETWORK(0.15),
    LINKS(0.10),
}

/** Simple grade shown next to the score. */
enum class ScoreGrade {
    GOOD,
    FAIR,
    AT_RISK;

    companion object {
        /** Grade for a 0–100 score. */
        fun forScore(score: Int): ScoreGrade = when {
            score >= 80 -> GOOD
            score >= 60 -> FAIR
            else -> AT_RISK
        }
    }
}

/** Score (0–100) for one category. */
data class CategoryScore(val category: ScoreCategory, val score: Int) {
    /** Colour level for this category's score. */
    val level: RiskLevel get() = levelForScore(score)
}

/** The overall 0–100 security score with its breakdown. */
data class SecurityScore(
    val overall: Int,
    val categories: List<CategoryScore>,
) {
    val grade: ScoreGrade get() = ScoreGrade.forScore(overall)

    /** Score for [category], or 100 when it is missing. */
    fun scoreOf(category: ScoreCategory): Int = categories.firstOrNull { it.category == category }?.score ?: 100
}

/** Maps a 0–100 score to a colour level. */
fun levelForScore(score: Int): RiskLevel = when {
    score >= 90 -> RiskLevel.SAFE
    score >= 75 -> RiskLevel.LOW
    score >= 50 -> RiskLevel.MEDIUM
    else -> RiskLevel.HIGH
}
