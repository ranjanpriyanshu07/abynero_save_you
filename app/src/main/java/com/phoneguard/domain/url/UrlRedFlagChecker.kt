package com.phoneguard.domain.url

import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.UrlAnalysis
import com.phoneguard.domain.model.UrlRedFlag
import javax.inject.Inject

/**
 * Finds common warning signs in a link, fully on the device.
 *
 * Checks: IP-address links, look-alike bank and payment domains, URL shorteners,
 * non-HTTPS links, punycode (lookalike letters), "user@host" tricks, odd ports,
 * cheap top-level domains, very long subdomain chains and scam words. `upi://`
 * links are recognised as payment requests.
 */
class UrlRedFlagChecker @Inject constructor() {

    /** Analyses [raw]. Never throws, even for garbage input. */
    fun check(raw: String): UrlAnalysis {
        val cleaned = clean(raw)
        if (cleaned.isEmpty()) return invalid(raw)

        val hasScheme = SCHEME_REGEX.containsMatchIn(cleaned)
        val candidate = if (hasScheme) cleaned else "http://$cleaned"
        val match = URL_REGEX.matchEntire(candidate) ?: return invalid(raw)

        val scheme = match.groupValues[1].lowercase()
        val authority = match.groupValues[2]
        val rest = match.groupValues[3]

        if (scheme == "upi") return analyseUpi(raw, candidate)
        if (scheme != "http" && scheme != "https") {
            return UrlAnalysis(
                input = raw,
                normalizedUrl = candidate,
                host = null,
                isWebUrl = false,
                flags = listOf(UrlRedFlag.UNSUPPORTED_SCHEME),
                level = UrlRedFlag.UNSUPPORTED_SCHEME.level,
            )
        }

        val userInfo = authority.substringBeforeLast('@', missingDelimiterValue = "")
        val hostPort = authority.substringAfterLast('@')
        val (host, port) = splitHostPort(hostPort)
        if (host.isEmpty() || !HOST_CHARS_REGEX.matches(host)) return invalid(raw)

        val flags = linkedSetOf<UrlRedFlag>()
        if (hasScheme && scheme == "http") flags += UrlRedFlag.NOT_HTTPS
        if (userInfo.isNotEmpty()) flags += UrlRedFlag.USERINFO_TRICK
        if (port != null && port != 80 && port != 443) flags += UrlRedFlag.UNUSUAL_PORT

        val isIp = isIpAddress(host)
        if (isIp) flags += UrlRedFlag.IP_ADDRESS_HOST

        var brand: Brand? = null
        if (!isIp) {
            val registrable = registrableDomain(host)
            val isOfficial = isOfficialDomain(host)
            if (registrable in KnownDomains.urlShorteners || host in KnownDomains.urlShorteners) {
                flags += UrlRedFlag.URL_SHORTENER
            }
            if (host.split('.').any { it.startsWith("xn--") } || host.any { it.code > 127 }) {
                flags += UrlRedFlag.PUNYCODE_DOMAIN
            }
            val tld = host.substringAfterLast('.')
            if (tld in KnownDomains.suspiciousTlds) flags += UrlRedFlag.SUSPICIOUS_TLD
            if (host.count { it == '.' } >= MAX_DOTS) flags += UrlRedFlag.MANY_SUBDOMAINS
            if (!isOfficial) {
                brand = findImpersonatedBrand(host)
                if (brand != null) flags += UrlRedFlag.LOOKALIKE_DOMAIN
                val lowerRest = rest.lowercase()
                if (KnownDomains.scamKeywords.any { host.contains(it) || lowerRest.contains(it) }) {
                    flags += UrlRedFlag.SCAM_KEYWORDS
                }
            }
        }

        val flagList = flags.toList()
        return UrlAnalysis(
            input = raw,
            normalizedUrl = candidate,
            host = host,
            isWebUrl = true,
            flags = flagList,
            level = combinedLevel(flagList),
            impersonatedBrand = brand?.displayName,
        )
    }

    /** True when [host] is, or is a subdomain of, a known official domain. */
    fun isOfficialDomain(host: String): Boolean = KnownDomains.brands.any { brand ->
        brand.officialDomains.any { host == it || host.endsWith(".$it") }
    }

    /** Returns the brand [host] seems to copy, or null. Official domains return null. */
    fun findImpersonatedBrand(host: String): Brand? {
        val normalizedHost = normalizeHomoglyphs(host)
        val tokens = normalizedHost.split('.', '-').filter { it.isNotEmpty() }
        val registrableLabel = normalizeHomoglyphs(registrableDomain(host).substringBefore('.'))

        for (brand in KnownDomains.brands) {
            if (brand.substringKeywords.any { normalizedHost.contains(it) }) return brand
            if (brand.tokenKeywords.any { kw -> tokens.any { it == kw || it.startsWith(kw) } }) return brand
            val officialLabels = brand.officialDomains.map { registrableDomain(it).substringBefore('.') }
            if (registrableLabel.length >= MIN_LOOKALIKE_LENGTH) {
                val close = officialLabels.any { official ->
                    official.length >= MIN_LOOKALIKE_LENGTH &&
                        levenshtein(registrableLabel, official) <= allowedDistance(official)
                }
                if (close) return brand
            }
        }
        return null
    }

