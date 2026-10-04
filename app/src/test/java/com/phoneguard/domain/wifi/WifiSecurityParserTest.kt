package com.phoneguard.domain.wifi

import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.WifiCheckResult
import com.phoneguard.domain.model.WifiSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WifiSecurityParserTest {

    @Test
    fun `parses scan result capabilities`() {
        assertEquals(WifiSecurity.OPEN, WifiSecurityParser.fromCapabilities("[ESS]"))
        assertEquals(WifiSecurity.WEP, WifiSecurityParser.fromCapabilities("[WEP][ESS]"))
        assertEquals(WifiSecurity.WPA_PERSONAL, WifiSecurityParser.fromCapabilities("[WPA2-PSK-CCMP][ESS]"))
        assertEquals(WifiSecurity.WPA_PERSONAL, WifiSecurityParser.fromCapabilities("[WPA2-PSK-CCMP][RSN-PSK+SAE-CCMP]"))
        assertEquals(WifiSecurity.WPA3, WifiSecurityParser.fromCapabilities("[RSN-SAE-CCMP][ESS]"))
        assertEquals(WifiSecurity.ENTERPRISE, WifiSecurityParser.fromCapabilities("[WPA2-EAP-CCMP][ESS]"))
        assertEquals(WifiSecurity.ENHANCED_OPEN, WifiSecurityParser.fromCapabilities("[RSN-OWE-CCMP][ESS]"))
        assertEquals(WifiSecurity.UNKNOWN, WifiSecurityParser.fromCapabilities(null))
    }

    @Test
    fun `parses android 12 security types`() {
        assertEquals(WifiSecurity.OPEN, WifiSecurityParser.fromSecurityType(0))
        assertEquals(WifiSecurity.WEP, WifiSecurityParser.fromSecurityType(1))
        assertEquals(WifiSecurity.WPA_PERSONAL, WifiSecurityParser.fromSecurityType(2))
        assertEquals(WifiSecurity.WPA3, WifiSecurityParser.fromSecurityType(4))
        assertEquals(WifiSecurity.ENHANCED_OPEN, WifiSecurityParser.fromSecurityType(6))
        assertEquals(WifiSecurity.UNKNOWN, WifiSecurityParser.fromSecurityType(-1))
    }

    @Test
    fun `cleans ssid`() {
        assertEquals("Home", WifiSecurityParser.cleanSsid("\"Home\""))
        assertNull(WifiSecurityParser.cleanSsid("<unknown ssid>"))
        assertNull(WifiSecurityParser.cleanSsid(null))
    }

    @Test
    fun `open and wep networks are high risk`() {
        assertEquals(RiskLevel.HIGH, WifiCheckResult(true, "Cafe", WifiSecurity.OPEN).level)
        assertEquals(RiskLevel.HIGH, WifiCheckResult(true, "Old", WifiSecurity.WEP).level)
        assertEquals(RiskLevel.SAFE, WifiCheckResult(true, "Home", WifiSecurity.WPA3).level)
        assertEquals(RiskLevel.LOW, WifiCheckResult(true, null, WifiSecurity.UNKNOWN).level)
        assertEquals(RiskLevel.SAFE, WifiCheckResult.NotConnected.level)
    }
}
