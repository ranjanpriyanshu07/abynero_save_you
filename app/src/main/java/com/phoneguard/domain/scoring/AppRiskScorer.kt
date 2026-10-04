package com.phoneguard.domain.scoring

import com.phoneguard.domain.model.AppRiskAssessment
import com.phoneguard.domain.model.AppScanInput
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.RiskReason
import com.phoneguard.domain.model.RiskyPermission
import com.phoneguard.domain.model.SuggestedAction
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Turns raw facts about an app into a risk level, reasons and a suggested action.
 *
 * Rules, in short:
 * - Each sensitive capability adds points ([RiskyPermission.weight]).
 * - Apps from trusted stores get a discount; sideloaded apps get extra points.
 * - Remote-control apps are always High (they are the core of screen-sharing scams).
 * - A sideloaded app with SMS, call log, accessibility, device admin or notification
 *   access is always High: that combination is how banking trojans steal OTPs.
 */
class AppRiskScorer @Inject constructor() {

    /** Scores one app. Never throws. */
    fun assess(input: AppScanInput): AppRiskAssessment {
        val permissions = detectPermissions(input)
        val source = InstallSources.classify(input.installerPackage)
        val sideloaded = source.isSideloaded
        val remoteControl = RemoteControlApps.isRemoteControl(input.packageName)
        val hasSensitiveAccess = permissions.any { it in SENSITIVE }

        val basePoints = permissions.sumOf { it.weight }
        val points = if (sideloaded) {
            basePoints + SIDELOADED_POINTS
        } else {
            (basePoints * TRUSTED_STORE_FACTOR).roundToInt()
        }

        var level = levelForPoints(points)
        if (remoteControl || (sideloaded && hasSensitiveAccess)) level = RiskLevel.HIGH

        val reasons = buildReasons(permissions, sideloaded, remoteControl, hasSensitiveAccess)
        val action = suggestAction(level, permissions, sideloaded, remoteControl, hasSensitiveAccess)

        return AppRiskAssessment(
            level = level,
            points = points,
            reasons = reasons,
            action = action,
            permissions = permissions,
            installSource = source,
            isRemoteControlApp = remoteControl,
        )
    }

    /** Works out which sensitive capabilities count against the app. */
    fun detectPermissions(input: AppScanInput): Set<RiskyPermission> = buildSet {
        val granted = input.grantedPermissions
        if (granted.any { it in PermissionGroups.SMS }) add(RiskyPermission.SMS)
        if (granted.any { it in PermissionGroups.CALL_LOG }) add(RiskyPermission.CALL_LOG)
        if (granted.any { it in PermissionGroups.MICROPHONE }) add(RiskyPermission.MICROPHONE)
        if (granted.any { it in PermissionGroups.CAMERA }) add(RiskyPermission.CAMERA)
        if (granted.any { it in PermissionGroups.LOCATION }) add(RiskyPermission.LOCATION)
        // Special app-op permissions are not reported as "granted" in the manifest flags,
        // so we count them when they are requested.
        if (PermissionGroups.OVERLAY in input.requestedPermissions) add(RiskyPermission.OVERLAY)
        if (PermissionGroups.INSTALL_PACKAGES in input.requestedPermissions) add(RiskyPermission.INSTALL_PACKAGES)
        if (input.hasEnabledAccessibilityService) add(RiskyPermission.ACCESSIBILITY)
        if (input.isActiveDeviceAdmin) add(RiskyPermission.DEVICE_ADMIN)
        if (input.hasNotificationAccess) add(RiskyPermission.NOTIFICATION_ACCESS)
    }

    private fun buildReasons(
        permissions: Set<RiskyPermission>,
        sideloaded: Boolean,
        remoteControl: Boolean,
        hasSensitiveAccess: Boolean,
    ): List<RiskReason> = buildList {
        if (remoteControl) add(RiskReason.REMOTE_CONTROL_APP)
        if (sideloaded && hasSensitiveAccess) add(RiskReason.SIDELOADED_SENSITIVE_ACCESS)
        if (sideloaded) add(RiskReason.SIDELOADED)
        permissions.sortedByDescending { it.weight }.forEach { add(reasonFor(it)) }
    }

    private fun reasonFor(permission: RiskyPermission): RiskReason = when (permission) {
        RiskyPermission.SMS -> RiskReason.READS_SMS
        RiskyPermission.CALL_LOG -> RiskReason.READS_CALL_LOG
        RiskyPermission.ACCESSIBILITY -> RiskReason.ACCESSIBILITY_ENABLED
        RiskyPermission.DEVICE_ADMIN -> RiskReason.DEVICE_ADMIN_ACTIVE
        RiskyPermission.NOTIFICATION_ACCESS -> RiskReason.READS_NOTIFICATIONS
        RiskyPermission.OVERLAY -> RiskReason.SCREEN_OVERLAY
        RiskyPermission.INSTALL_PACKAGES -> RiskReason.CAN_INSTALL_APPS
        RiskyPermission.MICROPHONE -> RiskReason.USES_MICROPHONE
        RiskyPermission.CAMERA -> RiskReason.USES_CAMERA
        RiskyPermission.LOCATION -> RiskReason.USES_LOCATION
    }

    private fun suggestAction(
        level: RiskLevel,
        permissions: Set<RiskyPermission>,
        sideloaded: Boolean,
        remoteControl: Boolean,
        hasSensitiveAccess: Boolean,
    ): SuggestedAction = when {
        remoteControl -> SuggestedAction.UNINSTALL_NOW
        sideloaded && hasSensitiveAccess -> SuggestedAction.UNINSTALL_NOW
        level.needsAttention && RiskyPermission.ACCESSIBILITY in permissions -> SuggestedAction.TURN_OFF_ACCESSIBILITY
        level.needsAttention && RiskyPermission.DEVICE_ADMIN in permissions -> SuggestedAction.REMOVE_DEVICE_ADMIN
        sideloaded -> SuggestedAction.UNINSTALL_IF_UNKNOWN
        level == RiskLevel.SAFE -> SuggestedAction.NO_ACTION
        else -> SuggestedAction.REVIEW_PERMISSIONS
    }

    companion object {
        /** Points added to any app that did not come from a trusted store. */
        const val SIDELOADED_POINTS = 20

        /** Multiplier for apps from Play Store, Galaxy Store or Amazon Appstore. */
        const val TRUSTED_STORE_FACTOR = 0.6

        /** Point thresholds for each level. */
        const val LOW_THRESHOLD = 1
        const val MEDIUM_THRESHOLD = 25
        const val HIGH_THRESHOLD = 60

        private val SENSITIVE = setOf(
            RiskyPermission.SMS,
            RiskyPermission.CALL_LOG,
            RiskyPermission.ACCESSIBILITY,
            RiskyPermission.DEVICE_ADMIN,
            RiskyPermission.NOTIFICATION_ACCESS,
        )

        /** Maps risk points to a level. */
        fun levelForPoints(points: Int): RiskLevel = when {
            points >= HIGH_THRESHOLD -> RiskLevel.HIGH
            points >= MEDIUM_THRESHOLD -> RiskLevel.MEDIUM
            points >= LOW_THRESHOLD -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }
    }
}
