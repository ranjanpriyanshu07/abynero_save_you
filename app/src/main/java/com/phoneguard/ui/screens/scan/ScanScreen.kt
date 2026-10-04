@file:OptIn(ExperimentalMaterial3Api::class)

package com.phoneguard.ui.screens.scan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoneguard.R
import com.phoneguard.domain.model.CheckStatus
import com.phoneguard.domain.model.DeviceCheckType
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.ScanProgress
import com.phoneguard.domain.model.ScanResult
import com.phoneguard.domain.model.ScanStage
import com.phoneguard.domain.model.SuggestedAction
import com.phoneguard.ui.components.AppIcon
import com.phoneguard.ui.components.RadarAnimation
import com.phoneguard.ui.components.ResultCard
import com.phoneguard.ui.components.ScanPulseButton
import com.phoneguard.ui.components.SectionHeader
import com.phoneguard.ui.components.TabTopBar
import com.phoneguard.ui.components.TintedIcon
import com.phoneguard.ui.screens.home.progressText
import com.phoneguard.ui.theme.PhoneGuardTheme
import com.phoneguard.ui.util.SettingsLauncher
import com.phoneguard.ui.util.adviceRes
import com.phoneguard.ui.util.buttonRes
import com.phoneguard.ui.util.color
import com.phoneguard.ui.util.fixRes
import com.phoneguard.ui.util.formatDateTime
import com.phoneguard.ui.util.groupTitleRes
import com.phoneguard.ui.util.icon
import com.phoneguard.ui.util.issueRes
import com.phoneguard.ui.util.labelRes
import com.phoneguard.ui.util.passedRes
import com.phoneguard.ui.util.scoreColor
import com.phoneguard.ui.util.textRes
import com.phoneguard.ui.util.titleRes
import com.phoneguard.ui.util.whyRes

/**
 * Scan tab: radar animation with live progress while scanning, then results grouped by
 * severity with a Fix button on every problem.
 */
@Composable
fun ScanScreen(
    onOpenApp: (String) -> Unit,
    onOpenWifi: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { TabTopBar(stringResource(R.string.scan_title), scrollBehavior) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        val mode = when {
            state.isScanning -> ScanMode.SCANNING
            state.result != null -> ScanMode.RESULTS
            else -> ScanMode.IDLE
        }
        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            modifier = Modifier.padding(padding),
            label = "scanMode",
        ) { current ->
            when (current) {
                ScanMode.SCANNING -> ScanningContent(state.progress)
                ScanMode.IDLE -> IdleContent(state.scanFailed, viewModel::startScan)
                ScanMode.RESULTS -> state.result?.let { result ->
                    ResultsContent(
                        state = state,
                        result = result,
                        onFilter = viewModel::setFilter,
                        onRescan = viewModel::startScan,
                        onOpenApp = onOpenApp,
                        onOpenWifi = onOpenWifi,
                        onFixLaunched = viewModel::onFixLaunched,
                    )
                }
            }
        }
    }
}

private enum class ScanMode { IDLE, SCANNING, RESULTS }

