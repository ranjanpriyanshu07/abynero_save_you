package com.phoneguard.ui.screens.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phoneguard.R
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val body: Int,
)

private val Pages = listOf(
    OnboardingPage(Icons.Rounded.Shield, R.string.onboarding_1_title, R.string.onboarding_1_body),
    OnboardingPage(Icons.AutoMirrored.Rounded.ManageSearch, R.string.onboarding_2_title, R.string.onboarding_2_body),
    OnboardingPage(Icons.Rounded.Lock, R.string.onboarding_3_title, R.string.onboarding_3_body),
)

/** Three short pages that explain what PhoneGuard does. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { Pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == Pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), MaterialTheme.colorScheme.background),
                ),
            )
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!isLast) {
                TextButton(onClick = onFinished) { Text(stringResource(R.string.action_skip)) }
            } else {
                Spacer(Modifier.height(48.dp))
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { index -> PageContent(Pages[index]) }

        PageIndicator(count = Pages.size, current = pagerState.currentPage)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (isLast) onFinished() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                stringResource(if (isLast) R.string.action_get_started else R.string.action_next),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    val transition = rememberInfiniteTransition(label = "float")
    val float by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1_800), RepeatMode.Reverse),
        label = "scale",
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            Box(
                Modifier
                    .size(240.dp)
                    .scale(float)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), CircleShape),
            )
            Box(
                Modifier
                    .size(180.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .background(
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(page.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(64.dp))
            }
        }
        Spacer(Modifier.height(40.dp))
        Text(
            stringResource(page.title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(page.body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int) {
    val description = stringResource(R.string.onboarding_page_of, current + 1, count)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 28.dp else 10.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "dotColor",
            )
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .height(10.dp)
                    .width(width)
                    .background(color, RoundedCornerShape(5.dp)),
            )
        }
    }
}
