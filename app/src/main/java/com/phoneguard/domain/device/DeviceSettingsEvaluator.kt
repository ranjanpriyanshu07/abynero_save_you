package com.phoneguard.domain.device

import com.phoneguard.domain.model.CheckStatus
import com.phoneguard.domain.model.DeviceCheckResult
import com.phoneguard.domain.model.DeviceCheckType
import com.phoneguard.domain.model.DeviceSnapshot
import com.phoneguard.domain.model.RiskLevel
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Turns raw device settings into a list of pass/fail checks with severities.
 */
class DeviceSettingsEvaluator @Inject constructor(private val clock: Clock) {

    /** Evaluates every check, in the order shown to the user. */
    fun evaluate(snapshot: DeviceSnapshot): List<DeviceCheckResult> = listOf(
        screenLock(snapshot),
        usbDebugging(snapshot),
        developerOptions(snapshot),
        unknownSources(snapshot),
        securityPatch(snapshot.securityPatch),
        DeviceCheckResult(DeviceCheckType.PLAY_PROTECT, CheckStatus.INFO, RiskLevel.LOW),
    )

    private fun screenLock(s: DeviceSnapshot) = if (s.isDeviceSecure) {
        passed(DeviceCheckType.SCREEN_LOCK)
    } else {
        DeviceCheckResult(DeviceCheckType.SCREEN_LOCK, CheckStatus.ISSUE, RiskLevel.HIGH)
    }

    private fun usbDebugging(s: DeviceSnapshot) = if (s.usbDebuggingEnabled) {
        DeviceCheckResult(DeviceCheckType.USB_DEBUGGING, CheckStatus.ISSUE, RiskLevel.MEDIUM)
    } else {
        passed(DeviceCheckType.USB_DEBUGGING)
    }

    private fun developerOptions(s: DeviceSnapshot) = if (s.developerOptionsEnabled) {
        DeviceCheckResult(DeviceCheckType.DEVELOPER_OPTIONS, CheckStatus.ISSUE, RiskLevel.LOW)
    } else {
        passed(DeviceCheckType.DEVELOPER_OPTIONS)
    }

    private fun unknownSources(s: DeviceSnapshot) = when {
        s.unknownSourceApps.isEmpty() -> passed(DeviceCheckType.UNKNOWN_SOURCES)
        else -> DeviceCheckResult(
            type = DeviceCheckType.UNKNOWN_SOURCES,
            status = CheckStatus.ISSUE,
            level = if (s.unknownSourcesConfirmed) RiskLevel.MEDIUM else RiskLevel.LOW,
            affectedApps = s.unknownSourceApps,
        )
    }

    /** Checks how old the Android security patch is. */
    fun securityPatch(patch: String?): DeviceCheckResult {
        val date = patch?.let { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }
            ?: return DeviceCheckResult(DeviceCheckType.SECURITY_PATCH, CheckStatus.INFO, RiskLevel.LOW, securityPatch = patch)
        val age = ChronoUnit.DAYS.between(date, LocalDate.now(clock)).coerceAtLeast(0)
        val level = when {
            age <= PATCH_OK_DAYS -> RiskLevel.SAFE
            age <= PATCH_LOW_DAYS -> RiskLevel.LOW
            age <= PATCH_MEDIUM_DAYS -> RiskLevel.MEDIUM
            else -> RiskLevel.HIGH
        }
        val status = if (level == RiskLevel.SAFE) CheckStatus.PASSED else CheckStatus.ISSUE
        return DeviceCheckResult(DeviceCheckType.SECURITY_PATCH, status, level, patchAgeDays = age, securityPatch = patch)
    }

    private fun passed(type: DeviceCheckType) = DeviceCheckResult(type, CheckStatus.PASSED, RiskLevel.SAFE)

    companion object {
        const val PATCH_OK_DAYS = 90L
        const val PATCH_LOW_DAYS = 180L
        const val PATCH_MEDIUM_DAYS = 365L
    }
}
