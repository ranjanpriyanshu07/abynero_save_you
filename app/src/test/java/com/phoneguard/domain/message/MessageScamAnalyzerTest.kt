package com.phoneguard.domain.message

import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.ScamSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageScamAnalyzerTest {

    private val analyzer = MessageScamAnalyzer()

    @Test
    fun `extracts links with and without scheme`() {
        val text = "Dear customer, your KYC expired. Update at https://sbi-kyc.xyz/u?id=1. " +
            "Or visit www.fake-bank.in now, or bit.ly/abc123."
        assertEquals(
            listOf("https://sbi-kyc.xyz/u?id=1", "www.fake-bank.in", "bit.ly/abc123"),
            analyzer.extractUrls(text),
        )
    }

    @Test
    fun `extracts upi and ip links and removes duplicates`() {
        val text = "Pay here upi://pay?pa=x@ybl and http://10.0.0.1/login and HTTP://10.0.0.1/login"
        assertEquals(listOf("upi://pay?pa=x@ybl", "http://10.0.0.1/login"), analyzer.extractUrls(text))
    }

    @Test
    fun `does not treat normal sentences as links`() {
        assertTrue(analyzer.extractUrls("Meet at 5.30 pm. Rs.500 paid. Thanks!").isEmpty())
    }

    @Test
    fun `detects fake kyc with urgency as high`() {
        val signals = analyzer.detectSignals("Your account will be blocked today itself. Complete KYC now.")
        assertTrue(ScamSignal.FAKE_KYC in signals)
        assertTrue(ScamSignal.URGENT_THREAT in signals)
        assertEquals(RiskLevel.HIGH, analyzer.levelForSignals(signals))
    }

    @Test
    fun `detects digital arrest and remote access as always high`() {
        assertEquals(
            RiskLevel.HIGH,
            analyzer.levelForSignals(analyzer.detectSignals("This is CBI. You are under digital arrest.")),
        )
        assertEquals(
            RiskLevel.HIGH,
            analyzer.levelForSignals(analyzer.detectSignals("Please install AnyDesk so I can help you")),
        )
    }

    @Test
    fun `detects upi collect and otp requests`() {
        assertTrue(ScamSignal.UPI_COLLECT in analyzer.detectSignals("Enter your UPI PIN to receive Rs 5000"))
        assertTrue(ScamSignal.OTP_REQUEST in analyzer.detectSignals("Sir please share the OTP you got"))
    }

    @Test
    fun `normal otp message is not a scam`() {
        val signals = analyzer.detectSignals("123456 is your OTP for login. Do not share it with anyone.")
        assertTrue(signals.isEmpty())
        assertEquals(RiskLevel.SAFE, analyzer.levelForSignals(signals))
    }

    @Test
    fun `word boundaries avoid false matches`() {
        // "training" contains "trai", "straight" contains "trai" – neither should match.
        assertTrue(analyzer.detectSignals("Training starts straight after lunch").isEmpty())
    }

    @Test
    fun `single signal is medium and urgency alone is low`() {
        assertEquals(RiskLevel.MEDIUM, analyzer.levelForSignals(setOf(ScamSignal.FAKE_DELIVERY)))
        assertEquals(RiskLevel.LOW, analyzer.levelForSignals(setOf(ScamSignal.URGENT_THREAT)))
        assertEquals(
            RiskLevel.HIGH,
            analyzer.levelForSignals(setOf(ScamSignal.FAKE_DELIVERY, ScamSignal.PRIZE_OR_REFUND)),
        )
    }
}
