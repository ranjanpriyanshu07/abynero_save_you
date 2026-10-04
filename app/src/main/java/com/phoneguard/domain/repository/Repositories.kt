package com.phoneguard.domain.repository

import com.phoneguard.domain.model.AppRiskReport
import com.phoneguard.domain.model.LinkStats
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.ScanHistoryEntry
import com.phoneguard.domain.model.ScanOutcome
import com.phoneguard.domain.model.ScanResult
import com.phoneguard.domain.model.ScanState
import com.phoneguard.domain.model.ScanTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Runs scans and holds the latest result so every screen sees the same data.
 */
interface ScanRepository {
    /** Live scan state: progress while scanning, then the result. */
    val state: StateFlow<ScanState>

    /** Starts a manual scan in the background. Does nothing if one is already running. */
    fun startScan()

    /** Runs a full scan, saves it to history and returns it with any newly found risks. */
    suspend fun runFullScan(trigger: ScanTrigger): ScanOutcome

    /** Report for one app, from the last scan or a fresh single-app scan. Null if not installed. */
    suspend fun findApp(packageName: String): AppRiskReport?

    /** Re-checks everything quietly after the user fixed something in Settings. */
    suspend fun recheckAfterFix()

    /**
     * Scans one newly installed app. Returns the report only when it is risky and the user
     * has not been told about it yet.
     */
    suspend fun checkNewApp(packageName: String): AppRiskReport?
}

/** Stores scan history and link-check counts on the device. */
interface HistoryRepository {
    /** Most recent scans first. */
    fun observeScans(limit: Int = 60): Flow<List<ScanHistoryEntry>>

    /** The latest saved scan, or null. */
    fun observeLatestScan(): Flow<ScanHistoryEntry?>

    /** Saves a scan and trims old entries. */
    suspend fun saveScan(result: ScanResult, trigger: ScanTrigger)

    /** Deletes all saved scans. */
    suspend fun clearScans()

    /** Records that a link was checked. Only the host name is stored. */
    suspend fun recordLinkCheck(host: String?, level: RiskLevel)

    /** Link statistics since [sinceMillis], updating live. */
    fun observeLinkStats(sinceMillis: Long): Flow<LinkStats>

    /** Link statistics since [sinceMillis]. */
    suspend fun linkStatsSince(sinceMillis: Long): LinkStats
}

/** Small user settings kept in DataStore. */
interface PreferencesRepository {
    /** Whether onboarding has been completed. */
    val onboardingCompleted: Flow<Boolean>

    /** Marks onboarding as done. */
    suspend fun setOnboardingCompleted()

    /** Risk keys the user has already been told about. */
    suspend fun knownRiskKeys(): Set<String>

    /** Replaces the known risk keys. */
    suspend fun setKnownRiskKeys(keys: Set<String>)
}
