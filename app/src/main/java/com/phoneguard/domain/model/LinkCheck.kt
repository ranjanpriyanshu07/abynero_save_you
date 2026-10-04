package com.phoneguard.domain.model

/** Warning signs found in a link without contacting any server. */
enum class UrlRedFlag(val level: RiskLevel) {
    INVALID_URL(RiskLevel.MEDIUM),
    IP_ADDRESS_HOST(RiskLevel.HIGH),
    LOOKALIKE_DOMAIN(RiskLevel.HIGH),
    USERINFO_TRICK(RiskLevel.HIGH),
    URL_SHORTENER(RiskLevel.MEDIUM),
    NOT_HTTPS(RiskLevel.MEDIUM),
    PUNYCODE_DOMAIN(RiskLevel.MEDIUM),
    UPI_PAYMENT_LINK(RiskLevel.LOW),
    UNSUPPORTED_SCHEME(RiskLevel.LOW),
    SUSPICIOUS_TLD(RiskLevel.LOW),
    MANY_SUBDOMAINS(RiskLevel.LOW),
    UNUSUAL_PORT(RiskLevel.LOW),
    SCAM_KEYWORDS(RiskLevel.LOW),
}

/**
 * On-device analysis of a single link.
 *
 * @property normalizedUrl the link with a scheme added if it was missing; null if unreadable.
 * @property isWebUrl true for http/https links, which can be checked with Safe Browsing.
 * @property impersonatedBrand display name of the bank or service this link pretends to be.
 * @property upiPayee for `upi://` links, who the payment goes to.
 */
data class UrlAnalysis(
    val input: String,
    val normalizedUrl: String?,
    val host: String?,
    val isWebUrl: Boolean,
    val flags: List<UrlRedFlag>,
    val level: RiskLevel,
    val impersonatedBrand: String? = null,
    val upiPayee: String? = null,
)

/** Outcome of a Google Safe Browsing lookup. */
enum class SafeBrowsingStatus {
    NO_THREAT,
    THREAT_FOUND,
    NOT_CONFIGURED,
    OFFLINE,
    ERROR,
    SKIPPED,
}

/** Kinds of threats reported by Safe Browsing. */
enum class ThreatType {
    MALWARE,
    SOCIAL_ENGINEERING,
    UNWANTED_SOFTWARE,
    POTENTIALLY_HARMFUL_APPLICATION,
    OTHER,
}

/** Safe Browsing lookup result. */
data class SafeBrowsingResult(
    val status: SafeBrowsingStatus,
    val threats: List<ThreatType> = emptyList(),
)

/** Final verdict for one link, combining on-device checks and Safe Browsing. */
data class LinkCheckResult(
    val analysis: UrlAnalysis,
    val safeBrowsing: SafeBrowsingResult,
    val level: RiskLevel,
)

/** Common scam patterns found in the text of a message. */
enum class ScamSignal {
    FAKE_KYC,
    DIGITAL_ARREST,
    UPI_COLLECT,
    FAKE_DELIVERY,
    PRIZE_OR_REFUND,
    OTP_REQUEST,
    ELECTRICITY_BILL,
    REMOTE_ACCESS,
    URGENT_THREAT,
}

/** Result of checking a pasted or shared message (or a single link). */
data class MessageCheckResult(
    val links: List<LinkCheckResult>,
    val signals: Set<ScamSignal>,
    val level: RiskLevel,
)

/** How many links were checked recently and how many were dangerous. */
data class LinkStats(
    val checked: Int = 0,
    val dangerous: Int = 0,
)
