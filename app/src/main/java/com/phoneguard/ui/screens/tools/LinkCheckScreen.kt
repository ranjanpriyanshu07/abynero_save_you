package com.phoneguard.ui.screens.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoneguard.R
import com.phoneguard.domain.model.LinkCheckResult
import com.phoneguard.domain.model.MessageCheckResult
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.SafeBrowsingStatus
import com.phoneguard.domain.model.UrlRedFlag
import com.phoneguard.ui.components.BackTopBar
import com.phoneguard.ui.components.ReasonRow
import com.phoneguard.ui.components.RiskChip
import com.phoneguard.ui.components.SectionHeader
import com.phoneguard.ui.util.color
import com.phoneguard.ui.util.containerColor
import com.phoneguard.ui.util.icon
import com.phoneguard.ui.util.onContainerColor
import com.phoneguard.ui.util.textRes

/**
 * Link and message checker. The user pastes or shares a link or a whole SMS, or scans a
 * QR code, and gets a clear verdict with the reasons.
 *
 * @param incomingText text shared from another app or read from a QR code, if any.
 * @param openQrOnStart opens the QR scanner straight away (used by the Tools tab).
 */
@Composable
fun LinkCheckScreen(
    incomingText: String?,
    onIncomingConsumed: () -> Unit,
    openQrOnStart: Boolean,
    onOpenQr: () -> Unit,
    onBack: () -> Unit,
    viewModel: LinkCheckViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    var qrOpened by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(incomingText) {
        if (incomingText != null) {
            viewModel.onIncomingText(incomingText)
            onIncomingConsumed()
        }
    }
    LaunchedEffect(openQrOnStart) {
        if (openQrOnStart && !qrOpened) {
            qrOpened = true
            onOpenQr()
        }
    }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.link_check_title), onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.link_check_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::onInputChange,
                label = { Text(stringResource(R.string.link_check_hint)) },
                placeholder = { Text(stringResource(R.string.link_check_placeholder)) },
                minLines = 3,
                maxLines = 8,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Default),
                trailingIcon = {
                    if (state.input.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onInputChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.cd_clear))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { clipboard.getText()?.text?.let(viewModel::onInputChange) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_paste))
                }
                OutlinedButton(
                    onClick = onOpenQr,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_scan_qr))
                }
            }
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.check()
                },
                enabled = state.input.isNotBlank() && !state.isChecking,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_check), style = MaterialTheme.typography.titleMedium)
            }

            if (state.isChecking) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.link_checking), style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (state.failed) {
                Text(stringResource(R.string.link_check_failed), color = MaterialTheme.colorScheme.error)
            }

            AnimatedVisibility(
                visible = state.result != null,
                enter = fadeIn() + slideInVertically { it / 4 },
            ) {
                state.result?.let { MessageResult(it) }
            }

            PrivacyNote()
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MessageResult(result: MessageCheckResult) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (result.links.isEmpty() && result.signals.isEmpty()) {
            VerdictCard(
                level = RiskLevel.LOW,
                title = stringResource(R.string.link_no_link_title),
                body = stringResource(R.string.link_no_link_body),
            )
            return@Column
        }
        VerdictCard(
            level = result.level,
            title = stringResource(verdictTitle(result.level)),
            body = stringResource(verdictBody(result.level)),
        )
        if (result.signals.isNotEmpty()) {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(stringResource(R.string.link_scam_signs), count = result.signals.size)
                    result.signals.forEach { ReasonRow(stringResource(it.textRes), RiskLevel.HIGH) }
                }
            }
        }
        if (result.links.isNotEmpty()) {
            SectionHeader(stringResource(R.string.link_links_found), count = result.links.size)
            result.links.forEach { LinkResultCard(it) }
        }
    }
}

@Composable
private fun VerdictCard(level: RiskLevel, title: String, body: String) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = level.containerColor(), contentColor = level.onContainerColor()),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(level.color().copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (level == RiskLevel.SAFE) Icons.Rounded.VerifiedUser else level.icon,
                    contentDescription = null,
                    tint = level.color(),
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LinkResultCard(link: LinkCheckResult) {
    val analysis = link.analysis
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = analysis.host ?: analysis.input,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                RiskChip(link.level)
            }
            Text(
                analysis.input,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            analysis.flags.forEach { flag ->
                val text = when {
                    flag == UrlRedFlag.LOOKALIKE_DOMAIN && analysis.impersonatedBrand != null ->
                        stringResource(R.string.flag_lookalike_brand, analysis.impersonatedBrand.orEmpty())
                    flag == UrlRedFlag.UPI_PAYMENT_LINK && analysis.upiPayee != null ->
                        stringResource(R.string.flag_upi_payee, analysis.upiPayee.orEmpty())
                    else -> stringResource(flag.textRes)
                }
                ReasonRow(text, flag.level)
            }
            if (analysis.flags.isEmpty()) {
                ReasonRow(stringResource(R.string.link_no_flags), RiskLevel.SAFE)
            }
            if (link.safeBrowsing.status != SafeBrowsingStatus.SKIPPED) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sbIcon = when (link.safeBrowsing.status) {
                        SafeBrowsingStatus.NO_THREAT -> Icons.Rounded.GppGood
                        SafeBrowsingStatus.THREAT_FOUND -> RiskLevel.HIGH.icon
                        SafeBrowsingStatus.OFFLINE, SafeBrowsingStatus.ERROR, SafeBrowsingStatus.NOT_CONFIGURED -> Icons.Rounded.CloudOff
                        SafeBrowsingStatus.SKIPPED -> Icons.Rounded.Payments
                    }
                    val sbLevel = when (link.safeBrowsing.status) {
                        SafeBrowsingStatus.NO_THREAT -> RiskLevel.SAFE
                        SafeBrowsingStatus.THREAT_FOUND -> RiskLevel.HIGH
                        else -> RiskLevel.LOW
                    }
                    Icon(sbIcon, contentDescription = null, tint = sbLevel.color(), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(link.safeBrowsing.status.textRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun verdictTitle(level: RiskLevel): Int = when (level) {
    RiskLevel.SAFE -> R.string.verdict_safe_title
    RiskLevel.LOW -> R.string.verdict_low_title
    RiskLevel.MEDIUM -> R.string.verdict_medium_title
    RiskLevel.HIGH -> R.string.verdict_high_title
}

private fun verdictBody(level: RiskLevel): Int = when (level) {
    RiskLevel.SAFE -> R.string.verdict_safe_body
    RiskLevel.LOW -> R.string.verdict_low_body
    RiskLevel.MEDIUM -> R.string.verdict_medium_body
    RiskLevel.HIGH -> R.string.verdict_high_body
}
