package com.phoneguard.domain.scoring

import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.ScanResult

/**
 * Stable keys for risky findings, used to notify the user only about *new* risks.
 */
object RiskKeys {
    private const val APP_PREFIX = "app:"
    private const val DEVICE_PREFIX = "device:"

    /** Key for a risky app. */
    fun forApp(packageName: String): String = APP_PREFIX + packageName

    /** Package name inside an app key, or null for other keys. */
    fun packageFromKey(key: String): String? = key.removePrefix(APP_PREFIX).takeIf { key.startsWith(APP_PREFIX) }

    /** Keys for every Medium or High finding in [result]. */
    fun from(result: ScanResult): Set<String> = buildSet {
        result.apps.filter { it.level.needsAttention }.forEach { add(forApp(it.packageName)) }
        result.deviceChecks
            .filter { it.isIssue && it.level >= RiskLevel.MEDIUM }
            .forEach { add(DEVICE_PREFIX + it.type.name) }
    }

    /** Keys in [current] that were not in [known]. */
    fun newKeys(current: Set<String>, known: Set<String>): Set<String> = current - known
}