    /** Last two labels of [host], or three for suffixes like `co.in`. */
    fun registrableDomain(host: String): String {
        val labels = host.lowercase().trimEnd('.').split('.')
        if (labels.size <= 2) return labels.joinToString(".")
        val lastTwo = labels.takeLast(2).joinToString(".")
        val lastThree = labels.takeLast(3).joinToString(".")
        return when {
            labels.size >= 4 && lastThree in KnownDomains.secondLevelSuffixes -> labels.takeLast(4).joinToString(".")
            lastTwo in KnownDomains.secondLevelSuffixes -> lastThree
            else -> lastTwo
        }
    }

    private fun analyseUpi(raw: String, candidate: String): UrlAnalysis {
        val query = candidate.substringAfter('?', "")
        val params = query.split('&').mapNotNull { part ->
            val key = part.substringBefore('=', "").lowercase()
            val value = part.substringAfter('=', "")
            if (key.isEmpty()) null else key to decode(value)
        }.toMap()
        val payee = params["pn"]?.takeIf { it.isNotBlank() } ?: params["pa"]
        return UrlAnalysis(
            input = raw,
            normalizedUrl = candidate,
            host = null,
            isWebUrl = false,
            flags = listOf(UrlRedFlag.UPI_PAYMENT_LINK),
            level = UrlRedFlag.UPI_PAYMENT_LINK.level,
            upiPayee = payee,
        )
    }

    private fun invalid(raw: String) = UrlAnalysis(
        input = raw,
        normalizedUrl = null,
        host = null,
        isWebUrl = false,
        flags = listOf(UrlRedFlag.INVALID_URL),
        level = UrlRedFlag.INVALID_URL.level,
    )

    private fun clean(raw: String): String {
        val firstToken = raw.trim().split(WHITESPACE_REGEX).firstOrNull().orEmpty()
        return firstToken.trim('<', '>', '"', '\'', '(', ')', '[', ']').trimEnd('.', ',', '!', '?', ';', ':')
    }

    private fun splitHostPort(hostPort: String): Pair<String, Int?> {
        if (hostPort.startsWith("[")) {
            val end = hostPort.indexOf(']')
            if (end < 0) return "" to null
            val host = hostPort.substring(0, end + 1).lowercase()
            val port = hostPort.substring(end + 1).removePrefix(":").toIntOrNull()
            return host to port
        }
        val host = hostPort.substringBefore(':').lowercase().trimEnd('.')
        val port = hostPort.substringAfter(':', "").takeIf { it.isNotEmpty() }?.toIntOrNull()
        return host to port
    }

    private fun isIpAddress(host: String): Boolean =
        host.startsWith("[") || IPV4_REGEX.matches(host) || host.all { it.isDigit() } ||
            HEX_IP_REGEX.matches(host)

    private fun combinedLevel(flags: List<UrlRedFlag>): RiskLevel {
        if (flags.isEmpty()) return RiskLevel.SAFE
        val max = RiskLevel.max(flags.map { it.level })
        val serious = flags.count { it.level >= RiskLevel.MEDIUM }
        return when {
            max == RiskLevel.MEDIUM && serious >= 2 -> RiskLevel.HIGH
            max < RiskLevel.HIGH && flags.size >= 3 -> max.bump()
            else -> max
        }
    }

    private fun decode(value: String): String = buildString {
        var i = 0
        while (i < value.length) {
            val c = value[i]
            when {
                c == '+' -> append(' ')
                c == '%' && i + 2 < value.length -> {
                    val code = value.substring(i + 1, i + 3).toIntOrNull(16)
                    if (code != null) {
                        append(code.toChar())
                        i += 2
                    } else {
                        append(c)
                    }
                }
                else -> append(c)
            }
            i++
        }
    }

    companion object {
        private const val MAX_DOTS = 4
        private const val MIN_LOOKALIKE_LENGTH = 6

        private val SCHEME_REGEX = Regex(
            "^([a-zA-Z][a-zA-Z0-9+.-]*://|(upi|mailto|tel|sms|intent|javascript|data|market):)",
            RegexOption.IGNORE_CASE,
        )
        private val URL_REGEX = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):/{0,3}([^/?#]*)(.*)$", RegexOption.DOT_MATCHES_ALL)
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val IPV4_REGEX = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")
        private val HEX_IP_REGEX = Regex("^0x[0-9a-f]+$")
        private val HOST_CHARS_REGEX = Regex("^[^\\s/\\\\]+$")

        private fun allowedDistance(official: String): Int = if (official.length >= 8) 2 else 1

        /** Replaces characters scammers swap in to fool the eye (0→o, 1→l, rn→m …). */
        fun normalizeHomoglyphs(text: String): String = text.lowercase()
            .replace("rn", "m")
            .replace("vv", "w")
            .map {
                when (it) {
                    '0' -> 'o'
                    '1' -> 'l'
                    '3' -> 'e'
                    '4' -> 'a'
                    '5' -> 's'
                    '7' -> 't'
                    '@' -> 'a'
                    else -> it
                }
            }
            .joinToString("")

        /** Classic edit distance between two strings. */
        fun levenshtein(a: String, b: String): Int {
            if (a == b) return 0
            if (a.isEmpty()) return b.length
            if (b.isEmpty()) return a.length
            var previous = IntArray(b.length + 1) { it }
            var current = IntArray(b.length + 1)
            for (i in 1..a.length) {
                current[0] = i
                for (j in 1..b.length) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
                }
                val swap = previous
                previous = current
                current = swap
            }
            return previous[b.length]
        }
    }
}
