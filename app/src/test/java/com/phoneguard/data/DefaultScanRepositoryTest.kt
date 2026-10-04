package com.phoneguard.data

import com.phoneguard.data.repository.DefaultScanRepository
import com.phoneguard.domain.model.AppRiskReport
import com.phoneguard.domain.model.AppScanInput
import com.phoneguard.domain.model.CheckStatus
import com.phoneguard.domain.model.DeviceCheckResult
import com.phoneguard.domain.model.DeviceCheckType
import com.phoneguard.domain.model.LinkStats
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.ScanHistoryEntry
import com.phoneguard.domain.model.ScanResult
import com.phoneguard.domain.model.ScanTrigger
import com.phoneguard.domain.model.WifiCheckResult
import com.phoneguard.domain.model.WifiSecurity
import com.phoneguard.domain.repository.HistoryRepository
import com.phoneguard.domain.repository.PreferencesRepository
import com.phoneguard.domain.scoring.AppRiskScorer
import com.phoneguard.domain.scoring.InstallSources
import com.phoneguard.domain.scoring.SecurityScoreCalculator
import com.phoneguard.scanner.AppScanner
import com.phoneguard.scanner.DeviceSettingsScanner
import com.phoneguard.scanner.WifiScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DefaultScanRepositoryTest {

    private val scorer = AppRiskScorer()

    private fun report(pkg: String, installer: String?, granted: Set<String> = emptySet()) = AppRiskReport(
        packageName = pkg,
        appName = pkg,
        versionName = "1",
        installerPackage = installer,
        firstInstallTime = 0L,
        assessment = scorer.assess(AppScanInput(pkg, granted, granted, installer)),
    )

    private class FakeApps(var apps: List<AppRiskReport>) : AppScanner {
        override suspend fun scanInstalledApps(onProgress: (Int, Int) -> Unit): List<AppRiskReport> {
            apps.forEachIndexed { i, _ -> onProgress(i + 1, apps.size) }
            return apps
        }
        override suspend fun scanApp(packageName: String) = apps.firstOrNull { it.packageName == packageName }
    }

    private class FakeDevice(var checks: List<DeviceCheckResult>) : DeviceSettingsScanner {
        override suspend fun scan() = checks
    }

    private class FakeWifi(var result: WifiCheckResult) : WifiScanner {
        override suspend fun scan() = result
    }

    private class FakeHistory : HistoryRepository {
        val saved = mutableListOf<Pair<ScanResult, ScanTrigger>>()
        override fun observeScans(limit: Int): Flow<List<ScanHistoryEntry>> = flowOf(emptyList())
        override fun observeLatestScan(): Flow<ScanHistoryEntry?> = flowOf(null)
        override suspend fun saveScan(result: ScanResult, trigger: ScanTrigger) { saved += result to trigger }
        override suspend fun clearScans() = Unit
        override suspend fun recordLinkCheck(host: String?, level: RiskLevel) = Unit
        override fun observeLinkStats(sinceMillis: Long): Flow<LinkStats> = flowOf(LinkStats())
        override suspend fun linkStatsSince(sinceMillis: Long) = LinkStats(checked = 3, dangerous = 1)
    }

    private class FakePrefs : PreferencesRepository {
        var known = emptySet<String>()
        override val onboardingCompleted: Flow<Boolean> = MutableStateFlow(true)
        override suspend fun setOnboardingCompleted() = Unit
        override suspend fun knownRiskKeys() = known
        override suspend fun setKnownRiskKeys(keys: Set<String>) { known = keys }
    }

    private val clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC)
    private val safeApp = report("com.safe", InstallSources.PLAY_STORE)
    private val trojan = report("com.fake.bank", installer = null, granted = setOf("android.permission.READ_SMS"))
    private val usbOn = DeviceCheckResult(DeviceCheckType.USB_DEBUGGING, CheckStatus.ISSUE, RiskLevel.MEDIUM)

    private fun repo(
        apps: FakeApps,
        device: FakeDevice = FakeDevice(listOf(usbOn)),
        history: FakeHistory = FakeHistory(),
        prefs: FakePrefs = FakePrefs(),
    ) = DefaultScanRepository(
        appScanner = apps,
        deviceScanner = device,
        wifiScanner = FakeWifi(WifiCheckResult(true, "Home", WifiSecurity.WPA_PERSONAL)),
        calculator = SecurityScoreCalculator(),
        historyRepository = history,
        preferences = prefs,
        clock = clock,
        appScope = TestScope(),
    )

    @Test
    fun `full scan saves history and reports only new risks`() = runTest {
        val apps = FakeApps(listOf(safeApp, trojan))
        val history = FakeHistory()
        val prefs = FakePrefs()
        val repository = repo(apps, history = history, prefs = prefs)

        val first = repository.runFullScan(ScanTrigger.SCHEDULED)
        assertEquals(setOf("app:com.fake.bank", "device:USB_DEBUGGING"), first.newRiskKeys)
        assertEquals(1, history.saved.size)
        assertEquals(ScanTrigger.SCHEDULED, history.saved.single().second)
        assertEquals(1, first.result.linkStats.dangerous)
        assertFalse(repository.state.value.isScanning)
        assertEquals(first.result, repository.state.value.result)

        // Same findings again: nothing new to notify about.
        val second = repository.runFullScan(ScanTrigger.SCHEDULED)
        assertTrue(second.newRiskKeys.isEmpty())

        // User fixed USB debugging, then a new risky app appears.
        val anyDesk = report("com.anydesk.anydeskandroid", InstallSources.PLAY_STORE)
        apps.apps = listOf(safeApp, trojan, anyDesk)
        val third = repo(apps, device = FakeDevice(emptyList()), prefs = prefs).runFullScan(ScanTrigger.SCHEDULED)
        assertEquals(setOf("app:com.anydesk.anydeskandroid"), third.newRiskKeys)
        assertEquals(setOf("app:com.fake.bank", "app:com.anydesk.anydeskandroid"), prefs.known)
    }

    @Test
    fun `new app check notifies once for risky apps only`() = runTest {
        val apps = FakeApps(listOf(safeApp, trojan))
        val prefs = FakePrefs()
        val repository = repo(apps, prefs = prefs)

        assertNull(repository.checkNewApp("com.safe"))
        assertEquals("com.fake.bank", repository.checkNewApp("com.fake.bank")?.packageName)
        assertNull("second time is not new", repository.checkNewApp("com.fake.bank"))
        assertNull("uninstalled app", repository.checkNewApp("com.gone"))
    }

    @Test
    fun `find app falls back to a single app scan`() = runTest {
        val repository = repo(FakeApps(listOf(safeApp)))
        assertEquals("com.safe", repository.findApp("com.safe")?.packageName)
        assertNull(repository.findApp("com.missing"))
    }

    @Test
    fun `scanner failure leaves state not scanning`() = runTest {
        val failing = object : AppScanner {
            override suspend fun scanInstalledApps(onProgress: (Int, Int) -> Unit): List<AppRiskReport> = error("boom")
            override suspend fun scanApp(packageName: String): AppRiskReport? = null
        }
        val repository = DefaultScanRepository(
            failing, FakeDevice(emptyList()), FakeWifi(WifiCheckResult.NotConnected), SecurityScoreCalculator(),
            FakeHistory(), FakePrefs(), clock, TestScope(),
        )
        val error = runCatching { repository.runFullScan(ScanTrigger.SCHEDULED) }.exceptionOrNull()
        assertEquals("boom", error?.message)
        assertFalse(repository.state.value.isScanning)
    }
}
