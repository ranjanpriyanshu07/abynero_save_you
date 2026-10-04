package com.phoneguard.domain.device

import com.phoneguard.domain.model.CheckStatus
import com.phoneguard.domain.model.DeviceCheckType
import com.phoneguard.domain.model.DeviceSnapshot
import com.phoneguard.domain.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DeviceSettingsEvaluatorTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
    private val evaluator = DeviceSettingsEvaluator(clock)

    private val safeSnapshot = DeviceSnapshot(
        isDeviceSecure = true,
        developerOptionsEnabled = false,
        usbDebuggingEnabled = false,
        unknownSourceApps = emptyList(),
        unknownSourcesConfirmed = false,
        securityPatch = "2026-09-05",
    )

    @Test
    fun `safe device passes everything except the play protect hint`() {
        val results = evaluator.evaluate(safeSnapshot)
        assertEquals(DeviceCheckType.entries.toSet(), results.map { it.type }.toSet())
        results.filter { it.type != DeviceCheckType.PLAY_PROTECT }.forEach {
            assertEquals(it.type.name, CheckStatus.PASSED, it.status)
        }
        assertEquals(CheckStatus.INFO, results.first { it.type == DeviceCheckType.PLAY_PROTECT }.status)
    }

    @Test
    fun `unsafe settings are reported with severities`() {
        val results = evaluator.evaluate(
            safeSnapshot.copy(
                isDeviceSecure = false,
                developerOptionsEnabled = true,
                usbDebuggingEnabled = true,
                unknownSourceApps = listOf("Files"),
                unknownSourcesConfirmed = true,
            ),
        ).associateBy { it.type }
        assertEquals(RiskLevel.HIGH, results.getValue(DeviceCheckType.SCREEN_LOCK).level)
        assertEquals(RiskLevel.MEDIUM, results.getValue(DeviceCheckType.USB_DEBUGGING).level)
        assertEquals(RiskLevel.LOW, results.getValue(DeviceCheckType.DEVELOPER_OPTIONS).level)
        assertEquals(RiskLevel.MEDIUM, results.getValue(DeviceCheckType.UNKNOWN_SOURCES).level)
        assertEquals(listOf("Files"), results.getValue(DeviceCheckType.UNKNOWN_SOURCES).affectedApps)
    }

    @Test
    fun `unconfirmed unknown sources are low`() {
        val result = evaluator.evaluate(safeSnapshot.copy(unknownSourceApps = listOf("Browser")))
            .first { it.type == DeviceCheckType.UNKNOWN_SOURCES }
        assertEquals(RiskLevel.LOW, result.level)
    }

    @Test
    fun `security patch age thresholds`() {
        fun level(patch: String) = evaluator.securityPatch(patch).level
        assertEquals(RiskLevel.SAFE, level("2026-08-01"))
        assertEquals(RiskLevel.LOW, level("2026-05-01"))
        assertEquals(RiskLevel.MEDIUM, level("2026-01-01"))
        assertEquals(RiskLevel.HIGH, level("2024-01-01"))
        assertEquals(61L, evaluator.securityPatch("2026-08-01").patchAgeDays)
    }

    @Test
    fun `unreadable patch is info only`() {
        assertEquals(CheckStatus.INFO, evaluator.securityPatch(null).status)
        assertEquals(CheckStatus.INFO, evaluator.securityPatch("unknown").status)
    }
}
