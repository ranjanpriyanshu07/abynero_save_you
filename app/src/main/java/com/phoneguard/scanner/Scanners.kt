package com.phoneguard.scanner

import com.phoneguard.domain.model.AppRiskReport
import com.phoneguard.domain.model.DeviceCheckResult
import com.phoneguard.domain.model.LinkCheckResult
import com.phoneguard.domain.model.SafeBrowsingResult
import com.phoneguard.domain.model.WifiCheckResult

/** Scans installed apps and scores each one. */
interface AppScanner {
    /**
     * Scans every user-installed app.
     *
     * @param onProgress called after each app with (done, total).
     * @return reports sorted from riskiest to safest. Apps that cannot be read are skipped.
     */
    suspend fun scanInstalledApps(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): List<AppRiskReport>

    /** Scans a single app, or returns null if it is not installed or cannot be read. */
    suspend fun scanApp(packageName: String): AppRiskReport?
}

/** Checks device settings such as screen lock and USB debugging. */
interface DeviceSettingsScanner {
    /** Returns one result per [com.phoneguard.domain.model.DeviceCheckType]. */
    suspend fun scan(): List<DeviceCheckResult>
}

/** Checks the current Wi-Fi connection. */
interface WifiScanner {
    /** Never throws; returns [WifiCheckResult.NotConnected] when not on Wi-Fi. */
    suspend fun scan(): WifiCheckResult
}

/** Looks up a URL with Google Safe Browsing. */
interface SafeBrowsingClient {
    /** Never throws; network problems are reported through the result status. */
    suspend fun lookup(url: String): SafeBrowsingResult
}

/** Checks one link using on-device rules and Safe Browsing. */
interface LinkScanner {
    /** Never throws. */
    suspend fun check(rawUrl: String): LinkCheckResult
}
