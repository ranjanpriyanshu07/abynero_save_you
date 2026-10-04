package com.phoneguard.domain.message

import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.ScamSignal
import javax.inject.Inject

/**
 * Looks for links and common Indian scam patterns in a message the user pasted or shared.
 *
 * Runs fully on the device; the message text never leaves the phone.
 */
class MessageScamAnalyzer @Inject constructor() {

    /** Returns up to [limit] distinct links found in [text], in order of appearance. */
    fun extractUrls(text: String, limit: Int = MAX_LINKS): List<String> =
        URL_REGEX.findAll(text)
            .map { it.value.trimEnd('.', ',', '!', '?', ';', ':', ')', ']', '"', '\'') }
            .filter { it.length >= 4 }
            .distinctBy { it.lowercase() }
            .take(limit)
            .toList()

    /** Scam patterns found in [text]. */
    fun detectSignals(text: String): Set<ScamSignal> {
        val lower = text.lowercase()
        return SIGNAL_PATTERNS.filterValues { patterns -> patterns.any { it.containsMatchIn(lower) } }.keys
    }

    /** Risk level suggested by the scam patterns alone. */
    fun levelForSignals(signals: Set<ScamSignal>): RiskLevel {
        if (signals.isEmpty()) return RiskLevel.SAFE
        if (signals.any { it in ALWAYS_HIGH }) return RiskLevel.HIGH
        val strong = signals.count { it != ScamSignal.URGENT_THREAT }
        val urgent = ScamSignal.URGENT_THREAT in signals
        return when {
            strong >= 2 -> RiskLevel.HIGH
            strong == 1 && urgent -> RiskLevel.HIGH
            strong == 1 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    companion object {
        /** Maximum links we check from one message. */
        const val MAX_LINKS = 5

        private val ALWAYS_HIGH = setOf(
            ScamSignal.DIGITAL_ARREST,
            ScamSignal.UPI_COLLECT,
            ScamSignal.OTP_REQUEST,
            ScamSignal.REMOTE_ACCESS,
        )

        private const val TLDS =
            "com|in|net|org|info|co|io|me|xyz|top|club|online|site|live|ly|gl|app|link|click|shop|store|icu|" +
                "buzz|tk|ml|ga|cf|gq|ws|cc|biz|us|uk|gov|sbi|bank|page|dev|ai|cam|rest|work|vip|support|today|" +
                "tech|cyou|sbs|cfd|lol|monster|quest|at|to|id|gy|ru|cn"

        private val URL_REGEX = Regex(
            "(?:\\b[a-zA-Z][a-zA-Z0-9+.-]{1,15}://[^\\s<>\"']+)" +
                "|(?:\\bupi:[^\\s<>\"']+)" +
                "|(?:\\bwww\\.[^\\s<>\"']+)" +
                "|(?:\\b\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d+)?(?:/[^\\s<>\"']*)?)" +
                "|(?:\\b(?:[a-zA-Z0-9-]+\\.)+(?:$TLDS)(?:\\.[a-zA-Z]{2})?\\b(?:/[^\\s<>\"']*)?)",
            RegexOption.IGNORE_CASE,
        )

        private fun words(vararg phrases: String): List<Regex> =
            phrases.map { Regex("(?<![a-z0-9])" + Regex.escape(it) + "(?![a-z0-9])") }

        private val SIGNAL_PATTERNS: Map<ScamSignal, List<Regex>> = mapOf(
            ScamSignal.FAKE_KYC to words(
                "kyc", "re-kyc", "pan card update", "pan update", "update your pan", "aadhaar update",
                "account will be blocked", "account will be suspended", "account has been blocked",
                "kyc expired", "kyc pending",
            ),
            ScamSignal.DIGITAL_ARREST to words(
                "digital arrest", "cbi", "narcotics", "money laundering", "arrest warrant", "police case",
                "fir registered", "cyber cell", "trai", "illegal parcel", "drugs found", "customs department",
                "ed officer",
            ),
            ScamSignal.UPI_COLLECT to words(
                "collect request", "approve the request", "enter upi pin to receive", "enter your upi pin",
                "pin to receive", "scan to receive", "accept the payment request",
            ),
            ScamSignal.FAKE_DELIVERY to words(
                "delivery failed", "package is on hold", "parcel is on hold", "address incomplete",
                "incomplete address", "update your address", "redelivery", "re-delivery", "courier", "india post",
                "shipment on hold",
            ),
            ScamSignal.PRIZE_OR_REFUND to words(
                "you have won", "you won", "lottery", "prize", "cashback", "refund", "reward points",
                "lucky draw", "kbc", "claim your reward", "gift card",
            ),
            ScamSignal.OTP_REQUEST to words(
                "share otp", "share the otp", "send otp", "send the otp", "tell otp", "tell the otp",
                "otp batao", "forward the otp", "share the code", "otp bhejo",
            ),
            ScamSignal.ELECTRICITY_BILL to words(
                "electricity bill", "electricity connection", "power will be disconnected", "bijli",
                "light bill", "electricity will be disconnected",
            ),
            ScamSignal.REMOTE_ACCESS to words(
                "anydesk", "teamviewer", "quicksupport", "airdroid", "rustdesk", "screen share",
                "screen sharing", "share your screen",
            ),
            ScamSignal.URGENT_THREAT to words(
                "immediately", "within 24 hours", "today itself", "will be blocked", "will be suspended",
                "will be disconnected", "last warning", "urgent", "final notice", "turant", "band ho jayega",
                "tonight",
            ),
        )
    }
}
