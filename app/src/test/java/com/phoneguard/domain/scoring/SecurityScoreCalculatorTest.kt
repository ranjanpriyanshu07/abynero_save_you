package com.phoneguard.domain.scoring

import com.phoneguard.domain.model.AppRiskAssessment
import com.phoneguard.domain.model.AppRiskReport
import com.phoneguard.domain.model.CheckStatus
import com.phoneguard.domain.model.DeviceCheckResult
import com.phoneguard.domain.model.DeviceCheckType
import com.phoneguard.domain.model.InstallSource
import com.phoneguard.domain.model.LinkStats
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.ScanResult
import com.phoneguard.domain.model.ScoreCategory
import com.phoneguard.domain.model.ScoreGrade
import com.phoneguard.domain.model.SuggestedAction
import com.phoneguard.domain.model.WifiCheckResult
import com.phoneguard.domain.model.WifiSecurity
import org.junit.Assert.assertEquals
import org.junit.Test

class SecurityScoreCalculatorTest {

    private val calculator = SecurityScoreCalculator()

    private fun app(level: RiskLevel, pkg: String = "app.$level.${counter++}") = AppRiskReport(
        packageName = pkg,
        appName = pkg,
        versionName = "1.0",
        installerPackage = null,
        firstInstallTime = 0L,
        assessment = AppRiskAssessment(
            level = level,
            points = 0,
            reasons = emptyList(),
            action = SuggestedAction.NO_ACTION,
            permissions = emptySet(),
            installSource = InstallSource.PLAY_STORE,
            isRemoteControlApp = false,
        ),
    )

    private var counter = 0

    private fun issue(type: DeviceCheckType, level: RiskLevel) = DeviceCheckResult(type, CheckStatus.ISSUE, level)
    private fun pass(type: DeviceCheckType) = DeviceCheckResult(type, CheckStatus.PASSED, RiskLevel.SAFE)

    private val secureWifi = WifiCheckResult(true, "Home", WifiSecurity.WPA_PERSONAL)
    private val openWifi = WifiCheckResult(true, "Cafe", WifiSecurity.OPEN)

    @Test
    fun `perfect phone scores 100 and is good`() {
        val score = calculator.calculate(
            apps = listOf(app(RiskLevel.SAFE), app(RiskLevel.SAFE)),
            deviceChecks = DeviceCheckType.entries.map { pass(it) },
            wifi = secureWifi,
            dangerousLinksLastWeek = 0,
        )
        assertEquals(100, score.overall)
        assertEquals(ScoreGrade.GOOD, score.grade)
        ScoreCategory.entries.forEach { assertEquals(100, score.scoreOf(it)) }
    }

    @Test
    fun `apps score penalties`() {
        assertEquals(100, calculator.appsScore(emptyList()))
        assertEquals(75, calculator.appsScore(listOf(app(RiskLevel.HIGH))))
        assertEquals(90, calculator.appsScore(listOf(app(RiskLevel.MEDIUM))))
        assertEquals(98, calculator.appsScore(listOf(app(RiskLevel.LOW))))
        // Low penalties are capped at 10 in total.
        assertEquals(90, calculator.appsScore(List(20) { app(RiskLevel.LOW) }))
        assertEquals(0, calculator.appsScore(List(5) { app(RiskLevel.HIGH) }))
    }

