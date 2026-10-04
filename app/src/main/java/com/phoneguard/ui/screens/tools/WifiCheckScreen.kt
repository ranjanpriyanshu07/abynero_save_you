package com.phoneguard.ui.screens.tools

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiPassword
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoneguard.R
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.WifiCheckResult
import com.phoneguard.domain.model.WifiSecurity
import com.phoneguard.ui.components.BackTopBar
import com.phoneguard.ui.components.PermissionRationaleDialog
import com.phoneguard.ui.components.ReasonRow
import com.phoneguard.ui.components.SectionHeader
import com.phoneguard.ui.util.SettingsLauncher
import com.phoneguard.ui.util.color
import com.phoneguard.ui.util.containerColor
import com.phoneguard.ui.util.labelRes
import com.phoneguard.ui.util.onContainerColor

/**
 * Wi-Fi safety check: warns about open and WEP networks and explains how to stay safe on
 * public Wi-Fi.
 */
@Composable
fun WifiCheckScreen(
    onBack: () -> Unit,
    viewModel: WifiViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refresh() }

    if (showRationale) {
        PermissionRationaleDialog(
            icon = Icons.Rounded.LocationOn,
            title = stringResource(R.string.wifi_location_title),
            text = stringResource(R.string.wifi_location_body),
            onConfirm = {
                showRationale = false
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onDismiss = { showRationale = false },
        )
    }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.wifi_title), onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val result = state.result
            if (state.isLoading || result == null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            } else {
                WifiStatusCard(result)
                if (result.locationPermissionNeeded) {
                    val alreadyGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                    Button(
                        onClick = {
                            if (alreadyGranted) viewModel.refresh() else showRationale = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_allow_location))
                    }
                }
                if (result.isWifiConnected) WifiDetails(result)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = viewModel::refresh,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_check_again))
                }
                OutlinedButton(
                    onClick = { SettingsLauncher.openWifiSettings(context) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_wifi_settings))
                }
            }

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(stringResource(R.string.wifi_tips_title))
                    stringArrayResource(R.array.wifi_tips).forEach { ReasonRow(it, RiskLevel.LOW) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WifiStatusCard(result: WifiCheckResult) {
    val level = result.level
    val (title, body) = when {
        !result.isWifiConnected -> R.string.wifi_not_connected_title to R.string.wifi_not_connected_body
        result.security.isUnsafe -> R.string.wifi_unsafe_title to R.string.wifi_unsafe_body
        result.locationPermissionNeeded -> R.string.wifi_location_needed_title to R.string.wifi_location_body
        result.security == WifiSecurity.UNKNOWN -> R.string.wifi_unknown_title to R.string.wifi_unknown_body
        else -> R.string.wifi_safe_title to R.string.wifi_safe_body
    }
    val icon = when {
        !result.isWifiConnected -> Icons.Rounded.SignalCellularAlt
        level == RiskLevel.SAFE -> Icons.Rounded.WifiPassword
        else -> Icons.Rounded.Wifi
    }
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = level.containerColor(), contentColor = level.onContainerColor()),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(level.color().copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = level.color(), modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                if (title == R.string.wifi_safe_title) {
                    stringResource(body, stringResource(result.security.labelRes))
                } else {
                    stringResource(body)
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WifiDetails(result: WifiCheckResult) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            DetailRow(stringResource(R.string.wifi_detail_name), result.ssid ?: stringResource(R.string.wifi_name_hidden))
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            DetailRow(stringResource(R.string.wifi_detail_security), stringResource(result.security.labelRes))
            result.nearbyOpenNetworks?.let { count ->
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                DetailRow(stringResource(R.string.wifi_detail_nearby_open), count.toString())
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End)
    }
}
