package com.phoneguard.domain.model

/**
 * Severity of a finding, ordered from least to most serious.
 *
 * Enum order matters: comparisons such as `level >= RiskLevel.MEDIUM` rely on it.
 */
enum class RiskLevel {
    SAFE,
    LOW,
    MEDIUM,
    HIGH;

    /** True when the finding deserves the user's attention (Medium or High). */
    val needsAttention: Boolean get() = this >= MEDIUM

    /** Returns the next more serious level, or [HIGH] if already at the top. */
    fun bump(): RiskLevel = entries[(ordinal + 1).coerceAtMost(HIGH.ordinal)]

    companion object {
        /** The most serious level in [levels], or [SAFE] when empty. */
        fun max(levels: Iterable<RiskLevel>): RiskLevel = levels.maxOrNull() ?: SAFE
    }
}
