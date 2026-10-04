package com.phoneguard.domain.model

/** What started a scan. */
enum class ScanTrigger {
    MANUAL,
    SCHEDULED,
    NEW_APP,
}

/** Stages of a full scan, shown on the progress screen. */
enum class ScanStage {
    APPS,
    DEVICE,
    NETWORK,
    SCORING,
}

/** Live progress of a running scan. */
data class ScanProgress(
    val stage: ScanStage,
    val done: Int = 0,
    val total: Int = 0,
) {
    /** Overall progress from 0 to 1 across all stages. */
    val fraction: Float
        get() {
            val stageShare = 1f / ScanStage.entries.size
            val within = if (total > 0) done.toFloat() / total else 0f
            return (stage.ordinal * stageShare + within * stageShare).coerceIn(0f, 1f)
        }
}

/** Everything found by one full scan. */
data class ScanResult(
    val timestamp: Long,
    val apps: List<AppRiskReport>,
    val deviceChecks: List<DeviceCheckResult>,
    val wifi: WifiCheckResult,
    val linkStats: LinkStats,
    val score: SecurityScore,
) {
    val riskyApps: List<AppRiskReport> get() = apps.filter { it.level.needsAttention }
    val highRiskAppCount: Int get() = apps.count { it.level == RiskLevel.HIGH }
    val mediumRiskAppCount: Int get() = apps.count { it.level == RiskLevel.MEDIUM }
    val deviceIssueCount: Int get() = deviceChecks.count { it.isIssue }
}

/** Shared scan state observed by the UI. */
data class ScanState(
    val isScanning: Boolean = false,
    val progress: ScanProgress? = null,
    val result: ScanResult? = null,
    val lastScanFailed: Boolean = false,
)

/** Result of a full scan plus the risks that were not seen in the previous scan. */
data class ScanOutcome(
    val result: ScanResult,
    val newRiskKeys: Set<String>,
)

/** One saved scan in the history list. */
data class ScanHistoryEntry(
    val id: Long,
    val timestamp: Long,
    val score: Int,
    val appsScore: Int,
    val deviceScore: Int,
    val networkScore: Int,
    val linksScore: Int,
    val appsScanned: Int,
    val highRiskApps: Int,
    val mediumRiskApps: Int,
    val deviceIssues: Int,
    val trigger: ScanTrigger,
)
