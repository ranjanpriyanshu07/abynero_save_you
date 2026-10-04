package com.phoneguard.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phoneguard.R
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.ui.util.color

/**
 * Expandable result card with a colour stripe for the risk level, a clear Fix button and,
 * when expanded, the plain-language reasons.
 *
 * @param leading icon or app icon shown on the left.
 * @param fixLabel text of the main action button; hidden when null.
 */
@Composable
fun ResultCard(
    title: String,
    subtitle: String?,
    level: RiskLevel,
    leading: @Composable () -> Unit,
    reasons: List<String>,
    modifier: Modifier = Modifier,
    advice: String? = null,
    fixLabel: String? = null,
    onFix: () -> Unit = {},
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
    initiallyExpanded: Boolean = false,
    showChip: Boolean = true,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    val canExpand = reasons.isNotEmpty() || advice != null
    val expandedText = stringResource(R.string.state_expanded)
    val collapsedText = stringResource(R.string.state_collapsed)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(level.color()),
            )
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (canExpand) {
                                Modifier
                                    .clickable(role = Role.Button) { expanded = !expanded }
                                    .semantics { stateDescription = if (expanded) expandedText else collapsedText }
                            } else {
                                Modifier
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leading()
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (showChip) {
                            Spacer(Modifier.height(6.dp))
                            RiskChip(level)
                        }
                    }
                    if (canExpand) {
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = stringResource(if (expanded) R.string.cd_collapse else R.string.cd_expand),
                            modifier = Modifier
                                .size(28.dp)
                                .rotate(rotation),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = expanded && canExpand,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reasons.forEach { reason -> ReasonRow(reason, level) }
                        if (advice != null) {
                            Text(
                                text = advice,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                            )
                        }
                    }
                }

                if (fixLabel != null || secondaryLabel != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (fixLabel != null) {
                            Button(
                                onClick = onFix,
                                colors = if (level == RiskLevel.HIGH) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                    )
                                } else {
                                    ButtonDefaults.buttonColors()
                                },
                                shape = RoundedCornerShape(16.dp),
                            ) { Text(fixLabel) }
                        }
                        if (secondaryLabel != null) {
                            OutlinedButton(onClick = onSecondary, shape = RoundedCornerShape(16.dp)) {
                                Text(secondaryLabel)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One bullet line explaining a reason. */
@Composable
fun ReasonRow(text: String, level: RiskLevel, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 8.dp)
                .size(8.dp)
                .background(level.color(), CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Round tinted icon used as the leading element of cards. */
@Composable
fun TintedIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    level: RiskLevel,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(level.color().copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = level.color(), modifier = Modifier.size(24.dp))
    }
}
