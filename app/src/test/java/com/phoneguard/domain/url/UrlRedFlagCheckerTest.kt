package com.phoneguard.domain.url

import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.UrlRedFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlRedFlagCheckerTest {

    private val checker = UrlRedFlagChecker()

    private fun flags(url: String) = checker.check(url).flags

    @Test
    fun `official bank links are safe`() {
        listOf(
            "https://www.onlinesbi.sbi/",
            "https://retail.onlinesbi.sbi/retail/login.htm",
            "https://www.hdfcbank.com/personal/pay",
            "https://netbanking.hdfcbank.com",
            "https://www.icicibank.com",
            "https://www.sbi.co.in",
            "https://myaadhaar.uidai.gov.in",
            "https://www.amazon.in/gp/your-account",
        ).forEach { url ->
            val result = checker.check(url)
            assertEquals(url, RiskLevel.SAFE, result.level)
            assertTrue(url, result.flags.isEmpty())
            assertTrue(url, result.isWebUrl)
        }
    }

    @Test
    fun `ordinary sites are safe`() {
        assertEquals(RiskLevel.SAFE, checker.check("https://www.wikipedia.org/wiki/India").level)
        assertEquals(RiskLevel.SAFE, checker.check("https://www.usbinfo.com").level)
        assertEquals(RiskLevel.SAFE, checker.check("https://www.taxis.com").level)
    }

    @Test
    fun `plain http is flagged`() {
        val result = checker.check("http://example.com")
        assertEquals(listOf(UrlRedFlag.NOT_HTTPS), result.flags)
        assertEquals(RiskLevel.MEDIUM, result.level)
    }

    @Test
    fun `missing scheme is not treated as http`() {
        val result = checker.check("example.com/page")
        assertFalse(UrlRedFlag.NOT_HTTPS in result.flags)
        assertEquals("http://example.com/page", result.normalizedUrl)
        assertEquals("example.com", result.host)
    }

    @Test
    fun `ip address links are dangerous`() {
        assertTrue(UrlRedFlag.IP_ADDRESS_HOST in flags("https://192.168.10.5/login"))
        assertTrue(UrlRedFlag.IP_ADDRESS_HOST in flags("http://3232235777/"))
        assertTrue(UrlRedFlag.IP_ADDRESS_HOST in flags("https://[2001:db8::1]/x"))
        assertEquals(RiskLevel.HIGH, checker.check("https://45.12.9.1/sbi").level)
    }

    @Test
    fun `brand keywords in unofficial domains are look-alikes`() {
        val sbi = checker.check("https://sbi-kyc-update.com/verify")
        assertTrue(UrlRedFlag.LOOKALIKE_DOMAIN in sbi.flags)
        assertEquals("SBI", sbi.impersonatedBrand)
        assertEquals(RiskLevel.HIGH, sbi.level)

        assertEquals("HDFC Bank", checker.check("https://hdfc-netbanking.in").impersonatedBrand)
        assertEquals("Paytm", checker.check("https://paytm-cashback.xyz").impersonatedBrand)
        assertEquals("ICICI Bank", checker.check("https://www.icicibank.com.secure-login.top").impersonatedBrand)
    }

    @Test
    fun `typo and homoglyph domains are look-alikes`() {
        assertEquals("ICICI Bank", checker.check("https://icicbank.com").impersonatedBrand)
        assertEquals("Flipkart", checker.check("https://flipkarrt.com").impersonatedBrand)
        assertEquals("Amazon", checker.check("https://amaz0n.in").impersonatedBrand)
        assertEquals("Amazon", checker.check("https://arnazon.in/deal").impersonatedBrand)
        assertTrue(UrlRedFlag.LOOKALIKE_DOMAIN in flags("https://hdfcbank.co"))
    }

    @Test
    fun `short tokens only match at the start of a domain part`() {
        assertNull(checker.findImpersonatedBrand("www.usbinfo.com"))
        assertNull(checker.findImpersonatedBrand("praxis.com"))
        assertEquals("SBI", checker.findImpersonatedBrand("mysite.sbiyono-rewards.in")?.displayName)
    }

    @Test
    fun `url shorteners are flagged`() {
        val result = checker.check("https://bit.ly/3abcd")
        assertEquals(listOf(UrlRedFlag.URL_SHORTENER), result.flags)
        assertEquals(RiskLevel.MEDIUM, result.level)
        assertTrue(UrlRedFlag.URL_SHORTENER in flags("tinyurl.com/xyz"))
    }

    @Test
    fun `userinfo trick is flagged and host is the real destination`() {
        val result = checker.check("https://www.sbi.co.in@evil-site.com/login")
        assertTrue(UrlRedFlag.USERINFO_TRICK in result.flags)
        assertEquals("evil-site.com", result.host)
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `punycode and unusual ports are flagged`() {
        assertTrue(UrlRedFlag.PUNYCODE_DOMAIN in flags("https://xn--pypal-4ve.com"))
        assertTrue(UrlRedFlag.UNUSUAL_PORT in flags("https://example.com:8443/"))
        assertFalse(UrlRedFlag.UNUSUAL_PORT in flags("https://example.com:443/"))
    }

    @Test
    fun `suspicious tld and scam keywords are low on their own`() {
        val result = checker.check("https://free-gift.xyz")
        assertTrue(UrlRedFlag.SUSPICIOUS_TLD in result.flags)
        assertTrue(UrlRedFlag.SCAM_KEYWORDS in result.flags)
        assertEquals(RiskLevel.LOW, result.level)
    }

    @Test
    fun `several warning signs raise the level`() {
        // NOT_HTTPS (medium) + suspicious TLD + scam keywords -> bumped to High.
        assertEquals(RiskLevel.HIGH, checker.check("http://reward-claim.top/kyc").level)
        // Two medium flags -> High.
        assertEquals(RiskLevel.HIGH, checker.check("http://bit.ly/abc").level)
    }

    @Test
    fun `many subdomains are flagged`() {
        assertTrue(UrlRedFlag.MANY_SUBDOMAINS in flags("https://a.b.c.d.example.com"))
    }

    @Test
    fun `upi links are recognised with payee`() {
        val result = checker.check("upi://pay?pa=shop@okaxis&pn=Ravi%20Stores&am=10")
        assertEquals(listOf(UrlRedFlag.UPI_PAYMENT_LINK), result.flags)
        assertEquals("Ravi Stores", result.upiPayee)
        assertFalse(result.isWebUrl)
    }

    @Test
    fun `garbage input is invalid and never throws`() {
        listOf("", "   ", "http://", "://", "hello", "https:// /").forEach { input ->
            val result = checker.check(input)
            assertFalse(input, result.isWebUrl && result.flags.isEmpty() && input.isBlank())
        }
        assertEquals(listOf(UrlRedFlag.INVALID_URL), checker.check("").flags)
        assertEquals(listOf(UrlRedFlag.INVALID_URL), checker.check("http://").flags)
    }

    @Test
    fun `trailing punctuation from messages is ignored`() {
        assertEquals("sbi-kyc.com", checker.check("http://sbi-kyc.com.").host)
        assertEquals("example.com", checker.check("(https://example.com)").host)
    }

    @Test
    fun `other schemes are not web urls`() {
        val result = checker.check("mailto:someone@example.com")
        assertEquals(listOf(UrlRedFlag.UNSUPPORTED_SCHEME), result.flags)
        assertFalse(result.isWebUrl)
    }

    @Test
    fun `registrable domain handles indian second level suffixes`() {
        assertEquals("sbi.co.in", checker.registrableDomain("retail.sbi.co.in"))
        assertEquals("example.com", checker.registrableDomain("a.b.example.com"))
        assertEquals("uidai.gov.in", checker.registrableDomain("myaadhaar.uidai.gov.in"))
    }

    @Test
    fun `levenshtein distance`() {
        assertEquals(0, UrlRedFlagChecker.levenshtein("bank", "bank"))
        assertEquals(1, UrlRedFlagChecker.levenshtein("icicbank", "icicibank"))
        assertEquals(2, UrlRedFlagChecker.levenshtein("hdfcbnak", "hdfcbank"))
        assertEquals(3, UrlRedFlagChecker.levenshtein("", "abc"))
    }
}
