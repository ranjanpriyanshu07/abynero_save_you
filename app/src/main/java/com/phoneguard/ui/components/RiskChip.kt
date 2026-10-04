package com.phoneguard.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.ui.util.containerColor
import com.phoneguard.ui.util.icon
import com.phoneguard.ui.util.labelRes
import com.phoneguard.ui.util.onContainerColor

/** Colour-coded pill showing a risk level, e.g. "High risk". */
@Composable
fun RiskChip(
    level: RiskLevel,
    modifier: Modifier = Modifier,
    text: String = stringResource(level.labelRes),
) {
    Surface(
        modifier = modifier,
        color = level.containerColor(),
        contentColor = level.onContainerColor(),
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(level.icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}
