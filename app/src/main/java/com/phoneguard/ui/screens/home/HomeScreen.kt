package com.phoneguard.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoneguard.R
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.ScanProgress
import com.phoneguard.domain.model.ScanStage
import com.phoneguard.domain.model.ScoreCategory
import com.phoneguard.domain.model.levelForScore
import com.phoneguard.ui.components.ScanPulseButton
import com.phoneguard.ui.components.ScoreGauge
import com.phoneguard.ui.components.SummaryCard
import com.phoneguard.ui.util.formatDateTime
import com.phoneguard.ui.util.icon
import com.phoneguard.ui.util.labelRes

/**
 * Home screen: animated score gauge, the big Scan Now button and a summary card for each
 * part of the score.
 */
@Composable
fun HomeScreen(
    onOpenResults: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenLinks: () -> Unit,
    onOpenTips: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val failedText = stringResource(R.string.scan_failed)
    LaunchedEffect(state.scanFailed) {
        if (state.scanFailed) snackbarHostState.showSnackbar(failedText)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeHeader()
            Spacer(Modifier.height(12.dp))
            ScoreGauge(score = state.score, animationKey = state.scoreKey)
            Spacer(Modifier.height(8.dp))
            StatusLine(state)
            Spacer(Modifier.height(8.dp))
            ScanPulseButton(isScanning = state.isScanning, onClick = viewModel::onScanClick)

            AnimatedVisibility(
                visible = state.isScanning,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = { state.progress?.fraction ?: 0f },
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        strokeCap = StrokeCap.Round,
                    )
                    TextButton(onClick = onOpenResults) { Text(stringResource(R.string.home_watch_live)) }
                }
            }
            AnimatedVisibility(visible = !state.isScanning && state.hasDetails) {
                OutlinedButton(onClick = onOpenResults, shape = RoundedCornerShape(16.dp)) {
                    Text(stringResource(R.string.home_see_results))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.home_breakdown_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            SummaryGrid(state, onOpenResults, onOpenWifi, onOpenLinks)
            Spacer(Modifier.height(16.dp))
            TipOfTheDay(onOpenTips)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
                    RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusLine(state: HomeUiState) {
    val text = when {
        state.isScanning -> progressText(state.progress)
        state.score == null -> stringResource(R.string.home_status_first_scan)
        state.problemCount == 0 -> stringResource(R.string.home_status_all_good)
        else -> stringResource(R.string.home_status_problems, state.problemCount)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        val lastScan = state.lastScanAt
        if (lastScan != null && !state.isScanning) {
            Text(
                text = stringResource(R.string.home_last_scan, formatDateTime(lastScan)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Checking apps… 42/87" style text for the current scan stage. */
@Composable
fun progressText(progress: ScanProgress?): String = when {
    progress == null -> stringResource(R.string.scanning)
    progress.stage == ScanStage.APPS && progress.total > 0 ->
        stringResource(R.string.stage_apps_count, progress.done, progress.total)
    else -> stringResource(progress.stage.labelRes)
}

@Composable
private fun SummaryGrid(
    state: HomeUiState,
    onOpenResults: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenLinks: () -> Unit,
) {
    val scores = state.categoryScores
    fun levelOf(category: ScoreCategory): RiskLevel = scores[category]?.let { levelForScore(it) } ?: RiskLevel.LOW

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                title = stringResource(ScoreCategory.APPS.labelRes),
                value = state.riskyApps?.let { stringResource(R.string.home_apps_value, it) } ?: "--",
                caption = state.appsScanned?.let { stringResource(R.string.home_apps_caption, it) }
                    ?: stringResource(R.string.home_not_checked),
                icon = ScoreCategory.APPS.icon,
                level = levelOf(ScoreCategory.APPS),
                score = scores[ScoreCategory.APPS],
                onClick = onOpenResults,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                title = stringResource(ScoreCategory.DEVICE.labelRes),
                value = when (val issues = state.deviceIssues) {
                    null -> "--"
                    0 -> stringResource(R.string.home_device_ok)
                    else -> stringResource(R.string.home_device_issues, issues)
                },
                caption = stringResource(R.string.home_device_caption),
                icon = ScoreCategory.DEVICE.icon,
                level = levelOf(ScoreCategory.DEVICE),
                score = scores[ScoreCategory.DEVICE],
                onClick = onOpenResults,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val wifi = state.wifi
            SummaryCard(
                title = stringResource(ScoreCategory.NETWORK.labelRes),
                value = when {
                    wifi == null -> scores[ScoreCategory.NETWORK]?.let { stringResource(R.string.home_score_value, it) } ?: "--"
                    !wifi.isWifiConnected -> stringResource(R.string.home_network_mobile)
                    wifi.security.isUnsafe -> stringResource(R.string.home_network_unsafe)
                    wifi.level == RiskLevel.SAFE -> stringResource(R.string.home_network_secure)
                    else -> stringResource(R.string.home_network_unknown)
                },
                caption = wifi?.ssid ?: stringResource(R.string.home_network_caption),
                icon = ScoreCategory.NETWORK.icon,
                level = wifi?.level ?: levelOf(ScoreCategory.NETWORK),
                score = scores[ScoreCategory.NETWORK],
                onClick = onOpenWifi,
                modifier = Modifier.weight(1f),
            )
            val links = state.linkStats
            SummaryCard(
                title = stringResource(ScoreCategory.LINKS.labelRes),
                value = stringResource(R.string.home_links_value, links.checked),
                caption = if (links.dangerous > 0) {
                    stringResource(R.string.home_links_caption_bad, links.dangerous)
                } else {
                    stringResource(R.string.home_links_caption_ok)
                },
                icon = ScoreCategory.LINKS.icon,
                level = if (links.dangerous > 0) RiskLevel.HIGH else levelOf(ScoreCategory.LINKS),
                score = scores[ScoreCategory.LINKS],
                onClick = onOpenLinks,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TipOfTheDay(onOpenTips: () -> Unit) {
    val tips = stringArrayResource(R.array.quick_tips)
    val index by rememberSaveable { mutableStateOf((System.currentTimeMillis() / 86_400_000L % tips.size).toInt()) }
    Card(
        onClick = onOpenTips,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Lightbulb, contentDescription = null)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_tip_title), style = MaterialTheme.typography.labelLarge)
                Text(tips[index.coerceIn(0, tips.lastIndex)], style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
        }
    }
}
