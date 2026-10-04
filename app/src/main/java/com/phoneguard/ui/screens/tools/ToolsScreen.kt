@file:OptIn(ExperimentalMaterial3Api::class)

package com.phoneguard.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.phoneguard.R
import com.phoneguard.ui.components.TabTopBar
import com.phoneguard.ui.theme.PhoneGuardTheme

/** Tools tab: link and message checker, QR scanner and Wi-Fi check. */
@Composable
fun ToolsScreen(
    onOpenLinkCheck: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onOpenWifi: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { TabTopBar(stringResource(R.string.tools_title), scrollBehavior) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ToolCard(
                icon = Icons.Rounded.Link,
                tint = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.tool_link_title),
                body = stringResource(R.string.tool_link_body),
                onClick = onOpenLinkCheck,
            )
            ToolCard(
                icon = Icons.Rounded.QrCodeScanner,
                tint = PhoneGuardTheme.statusColors.low,
                title = stringResource(R.string.tool_qr_title),
                body = stringResource(R.string.tool_qr_body),
                onClick = onOpenQrScanner,
            )
            ToolCard(
                icon = Icons.Rounded.Sms,
                tint = PhoneGuardTheme.statusColors.warning,
                title = stringResource(R.string.tool_message_title),
                body = stringResource(R.string.tool_message_body),
                onClick = onOpenLinkCheck,
            )
            ToolCard(
                icon = Icons.Rounded.Wifi,
                tint = PhoneGuardTheme.statusColors.safe,
                title = stringResource(R.string.tool_wifi_title),
                body = stringResource(R.string.tool_wifi_body),
                onClick = onOpenWifi,
            )
            PrivacyNote()
        }
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(tint.copy(alpha = 0.16f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** Reminds the user what leaves the phone and what does not. */
@Composable
fun PrivacyNote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
