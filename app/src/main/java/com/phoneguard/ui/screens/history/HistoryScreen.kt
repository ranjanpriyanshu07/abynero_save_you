@file:OptIn(ExperimentalMaterial3Api::class)

package com.phoneguard.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoneguard.R
import com.phoneguard.domain.model.ScanHistoryEntry
import com.phoneguard.domain.model.ScoreGrade
import com.phoneguard.ui.components.EmptyState
import com.phoneguard.ui.components.SectionHeader
import com.phoneguard.ui.components.TabTopBar
import com.phoneguard.ui.components.TrendChart
import com.phoneguard.ui.theme.PhoneGuardTheme
import com.phoneguard.ui.util.formatDateTime
import com.phoneguard.ui.util.labelRes
import com.phoneguard.ui.util.scoreColor

/** History tab: score trend chart and a list of past scans. */
@Composable
fun HistoryScreen(
    onScanStarted: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearHistory()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TabTopBar(stringResource(R.string.history_title), scrollBehavior) {
                if (state.entries.isNotEmpty()) {
                    IconButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.history_clear_title))
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        if (!state.isLoading && state.entries.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.History,
                title = stringResource(R.string.history_empty_title),
                body = stringResource(R.string.history_empty_body),
                actionLabel = stringResource(R.string.scan_now),
                onAction = {
                    viewModel.startScan()
                    onScanStarted()
                },
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "trend") { TrendCard(state) }
            item(key = "header") { SectionHeader(stringResource(R.string.history_past_scans), count = state.entries.size) }
            items(state.entries, key = { it.id }) { entry -> HistoryRow(entry) }
        }
    }
}

@Composable
private fun TrendCard(state: HistoryUiState) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.history_trend_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            val latest = state.latest?.score
            TrendChart(
                scores = state.trend,
                description = stringResource(R.string.history_trend_description, state.trend.size, latest ?: 0),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Stat(stringResource(R.string.history_stat_latest), latest?.toString() ?: "--")
                Stat(stringResource(R.string.history_stat_best), state.best?.toString() ?: "--")
                ChangeStat(state.change)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChangeStat(change: Int?) {
    val colors = PhoneGuardTheme.statusColors
    val (icon, tint) = when {
        change == null || change == 0 -> Icons.AutoMirrored.Rounded.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
        change > 0 -> Icons.AutoMirrored.Rounded.TrendingUp to colors.safe
        else -> Icons.AutoMirrored.Rounded.TrendingDown to colors.danger
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = when {
                    change == null -> "--"
                    change > 0 -> "+$change"
                    else -> change.toString()
                },
                style = MaterialTheme.typography.headlineSmall,
                color = tint,
            )
        }
        Text(stringResource(R.string.history_stat_change), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HistoryRow(entry: ScanHistoryEntry) {
    val color = PhoneGuardTheme.statusColors.scoreColor(entry.score.toFloat())
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(entry.score.toString(), style = MaterialTheme.typography.titleLarge, color = color)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(formatDateTime(entry.timestamp), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(
                        R.string.history_row_details,
                        stringResource(ScoreGrade.forScore(entry.score).labelRes),
                        stringResource(entry.trigger.labelRes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.history_row_counts, entry.highRiskApps + entry.mediumRiskApps, entry.deviceIssues),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