    @Test
    fun `device score penalties`() {
        assertEquals(60, calculator.deviceScore(listOf(issue(DeviceCheckType.SCREEN_LOCK, RiskLevel.HIGH))))
        assertEquals(80, calculator.deviceScore(listOf(issue(DeviceCheckType.USB_DEBUGGING, RiskLevel.MEDIUM))))
        assertEquals(90, calculator.deviceScore(listOf(issue(DeviceCheckType.DEVELOPER_OPTIONS, RiskLevel.LOW))))
        assertEquals(85, calculator.deviceScore(listOf(issue(DeviceCheckType.UNKNOWN_SOURCES, RiskLevel.MEDIUM))))
        assertEquals(95, calculator.deviceScore(listOf(issue(DeviceCheckType.UNKNOWN_SOURCES, RiskLevel.LOW))))
        assertEquals(75, calculator.deviceScore(listOf(issue(DeviceCheckType.SECURITY_PATCH, RiskLevel.HIGH))))
        // Info-only items never cost points.
        assertEquals(
            100,
            calculator.deviceScore(listOf(DeviceCheckResult(DeviceCheckType.PLAY_PROTECT, CheckStatus.INFO, RiskLevel.LOW))),
        )
    }

    @Test
    fun `network and links scores`() {
        assertEquals(100, calculator.networkScore(null))
        assertEquals(100, calculator.networkScore(WifiCheckResult.NotConnected))
        assertEquals(100, calculator.networkScore(secureWifi))
        assertEquals(40, calculator.networkScore(openWifi))
        assertEquals(90, calculator.networkScore(WifiCheckResult(true, null, WifiSecurity.UNKNOWN)))
        assertEquals(100, calculator.linksScore(0))
        assertEquals(70, calculator.linksScore(2))
        assertEquals(40, calculator.linksScore(10))
    }

    @Test
    fun `overall is a weighted average`() {
        val score = calculator.calculate(
            apps = listOf(app(RiskLevel.MEDIUM), app(RiskLevel.MEDIUM)), // 80
            deviceChecks = listOf(issue(DeviceCheckType.USB_DEBUGGING, RiskLevel.MEDIUM)), // 80
            wifi = openWifi, // 40
            dangerousLinksLastWeek = 0, // 100
        )
        // 80*0.40 + 80*0.35 + 40*0.15 + 100*0.10 = 32 + 28 + 6 + 10 = 76
        assertEquals(76, score.overall)
        assertEquals(ScoreGrade.FAIR, score.grade)
    }

    @Test
    fun `any high risk finding caps the score below 60`() {
        val withHighApp = calculator.calculate(listOf(app(RiskLevel.HIGH)), emptyList(), secureWifi, 0)
        // Weighted value would be 75*0.4 + 100*0.6 = 90 but is capped.
        assertEquals(SecurityScoreCalculator.CRITICAL_CAP, withHighApp.overall)
        assertEquals(ScoreGrade.AT_RISK, withHighApp.grade)

        val noScreenLock = calculator.calculate(
            emptyList(),
            listOf(issue(DeviceCheckType.SCREEN_LOCK, RiskLevel.HIGH)),
            secureWifi,
            0,
        )
        assertEquals(SecurityScoreCalculator.CRITICAL_CAP, noScreenLock.overall)
    }

    @Test
    fun `risk keys only include medium and high findings`() {
        val score = calculator.calculate(emptyList(), emptyList(), null, 0)
        val result = ScanResult(
            timestamp = 0L,
            apps = listOf(app(RiskLevel.HIGH, "bad.app"), app(RiskLevel.MEDIUM, "meh.app"), app(RiskLevel.LOW, "ok.app")),
            deviceChecks = listOf(
                issue(DeviceCheckType.USB_DEBUGGING, RiskLevel.MEDIUM),
                issue(DeviceCheckType.DEVELOPER_OPTIONS, RiskLevel.LOW),
            ),
            wifi = secureWifi,
            linkStats = LinkStats(),
            score = score,
        )
        val keys = RiskKeys.from(result)
        assertEquals(setOf("app:bad.app", "app:meh.app", "device:USB_DEBUGGING"), keys)
        assertEquals(setOf("app:bad.app"), RiskKeys.newKeys(keys, setOf("app:meh.app", "device:USB_DEBUGGING")))
        assertEquals("bad.app", RiskKeys.packageFromKey("app:bad.app"))
        assertEquals(null, RiskKeys.packageFromKey("device:USB_DEBUGGING"))
    }
}
