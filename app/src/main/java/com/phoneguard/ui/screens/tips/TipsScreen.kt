@file:OptIn(ExperimentalMaterial3Api::class)

package com.phoneguard.ui.screens.tips

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.automirrored.rounded.ScreenShare
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.phoneguard.R
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.ui.components.ResultCard
import com.phoneguard.ui.components.TabTopBar
import com.phoneguard.ui.components.TintedIcon
import com.phoneguard.ui.util.SettingsLauncher
import com.phoneguard.ui.util.containerColor
import com.phoneguard.ui.util.onContainerColor

/** A short safety tip about a common scam. */
private data class Tip(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val summary: Int,
    @ArrayRes val steps: Int,
    val level: RiskLevel,
)

private val Tips = listOf(
    Tip(Icons.Rounded.VerifiedUser, R.string.tip_kyc_title, R.string.tip_kyc_summary, R.array.tip_kyc_steps, RiskLevel.HIGH),
    Tip(Icons.Rounded.Gavel, R.string.tip_arrest_title, R.string.tip_arrest_summary, R.array.tip_arrest_steps, RiskLevel.HIGH),
    Tip(Icons.Rounded.CurrencyRupee, R.string.tip_upi_title, R.string.tip_upi_summary, R.array.tip_upi_steps, RiskLevel.HIGH),
    Tip(Icons.Rounded.LocalShipping, R.string.tip_delivery_title, R.string.tip_delivery_summary, R.array.tip_delivery_steps, RiskLevel.MEDIUM),
    Tip(Icons.AutoMirrored.Rounded.ScreenShare, R.string.tip_screen_title, R.string.tip_screen_summary, R.array.tip_screen_steps, RiskLevel.HIGH),
    Tip(Icons.Rounded.Password, R.string.tip_otp_title, R.string.tip_otp_summary, R.array.tip_otp_steps, RiskLevel.MEDIUM),
    Tip(Icons.Rounded.SupportAgent, R.string.tip_helpline_title, R.string.tip_helpline_summary, R.array.tip_helpline_steps, RiskLevel.MEDIUM),
)

/** Safety tips about common Indian scams, plus the national cyber-fraud helpline. */
@Composable
fun TipsScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { TabTopBar(stringResource(R.string.tips_title), scrollBehavior) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "helpline") { HelplineCard() }
            items(Tips, key = { it.title }) { tip ->
                ResultCard(
                    title = stringResource(tip.title),
                    subtitle = stringResource(tip.summary),
                    level = tip.level,
                    leading = { TintedIcon(tip.icon, tip.level) },
                    reasons = stringArrayResource(tip.steps).toList(),
                    showChip = false,
                )
            }
        }
    }
}

@Composable
private fun HelplineCard() {
    val context = LocalContext.current
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = RiskLevel.HIGH.containerColor(),
            contentColor = RiskLevel.HIGH.onContainerColor(),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.helpline_title), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.helpline_body), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { SettingsLauncher.dial(context, HELPLINE_NUMBER) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.helpline_call))
                }
                OutlinedButton(
                    onClick = { SettingsLauncher.openUrl(context, REPORT_URL) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) {
                    Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.helpline_report))
                }
            }
        }
    }
}

private const val HELPLINE_NUMBER = "1930"
private const val REPORT_URL = "https://cybercrime.gov.in"
