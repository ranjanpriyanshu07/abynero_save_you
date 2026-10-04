package com.phoneguard.domain.model

/** Encryption used by a Wi-Fi network. */
enum class WifiSecurity {
    OPEN,
    WEP,
    WPA_PERSONAL,
    WPA3,
    ENTERPRISE,
    ENHANCED_OPEN,
    UNKNOWN;

    /** True for networks where others nearby can read or tamper with traffic. */
    val isUnsafe: Boolean get() = this == OPEN || this == WEP
}

/**
 * Wi-Fi safety check result.
 *
 * @property ssid network name, or null when Android hides it from us.
 * @property nearbyOpenNetworks number of open networks in range, or null if unknown.
 * @property locationPermissionNeeded true on Android 11 and below when we need the
 * location permission to read the network's security type.
 */
data class WifiCheckResult(
    val isWifiConnected: Boolean,
    val ssid: String?,
    val security: WifiSecurity,
    val nearbyOpenNetworks: Int? = null,
    val locationPermissionNeeded: Boolean = false,
) {
    /** Risk of the current connection. */
    val level: RiskLevel
        get() = when {
            !isWifiConnected -> RiskLevel.SAFE
            security.isUnsafe -> RiskLevel.HIGH
            security == WifiSecurity.UNKNOWN -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }

    companion object {
        /** Result used when the phone is not on Wi-Fi. */
        val NotConnected = WifiCheckResult(isWifiConnected = false, ssid = null, security = WifiSecurity.UNKNOWN)
    }
}
