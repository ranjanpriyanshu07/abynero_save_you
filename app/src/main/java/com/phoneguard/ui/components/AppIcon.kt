package com.phoneguard.ui.components

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val iconCache = LruCache<String, ImageBitmap>(96)

/**
 * Shows an installed app's icon, loaded off the main thread and cached. Falls back to a
 * generic Android icon if it cannot be loaded (for example after uninstall).
 */
@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val cacheKey = "$packageName@$sizePx"
    val bitmap by produceState<ImageBitmap?>(initialValue = iconCache.get(cacheKey), cacheKey) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                try {
                    context.packageManager.getApplicationIcon(packageName)
                        .toBitmap(sizePx, sizePx)
                        .asImageBitmap()
                        .also { iconCache.put(cacheKey, it) }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    val shape = RoundedCornerShape(size * 0.28f)
    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(shape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}
