package com.phoneguard.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.phoneguard.R
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.ui.components.RiskChip

/**
 * Explains in simple words why each permission is needed, and lets the user grant the
 * optional ones. Nothing here is required to use the app.
 */
@Composable
fun PermissionsScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var notificationsGranted by remember {
        mutableStateOf(context.isGranted(Manifest.permission.POST_NOTIFICATIONS, minSdk = Build.VERSION_CODES.TIRAMISU))
    }
    var locationGranted by remember {
        mutableStateOf(context.isGranted(Manifest.permission.ACCESS_FINE_LOCATION, minSdk = 0))
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsGranted = it
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        locationGranted = it
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.permissions_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.permissions_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PermissionItem(
                icon = Icons.Rounded.Apps,
                title = stringResource(R.string.permission_apps_title),
                body = stringResource(R.string.permission_apps_body),
                status = stringResource(R.string.permission_status_always),
                granted = true,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionItem(
                    icon = Icons.Rounded.NotificationsActive,
                    title = stringResource(R.string.permission_notifications_title),
                    body = stringResource(R.string.permission_notifications_body),
                    status = stringResource(R.string.permission_status_allowed),
                    granted = notificationsGranted,
                    onAllow = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                )
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                PermissionItem(
                    icon = Icons.Rounded.LocationOn,
                    title = stringResource(R.string.permission_location_title),
                    body = stringResource(R.string.wifi_location_body),
                    status = stringResource(R.string.permission_status_allowed),
                    granted = locationGranted,
                    onAllow = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                )
            }
            PermissionItem(
                icon = Icons.Rounded.CameraAlt,
                title = stringResource(R.string.permission_camera_title),
                body = stringResource(R.string.permission_camera_body),
                status = stringResource(R.string.permission_status_later),
                granted = true,
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.complete(onDone) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(stringResource(R.string.action_continue), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    body: String,
    status: String,
    granted: Boolean,
    onAllow: (() -> Unit)? = null,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                if (granted || onAllow == null) {
                    RiskChip(level = RiskLevel.SAFE, text = status)
                } else {
                    FilledTonalButton(onClick = onAllow, shape = RoundedCornerShape(14.dp)) {
                        Text(stringResource(R.string.action_allow))
                    }
                }
            }
        }
    }
}

private fun Context.isGranted(permission: String, minSdk: Int): Boolean =
    Build.VERSION.SDK_INT < minSdk ||
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
