package com.phoneguard.scanner

import com.phoneguard.domain.message.MessageScamAnalyzer
import com.phoneguard.domain.model.MessageCheckResult
import com.phoneguard.domain.model.RiskLevel
import javax.inject.Inject

/**
 * Checks a pasted or shared message (or a single link): every link inside it plus
 * scam phrases in the text.
 */
class MessageChecker @Inject constructor(
    private val analyzer: MessageScamAnalyzer,
    private val linkScanner: LinkScanner,
) {
    /** Checks [text]. Returns null when there is nothing to check. */
    suspend fun check(text: String): MessageCheckResult? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val urls = analyzer.extractUrls(trimmed).ifEmpty {
            // A single word with no spaces is treated as a link the user typed.
            if (trimmed.none { it.isWhitespace() } && trimmed.contains('.')) listOf(trimmed) else emptyList()
        }
        val links = urls.map { linkScanner.check(it) }
        val signals = analyzer.detectSignals(trimmed)
        if (links.isEmpty() && signals.isEmpty()) {
            return MessageCheckResult(links = emptyList(), signals = emptySet(), level = RiskLevel.SAFE)
        }
        val level = RiskLevel.max(links.map { it.level } + analyzer.levelForSignals(signals))
        return MessageCheckResult(links = links, signals = signals, level = level)
    }
}
