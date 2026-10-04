package com.phoneguard.domain.scoring

import com.phoneguard.domain.model.AppScanInput
import com.phoneguard.domain.model.InstallSource
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.RiskReason
import com.phoneguard.domain.model.RiskyPermission
import com.phoneguard.domain.model.SuggestedAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRiskScorerTest {

    private val scorer = AppRiskScorer()

    private fun input(
        packageName: String = "com.example.app",
        granted: Set<String> = emptySet(),
        requested: Set<String> = granted,
        installer: String? = InstallSources.PLAY_STORE,
        accessibility: Boolean = false,
        deviceAdmin: Boolean = false,
        notificationAccess: Boolean = false,
    ) = AppScanInput(
        packageName = packageName,
        grantedPermissions = granted,
        requestedPermissions = requested,
        installerPackage = installer,
        hasEnabledAccessibilityService = accessibility,
        isActiveDeviceAdmin = deviceAdmin,
        hasNotificationAccess = notificationAccess,
    )

    @Test
    fun `play store app with no sensitive permissions is safe`() {
        val result = scorer.assess(input(granted = setOf("android.permission.INTERNET")))
        assertEquals(RiskLevel.SAFE, result.level)
        assertEquals(0, result.points)
        assertEquals(SuggestedAction.NO_ACTION, result.action)
        assertTrue(result.reasons.isEmpty())
        assertEquals(InstallSource.PLAY_STORE, result.installSource)
    }

    @Test
    fun `typical messaging app from play store is low risk`() {
        val result = scorer.assess(
            input(
                granted = setOf(
                    "android.permission.RECORD_AUDIO",
                    "android.permission.CAMERA",
                    "android.permission.ACCESS_FINE_LOCATION",
                ),
            ),
        )
        // (10 + 8 + 8) * 0.6 = 15.6 -> 16 points
        assertEquals(16, result.points)
        assertEquals(RiskLevel.LOW, result.level)
        assertEquals(SuggestedAction.REVIEW_PERMISSIONS, result.action)
    }

    @Test
    fun `requested but denied runtime permissions do not count`() {
        val result = scorer.assess(
            input(granted = emptySet(), requested = setOf("android.permission.READ_SMS", "android.permission.CAMERA")),
        )
        assertEquals(RiskLevel.SAFE, result.level)
        assertTrue(result.permissions.isEmpty())
    }

    @Test
    fun `overlay and install permissions count when only requested`() {
        val result = scorer.assess(
            input(requested = setOf(PermissionGroups.OVERLAY, PermissionGroups.INSTALL_PACKAGES)),
        )
        assertEquals(setOf(RiskyPermission.OVERLAY, RiskyPermission.INSTALL_PACKAGES), result.permissions)
    }

    @Test
    fun `sideloaded app without sensitive access is low and suggests uninstall if unknown`() {
        val result = scorer.assess(input(installer = null))
        assertEquals(InstallSource.UNKNOWN, result.installSource)
        assertTrue(result.isSideloaded)
        assertEquals(AppRiskScorer.SIDELOADED_POINTS, result.points)
        assertEquals(RiskLevel.LOW, result.level)
        assertEquals(SuggestedAction.UNINSTALL_IF_UNKNOWN, result.action)
        assertEquals(listOf(RiskReason.SIDELOADED), result.reasons)
    }

    @Test
    fun `app installed by a browser or file manager counts as sideloaded`() {
        val result = scorer.assess(input(installer = "com.android.chrome"))
        assertEquals(InstallSource.OTHER_APP, result.installSource)
        assertTrue(result.isSideloaded)
    }

    @Test
    fun `galaxy store and amazon appstore are trusted`() {
        assertFalse(scorer.assess(input(installer = InstallSources.GALAXY_STORE)).isSideloaded)
        assertFalse(scorer.assess(input(installer = InstallSources.AMAZON_APPSTORE)).isSideloaded)
    }

    @Test
    fun `sideloaded app reading sms is high risk and should be uninstalled`() {
        val result = scorer.assess(
            input(installer = "com.whatsapp", granted = setOf("android.permission.RECEIVE_SMS")),
        )
        assertEquals(RiskLevel.HIGH, result.level)
        assertEquals(SuggestedAction.UNINSTALL_NOW, result.action)
        assertEquals(RiskReason.SIDELOADED_SENSITIVE_ACCESS, result.reasons.first())
        assertTrue(RiskReason.READS_SMS in result.reasons)
    }

    @Test
    fun `sideloaded app with accessibility service is high risk`() {
        val result = scorer.assess(input(installer = null, accessibility = true))
        assertEquals(RiskLevel.HIGH, result.level)
        assertEquals(SuggestedAction.UNINSTALL_NOW, result.action)
    }

    @Test
    fun `remote control apps are always high even from play store`() {
        listOf(
            "com.anydesk.anydeskandroid",
            "com.teamviewer.quicksupport.market",
            "com.sand.airdroid",
        ).forEach { pkg ->
            val result = scorer.assess(input(packageName = pkg))
            assertEquals(pkg, RiskLevel.HIGH, result.level)
            assertTrue(pkg, result.isRemoteControlApp)
            assertEquals(pkg, RiskReason.REMOTE_CONTROL_APP, result.reasons.first())
            assertEquals(pkg, SuggestedAction.UNINSTALL_NOW, result.action)
        }
    }

    @Test
    fun `trusted app with accessibility enabled suggests turning it off`() {
        val result = scorer.assess(
            input(accessibility = true, requested = setOf(PermissionGroups.OVERLAY)),
        )
        // (30 + 15) * 0.6 = 27 -> Medium
        assertEquals(27, result.points)
        assertEquals(RiskLevel.MEDIUM, result.level)
        assertEquals(SuggestedAction.TURN_OFF_ACCESSIBILITY, result.action)
    }

    @Test
    fun `trusted device admin app suggests removing admin rights`() {
        val result = scorer.assess(
            input(deviceAdmin = true, granted = setOf("android.permission.READ_CALL_LOG")),
        )
        // (25 + 25) * 0.6 = 30 -> Medium
        assertEquals(RiskLevel.MEDIUM, result.level)
        assertEquals(SuggestedAction.REMOVE_DEVICE_ADMIN, result.action)
    }

    @Test
    fun `trusted app with many sensitive permissions reaches high`() {
        val result = scorer.assess(
            input(
                granted = setOf(
                    "android.permission.READ_SMS",
                    "android.permission.READ_CALL_LOG",
                    "android.permission.RECORD_AUDIO",
                    "android.permission.CAMERA",
                    "android.permission.ACCESS_FINE_LOCATION",
                ),
                requested = setOf(
                    "android.permission.READ_SMS",
                    PermissionGroups.OVERLAY,
                    PermissionGroups.INSTALL_PACKAGES,
                ),
                notificationAccess = true,
            ),
        )
        // (25+25+10+8+8+15+15+20) * 0.6 = 75.6 -> 76
        assertEquals(76, result.points)
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `reasons are ordered by permission weight`() {
        val result = scorer.assess(
            input(granted = setOf("android.permission.CAMERA", "android.permission.READ_SMS")),
        )
        assertEquals(listOf(RiskReason.READS_SMS, RiskReason.USES_CAMERA), result.reasons)
    }

    @Test
    fun `level thresholds`() {
        assertEquals(RiskLevel.SAFE, AppRiskScorer.levelForPoints(0))
        assertEquals(RiskLevel.LOW, AppRiskScorer.levelForPoints(1))
        assertEquals(RiskLevel.LOW, AppRiskScorer.levelForPoints(24))
        assertEquals(RiskLevel.MEDIUM, AppRiskScorer.levelForPoints(25))
        assertEquals(RiskLevel.MEDIUM, AppRiskScorer.levelForPoints(59))
        assertEquals(RiskLevel.HIGH, AppRiskScorer.levelForPoints(60))
    }
}
