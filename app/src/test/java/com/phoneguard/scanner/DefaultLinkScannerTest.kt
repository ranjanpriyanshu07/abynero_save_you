package com.phoneguard.scanner

import com.phoneguard.domain.message.MessageScamAnalyzer
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.SafeBrowsingResult
import com.phoneguard.domain.model.SafeBrowsingStatus
import com.phoneguard.domain.model.ScamSignal
import com.phoneguard.domain.model.ThreatType
import com.phoneguard.domain.url.UrlRedFlagChecker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultLinkScannerTest {

    private class FakeSafeBrowsing(
        private val result: SafeBrowsingResult = SafeBrowsingResult(SafeBrowsingStatus.NO_THREAT),
        private val throws: Boolean = false,
    ) : SafeBrowsingClient {
        val lookedUp = mutableListOf<String>()
        override suspend fun lookup(url: String): SafeBrowsingResult {
            lookedUp += url
            if (throws) error("network down")
            return result
        }
    }

    @Test
    fun `safe browsing threat makes any link dangerous`() = runTest {
        val client = FakeSafeBrowsing(SafeBrowsingResult(SafeBrowsingStatus.THREAT_FOUND, listOf(ThreatType.SOCIAL_ENGINEERING)))
        val result = DefaultLinkScanner(UrlRedFlagChecker(), client).check("https://innocent-looking.com")
        assertEquals(RiskLevel.HIGH, result.level)
        assertEquals(listOf("https://innocent-looking.com"), client.lookedUp)
    }

    @Test
    fun `clean link stays safe`() = runTest {
        val result = DefaultLinkScanner(UrlRedFlagChecker(), FakeSafeBrowsing()).check("https://www.hdfcbank.com")
        assertEquals(RiskLevel.SAFE, result.level)
        assertEquals(SafeBrowsingStatus.NO_THREAT, result.safeBrowsing.status)
    }

    @Test
    fun `client errors never crash and keep on-device verdict`() = runTest {
        val result = DefaultLinkScanner(UrlRedFlagChecker(), FakeSafeBrowsing(throws = true))
            .check("https://sbi-kyc-update.com")
        assertEquals(SafeBrowsingStatus.ERROR, result.safeBrowsing.status)
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `non web links are not sent to safe browsing`() = runTest {
        val client = FakeSafeBrowsing()
        val result = DefaultLinkScanner(UrlRedFlagChecker(), client).check("upi://pay?pa=a@b")
        assertEquals(SafeBrowsingStatus.SKIPPED, result.safeBrowsing.status)
        assertTrue(client.lookedUp.isEmpty())
    }

    @Test
    fun `message checker combines links and scam phrases`() = runTest {
        val checker = MessageChecker(MessageScamAnalyzer(), DefaultLinkScanner(UrlRedFlagChecker(), FakeSafeBrowsing()))
        val result = checker.check("Your parcel is on hold. Pay Rs 25 at https://indiapost-redelivery.top now")!!
        assertEquals(1, result.links.size)
        assertTrue(ScamSignal.FAKE_DELIVERY in result.signals)
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `message checker treats a bare domain as a link`() = runTest {
        val checker = MessageChecker(MessageScamAnalyzer(), DefaultLinkScanner(UrlRedFlagChecker(), FakeSafeBrowsing()))
        val result = checker.check("example.org")!!
        assertEquals(1, result.links.size)
        assertEquals(RiskLevel.SAFE, result.level)
        assertNull(checker.check("   "))
    }
}