@Composable
private fun ScanningContent(progress: ScanProgress?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        RadarAnimation(size = 248.dp)
        Spacer(Modifier.height(28.dp))
        Text(
            text = progressText(progress),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress?.fraction ?: 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            strokeCap = StrokeCap.Round,
        )
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ScanStage.entries.forEach { stage ->
                val current = progress?.stage ?: ScanStage.APPS
                val done = stage.ordinal < current.ordinal
                val active = stage == current
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (done) Icons.Rounded.CheckCircle else if (active) stage.icon else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = when {
                            done -> PhoneGuardTheme.statusColors.safe
                            active -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(stage.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (done || active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent(scanFailed: Boolean, onScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RadarAnimation(size = 220.dp, animate = false)
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(if (scanFailed) R.string.scan_failed else R.string.scan_idle_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.scan_idle_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        ScanPulseButton(isScanning = false, onClick = onScan)
    }
}

@Composable
private fun ResultsContent(
    state: ScanUiState,
    result: ScanResult,
    onFilter: (RiskLevel?) -> Unit,
    onRescan: () -> Unit,
    onOpenApp: (String) -> Unit,
    onOpenWifi: () -> Unit,
    onFixLaunched: () -> Unit,
) {
    var showSafe by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "summary") { ResultSummaryCard(result, onRescan) }
        item(key = "filters") { FilterRow(state, onFilter) }
        if (state.groups.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.results_nothing_here),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        state.groups.forEach { group ->
            val collapsible = group.level == RiskLevel.SAFE && state.filter == null
            item(key = "header-${group.level}") {
                SectionHeader(
                    text = stringResource(group.level.groupTitleRes),
                    count = group.items.size,
                    color = group.level.color(),
                )
            }
            if (collapsible && !showSafe) {
                item(key = "show-safe") {
                    OutlinedButton(onClick = { showSafe = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.results_show_safe, group.items.size))
                    }
                }
            } else {
                items(group.items, key = { it.key }) { item ->
                    ResultItemCard(item, onOpenApp, onOpenWifi, onFixLaunched)
                }
                if (collapsible) {
                    item(key = "hide-safe") {
                        TextButton(onClick = { showSafe = false }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.results_hide_safe))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSummaryCard(result: ScanResult, onRescan: () -> Unit) {
    val score = result.score.overall
    val color = PhoneGuardTheme.statusColors.scoreColor(score.toFloat())
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(score.toString(), style = MaterialTheme.typography.displaySmall, color = color)
                Text(stringResource(result.score.grade.labelRes), style = MaterialTheme.typography.labelLarge, color = color)
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.results_summary, result.apps.size, result.riskyApps.size, result.deviceIssueCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.home_last_scan, formatDateTime(result.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRescan, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_scan_again))
                }
            }
        }
    }
}

@Composable
private fun FilterRow(state: ScanUiState, onFilter: (RiskLevel?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = state.filter == null,
                onClick = { onFilter(null) },
                label = { Text(stringResource(R.string.filter_all)) },
            )
        }
        items(RiskLevel.entries.reversed()) { level ->
            val count = state.counts[level] ?: 0
            FilterChip(
                selected = state.filter == level,
                onClick = { onFilter(if (state.filter == level) null else level) },
                label = { Text(stringResource(R.string.filter_with_count, stringResource(level.labelRes), count)) },
                leadingIcon = { Icon(level.icon, contentDescription = null, tint = level.color(), modifier = Modifier.size(18.dp)) },
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}

@Composable
private fun ResultItemCard(
    item: ResultItem,
    onOpenApp: (String) -> Unit,
    onOpenWifi: () -> Unit,
    onFixLaunched: () -> Unit,
) {
    val context = LocalContext.current
    when (item) {
        is ResultItem.App -> {
            val report = item.report
            val assessment = report.assessment
            val reasons = assessment.reasons.map { stringResource(it.textRes) }
            ResultCard(
                title = report.appName,
                subtitle = reasons.firstOrNull() ?: stringResource(R.string.app_no_risky_permissions),
                level = report.level,
                leading = { AppIcon(report.packageName) },
                reasons = reasons,
                advice = stringResource(assessment.action.adviceRes),
                fixLabel = if (assessment.action == SuggestedAction.NO_ACTION) null else stringResource(assessment.action.buttonRes),
                onFix = {
                    if (SettingsLauncher.openForApp(context, report.packageName, assessment.action)) onFixLaunched()
                },
                secondaryLabel = stringResource(R.string.action_details),
                onSecondary = { onOpenApp(report.packageName) },
                initiallyExpanded = report.level == RiskLevel.HIGH,
                showChip = report.level != RiskLevel.SAFE,
            )
        }
        is ResultItem.Device -> {
            val check = item.check
            val subtitle = when {
                check.type == DeviceCheckType.SECURITY_PATCH && check.patchAgeDays != null && check.securityPatch != null ->
                    stringResource(R.string.device_patch_age, check.securityPatch.orEmpty(), check.patchAgeDays ?: 0L)
                check.status == CheckStatus.PASSED -> stringResource(check.type.passedRes)
                else -> stringResource(check.type.issueRes)
            }
            val reasons = buildList {
                add(stringResource(check.type.whyRes))
                if (check.affectedApps.isNotEmpty()) {
                    add(stringResource(R.string.device_affected_apps, check.affectedApps.joinToString(", ")))
                }
            }
            ResultCard(
                title = stringResource(check.type.titleRes),
                subtitle = subtitle,
                level = item.level,
                leading = { TintedIcon(check.type.icon, item.level) },
                reasons = if (check.status == CheckStatus.PASSED) emptyList() else reasons,
                fixLabel = if (check.status == CheckStatus.PASSED) null else stringResource(check.type.fixRes),
                onFix = { if (SettingsLauncher.openFor(context, check.type)) onFixLaunched() },
                showChip = check.status != CheckStatus.PASSED,
            )
        }
        is ResultItem.Wifi -> {
            val wifi = item.wifi
            ResultCard(
                title = wifi.ssid?.let { stringResource(R.string.wifi_named, it) } ?: stringResource(R.string.wifi_this_network),
                subtitle = stringResource(wifi.security.labelRes),
                level = item.level,
                leading = { TintedIcon(Icons.Rounded.Wifi, item.level) },
                reasons = if (wifi.security.isUnsafe) listOf(stringResource(R.string.wifi_unsafe_body)) else emptyList(),
                fixLabel = if (wifi.level == RiskLevel.SAFE) null else stringResource(R.string.action_details),
                onFix = onOpenWifi,
                showChip = wifi.level != RiskLevel.SAFE,
            )
        }
    }
}
