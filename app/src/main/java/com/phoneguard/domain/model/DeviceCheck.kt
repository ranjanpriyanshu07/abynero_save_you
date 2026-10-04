package com.phoneguard.domain.model

/** The device settings PhoneGuard checks. */
enum class DeviceCheckType {
    SCREEN_LOCK,
    DEVELOPER_OPTIONS,
    USB_DEBUGGING,
    UNKNOWN_SOURCES,
    SECURITY_PATCH,
    PLAY_PROTECT,
}

/** Outcome of a single device check. */
enum class CheckStatus {
    /** The setting is safe. */
    PASSED,

    /** The setting is unsafe and should be fixed. */
    ISSUE,

    /** We cannot read this setting, so we show a helpful hint instead. */
    INFO,
}

/**
 * Raw device facts read from the system.
 *
 * @property unknownSourceApps names of apps that can install other apps.
 * @property unknownSourcesConfirmed true when the system confirmed these apps are
 * allowed to install apps, false when they only asked for that ability.
 * @property securityPatch the `Build.VERSION.SECURITY_PATCH` string, e.g. "2024-05-05".
 */
data class DeviceSnapshot(
    val isDeviceSecure: Boolean,
    val developerOptionsEnabled: Boolean,
    val usbDebuggingEnabled: Boolean,
    val unknownSourceApps: List<String>,
    val unknownSourcesConfirmed: Boolean,
    val securityPatch: String?,
)

/**
 * Result of one device check.
 *
 * @property patchAgeDays age of the security patch in days, for [DeviceCheckType.SECURITY_PATCH].
 * @property affectedApps app names involved, for [DeviceCheckType.UNKNOWN_SOURCES].
 */
data class DeviceCheckResult(
    val type: DeviceCheckType,
    val status: CheckStatus,
    val level: RiskLevel,
    val patchAgeDays: Long? = null,
    val securityPatch: String? = null,
    val affectedApps: List<String> = emptyList(),
) {
    val isIssue: Boolean get() = status == CheckStatus.ISSUE
}
