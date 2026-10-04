package com.phoneguard.domain.wifi

import com.phoneguard.domain.model.WifiSecurity

/**
 * Converts Android's Wi-Fi security descriptions into [WifiSecurity].
 *
 * Raw integers mirror `android.net.wifi.WifiInfo.SECURITY_TYPE_*` so this stays testable
 * without Android.
 */
object WifiSecurityParser {

    /** Parses a `ScanResult.capabilities` string such as `[WPA2-PSK-CCMP][ESS]`. */
    fun fromCapabilities(capabilities: String?): WifiSecurity {
        val caps = capabilities?.uppercase() ?: return WifiSecurity.UNKNOWN
        return when {
            "WEP" in caps -> WifiSecurity.WEP
            "SAE" in caps && "PSK" !in caps -> WifiSecurity.WPA3
            "EAP" in caps -> WifiSecurity.ENTERPRISE
            "PSK" in caps || "WPA" in caps || "WAPI" in caps -> WifiSecurity.WPA_PERSONAL
            "SAE" in caps -> WifiSecurity.WPA3
            "OWE" in caps -> WifiSecurity.ENHANCED_OPEN
            else -> WifiSecurity.OPEN
        }
    }

    /** Parses `WifiInfo.getCurrentSecurityType()` (Android 12+). */
    fun fromSecurityType(type: Int): WifiSecurity = when (type) {
        0 -> WifiSecurity.OPEN
        1 -> WifiSecurity.WEP
        2, 7, 8 -> WifiSecurity.WPA_PERSONAL
        4, 13 -> WifiSecurity.WPA3
        6 -> WifiSecurity.ENHANCED_OPEN
        3, 5, 9, 10, 11, 12 -> WifiSecurity.ENTERPRISE
        else -> WifiSecurity.UNKNOWN
    }

    /** Removes the quotes Android puts around SSIDs and hides placeholder names. */
    fun cleanSsid(raw: String?): String? {
        val trimmed = raw?.trim()?.removeSurrounding("\"") ?: return null
        return trimmed.takeUnless { it.isBlank() || it == "<unknown ssid>" || it == "0x" }
    }
}
