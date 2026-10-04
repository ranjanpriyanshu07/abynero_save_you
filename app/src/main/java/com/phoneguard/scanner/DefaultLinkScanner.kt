package com.phoneguard.scanner

import com.phoneguard.domain.model.LinkCheckResult
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.SafeBrowsingResult
import com.phoneguard.domain.model.SafeBrowsingStatus
import com.phoneguard.domain.url.UrlRedFlagChecker
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Combines on-device red-flag checks with a Google Safe Browsing lookup.
 *
 * Only http/https links are sent to Safe Browsing; nothing else leaves the phone.
 */
class DefaultLinkScanner @Inject constructor(
    private val redFlagChecker: UrlRedFlagChecker,
    private val safeBrowsingClient: SafeBrowsingClient,
) : LinkScanner {

    override suspend fun check(rawUrl: String): LinkCheckResult {
        val analysis = redFlagChecker.check(rawUrl)
        val url = analysis.normalizedUrl
        val safeBrowsing = if (analysis.isWebUrl && url != null) {
            try {
                safeBrowsingClient.lookup(url)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SafeBrowsingResult(SafeBrowsingStatus.ERROR)
            }
        } else {
            SafeBrowsingResult(SafeBrowsingStatus.SKIPPED)
        }
        val level = if (safeBrowsing.status == SafeBrowsingStatus.THREAT_FOUND) RiskLevel.HIGH else analysis.level
        return LinkCheckResult(analysis = analysis, safeBrowsing = safeBrowsing, level = level)
    }
}
