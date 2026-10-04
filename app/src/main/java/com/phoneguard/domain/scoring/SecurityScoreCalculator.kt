package com.phoneguard.domain.scoring

import com.phoneguard.domain.model.AppRiskReport
import com.phoneguard.domain.model.CategoryScore
import com.phoneguard.domain.model.DeviceCheckResult
import com.phoneguard.domain.model.DeviceCheckType
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.ScoreCategory
import com.phoneguard.domain.model.SecurityScore
import com.phoneguard.domain.model.WifiCheckResult
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Combines every check into one 0–100 security score.
 *
 * Each category starts at 100 and loses points for problems. The overall score is a
 * weighted average ([ScoreCategory.weight]). If anything is High risk, the overall
 * score is capped at [CRITICAL_CAP] so a dangerous app can never hide behind a
 * good average.
 */
class SecurityScoreCalculator @Inject constructor() {

    /** Calculates the full score with its breakdown. */
    fun calculate(
        apps: List<AppRiskReport>,
        deviceChecks: List<DeviceCheckResult>,
        wifi: WifiCheckResult?,
        dangerousLinksLastWeek: Int,
    ): SecurityScore {
        val categories = listOf(
            CategoryScore(ScoreCategory.APPS, appsScore(apps)),
            CategoryScore(ScoreCategory.DEVICE, deviceScore(deviceChecks)),
            CategoryScore(ScoreCategory.NETWORK, networkScore(wifi)),
            CategoryScore(ScoreCategory.LINKS, linksScore(dangerousLinksLastWeek)),
        )
        var overall = categories.sumOf { it.score * it.category.weight }.roundToInt().coerceIn(0, 100)
        val hasCritical = apps.any { it.level == RiskLevel.HIGH } ||
            deviceChecks.any { it.isIssue && it.level == RiskLevel.HIGH }
        if (hasCritical) overall = overall.coerceAtMost(CRITICAL_CAP)
        return SecurityScore(overall = overall, categories = categories)
    }

    /** Apps score: High −25, Medium −10, Low −2 each (Low capped at −10 in total). */
    fun appsScore(apps: List<AppRiskReport>): Int {
        val high = apps.count { it.level == RiskLevel.HIGH } * 25
        val medium = apps.count { it.level == RiskLevel.MEDIUM } * 10
        val low = (apps.count { it.level == RiskLevel.LOW } * 2).coerceAtMost(10)
        return (100 - high - medium - low).coerceIn(0, 100)
    }

    /** Device score: each failing setting removes a fixed number of points. */
    fun deviceScore(checks: List<DeviceCheckResult>): Int {
        val penalty = checks.filter { it.isIssue }.sumOf { penaltyFor(it) }
        return (100 - penalty).coerceIn(0, 100)
    }

    /** Network score: open or WEP Wi-Fi is a big risk; unknown is a small one. */
    fun networkScore(wifi: WifiCheckResult?): Int = when (wifi?.level) {
        null, RiskLevel.SAFE -> 100
        RiskLevel.LOW -> 90
        RiskLevel.MEDIUM -> 70
        RiskLevel.HIGH -> 40
    }

    /** Links score: dangerous links received this week suggest the user is being targeted. */
    fun linksScore(dangerousLinksLastWeek: Int): Int =
        (100 - dangerousLinksLastWeek.coerceAtLeast(0) * 15).coerceAtLeast(MIN_LINKS_SCORE)

    private fun penaltyFor(check: DeviceCheckResult): Int = when (check.type) {
        DeviceCheckType.SCREEN_LOCK -> 40
        DeviceCheckType.USB_DEBUGGING -> 20
        DeviceCheckType.DEVELOPER_OPTIONS -> 10
        DeviceCheckType.UNKNOWN_SOURCES -> if (check.level >= RiskLevel.MEDIUM) 15 else 5
        DeviceCheckType.SECURITY_PATCH -> when (check.level) {
            RiskLevel.HIGH -> 25
            RiskLevel.MEDIUM -> 15
            RiskLevel.LOW -> 5
            RiskLevel.SAFE -> 0
        }
        DeviceCheckType.PLAY_PROTECT -> 0
    }

    companion object {
        /** Highest overall score allowed while any High-risk finding exists. */
        const val CRITICAL_CAP = 59
        private const val MIN_LINKS_SCORE = 40
    }
}
