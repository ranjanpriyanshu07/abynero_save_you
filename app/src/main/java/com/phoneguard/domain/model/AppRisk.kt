package com.phoneguard.domain.model

/**
 * Sensitive capabilities an app can hold, with the number of risk points each adds.
 */
enum class RiskyPermission(val weight: Int) {
    SMS(25),
    CALL_LOG(25),
    ACCESSIBILITY(30),
    DEVICE_ADMIN(25),
    NOTIFICATION_ACCESS(20),
    OVERLAY(15),
    INSTALL_PACKAGES(15),
    MICROPHONE(10),
    CAMERA(8),
    LOCATION(8),
}

/** A plain-language reason why an app was given its risk level. */
enum class RiskReason {
    REMOTE_CONTROL_APP,
    SIDELOADED_SENSITIVE_ACCESS,
    SIDELOADED,
    ACCESSIBILITY_ENABLED,
    DEVICE_ADMIN_ACTIVE,
    READS_SMS,
    READS_CALL_LOG,
    READS_NOTIFICATIONS,
    SCREEN_OVERLAY,
    CAN_INSTALL_APPS,
    USES_MICROPHONE,
    USES_CAMERA,
    USES_LOCATION,
}

/** The single most useful thing the user can do about an app. */
enum class SuggestedAction {
    NO_ACTION,
    REVIEW_PERMISSIONS,
    TURN_OFF_ACCESSIBILITY,
    REMOVE_DEVICE_ADMIN,
    UNINSTALL_IF_UNKNOWN,
    UNINSTALL_NOW,
}

/** Where an app was installed from. */
enum class InstallSource {
    PLAY_STORE,
    GALAXY_STORE,
    AMAZON_APPSTORE,
    OTHER_APP,
    UNKNOWN;

    /** True when the app did not come from a trusted app store. */
    val isSideloaded: Boolean get() = this == OTHER_APP || this == UNKNOWN
}

/**
 * Raw facts about one installed app, gathered by the scanner and fed to the scorer.
 *
 * @property grantedPermissions runtime permissions the user has granted.
 * @property requestedPermissions every permission declared in the app's manifest.
 * @property installerPackage package name of the installer app, or null if unknown.
 */
data class AppScanInput(
    val packageName: String,
    val grantedPermissions: Set<String>,
    val requestedPermissions: Set<String>,
    val installerPackage: String?,
    val hasEnabledAccessibilityService: Boolean = false,
    val isActiveDeviceAdmin: Boolean = false,
    val hasNotificationAccess: Boolean = false,
)

/**
 * The scorer's verdict for one app.
 *
 * @property points raw risk points; higher is riskier.
 * @property reasons ordered from most to least important.
 * @property permissions the sensitive capabilities that were counted.
 */
data class AppRiskAssessment(
    val level: RiskLevel,
    val points: Int,
    val reasons: List<RiskReason>,
    val action: SuggestedAction,
    val permissions: Set<RiskyPermission>,
    val installSource: InstallSource,
    val isRemoteControlApp: Boolean,
) {
    val isSideloaded: Boolean get() = installSource.isSideloaded
}

/** One scanned app, ready to show in the UI. */
data class AppRiskReport(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val installerPackage: String?,
    val firstInstallTime: Long,
    val assessment: AppRiskAssessment,
) {
    val level: RiskLevel get() = assessment.level
}
