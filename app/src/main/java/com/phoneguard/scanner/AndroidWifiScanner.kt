package com.phoneguard.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.phoneguard.di.IoDispatcher
import com.phoneguard.domain.model.WifiCheckResult
import com.phoneguard.domain.model.WifiSecurity
import com.phoneguard.domain.wifi.WifiSecurityParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Checks whether the current Wi-Fi network is open or uses weak WEP encryption.
 *
 * On Android 12+ the security type is read directly and no location permission is needed.
 * On Android 11 and below, Android only reveals it with the location permission, so the
 * result says when that permission is needed.
 */
class AndroidWifiScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WifiScanner {

    override suspend fun scan(): WifiCheckResult = withContext(ioDispatcher) {
        try {
            readWifi()
        } catch (e: RuntimeException) {
            WifiCheckResult.NotConnected
        }
    }

    @Suppress("DEPRECATION")
    private fun readWifi(): WifiCheckResult {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
        val capabilities = connectivity?.let { findWifiCapabilities(it) } ?: return WifiCheckResult.NotConnected

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val info = (capabilities.transportInfo as? WifiInfo) ?: wifiManager?.connectionInfo
            val security = info?.let { WifiSecurityParser.fromSecurityType(it.currentSecurityType) } ?: WifiSecurity.UNKNOWN
            WifiCheckResult(
                isWifiConnected = true,
                ssid = WifiSecurityParser.cleanSsid(info?.ssid),
                security = security,
                nearbyOpenNetworks = if (hasLocationPermission()) countOpenNetworks(wifiManager) else null,
            )
        } else {
            readWifiLegacy(wifiManager)
        }
    }

    @Suppress("DEPRECATION")
    private fun readWifiLegacy(wifiManager: WifiManager?): WifiCheckResult {
        if (!hasLocationPermission()) {
            return WifiCheckResult(
                isWifiConnected = true,
                ssid = null,
                security = WifiSecurity.UNKNOWN,
                locationPermissionNeeded = true,
            )
        }
        val info = wifiManager?.connectionInfo
        val scanResults = try {
            wifiManager?.scanResults.orEmpty()
        } catch (e: SecurityException) {
            emptyList()
        }
        val ssid = WifiSecurityParser.cleanSsid(info?.ssid)
        val match = scanResults.firstOrNull { it.BSSID != null && it.BSSID.equals(info?.bssid, ignoreCase = true) }
            ?: scanResults.firstOrNull { ssid != null && it.SSID == ssid }
        return WifiCheckResult(
            isWifiConnected = true,
            ssid = ssid,
            security = match?.let { WifiSecurityParser.fromCapabilities(it.capabilities) } ?: WifiSecurity.UNKNOWN,
            nearbyOpenNetworks = countOpenNetworks(wifiManager),
        )
    }

    @Suppress("DEPRECATION")
    private fun countOpenNetworks(wifiManager: WifiManager?): Int? = try {
        wifiManager?.scanResults
            ?.filter { WifiSecurityParser.fromCapabilities(it.capabilities) == WifiSecurity.OPEN }
            ?.mapNotNull { it.SSID?.takeIf { name -> name.isNotBlank() } }
            ?.distinct()
            ?.size
    } catch (e: SecurityException) {
        null
    }

    @Suppress("DEPRECATION")
    private fun findWifiCapabilities(connectivity: ConnectivityManager): NetworkCapabilities? {
        connectivity.activeNetwork?.let { network ->
            val caps = connectivity.getNetworkCapabilities(network)
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) return caps
        }
        // The active network may be a VPN or mobile data; look for a Wi-Fi network too.
        return connectivity.allNetworks
            .mapNotNull { connectivity.getNetworkCapabilities(it) }
            .firstOrNull { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
