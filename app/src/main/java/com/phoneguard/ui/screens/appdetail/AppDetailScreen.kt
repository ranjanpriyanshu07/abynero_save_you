package com.phoneguard.ui.screens.appdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoneguard.R
import com.phoneguard.domain.model.AppRiskReport
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.RiskyPermission
import com.phoneguard.ui.components.AppIcon
import com.phoneguard.ui.components.BackTopBar
import com.phoneguard.ui.components.EmptyState
import com.phoneguard.ui.components.ReasonRow
import com.phoneguard.ui.components.RiskChip
import com.phoneguard.ui.components.SectionHeader
import com.phoneguard.ui.util.SettingsLauncher
import com.phoneguard.ui.util.adviceRes
import com.phoneguard.ui.util.color
import com.phoneguard.ui.util.containerColor
import com.phoneguard.ui.util.descriptionRes
import com.phoneguard.ui.util.formatDate
import com.phoneguard.ui.util.icon
import com.phoneguard.ui.util.labelRes
import com.phoneguard.ui.util.onContainerColor
import com.phoneguard.ui.util.textRes
import com.phoneguard.ui.util.titleRes

/**
 * Details for one app: icon, risk badge, reasons, sensitive permissions and the
 * "Open settings" / "Uninstall" buttons.
 */
@Composable
fun AppDetailScreen(
    onBack: () -> Unit,
    viewModel: AppDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    Scaffold(
        topBar = { BackTopBar(title = stringResource(R.string.app_detail_title), onBack = onBack) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val current = state) {
                AppDetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                AppDetailUiState.NotInstalled -> EmptyState(
                    icon = Icons.Rounded.CheckCircle,
                    title = stringResource(R.string.app_detail_removed_title),
                    body = stringResource(R.string.app_detail_removed_body),
                    actionLabel = stringResource(R.string.action_back),
                    onAction = onBack,
                    modifier = Modifier.align(Alignment.Center),
                )
                is AppDetailUiState.Loaded -> AppDetailContent(current.report, viewModel::onFixLaunched)
            }
        }
    }
}

@Composable
private fun AppDetailContent(report: AppRiskReport, onFixLaunched: () -> Unit) {
    val context = LocalContext.current
    val assessment = report.assessment
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header.
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppIcon(report.packageName, size = 80.dp)
                Spacer(Modifier.height(12.dp))
                Text(report.appName, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(
                    report.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                RiskChip(report.level)
                Spacer(Modifier.height(12.dp))
                InfoLine(stringResource(R.string.app_detail_source, stringResource(assessment.installSource.labelRes)))
                report.versionName?.let { InfoLine(stringResource(R.string.app_detail_version, it)) }
                if (report.firstInstallTime > 0) {
                    InfoLine(stringResource(R.string.app_detail_installed_on, formatDate(report.firstInstallTime)))
                }
            }
        }

        if (assessment.isRemoteControlApp) ScamWarningCard()

        if (assessment.reasons.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(stringResource(R.string.app_detail_found))
                assessment.reasons.forEach { ReasonRow(stringResource(it.textRes), report.level) }
            }
        }

        // What to do.
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = report.level.containerColor(),
                contentColor = report.level.onContainerColor(),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Lightbulb, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.app_detail_what_to_do), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(assessment.action.adviceRes), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(stringResource(R.string.app_detail_permissions), count = assessment.permissions.size)
            if (assessment.permissions.isEmpty()) {
                Text(
                    stringResource(R.string.app_no_risky_permissions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                assessment.permissions.sortedByDescending { it.weight }.forEach { PermissionRow(it, report.level) }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            OutlinedButton(
                onClick = { if (SettingsLauncher.openAppSettings(context, report.packageName)) onFixLaunched() },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_open_settings))
            }
            Button(
                onClick = { if (SettingsLauncher.uninstall(context, report.packageName)) onFixLaunched() },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_uninstall))
            }
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ScamWarningCard() {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = RiskLevel.HIGH.containerColor(),
            contentColor = RiskLevel.HIGH.onContainerColor(),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.GppMaybe, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.scam_warning_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.scam_warning_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PermissionRow(permission: RiskyPermission, level: RiskLevel) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(level.color().copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(permission.icon, contentDescription = null, tint = level.color(), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(stringResource(permission.titleRes), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(permission.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
