package com.phoneguard.ui.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.ui.graphics.vector.ImageVector
import com.phoneguard.R

/** Navigation routes. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val PERMISSIONS = "permissions"
    const val HOME = "home"
    const val SCAN = "scan"
    const val TOOLS = "tools"
    const val HISTORY = "history"
    const val TIPS = "tips"
    const val APP_DETAIL = "app/{packageName}"
    const val LINK_CHECK = "link_check?openQr={openQr}"
    const val QR_SCANNER = "qr_scanner"
    const val WIFI = "wifi"

    const val ARG_OPEN_QR = "openQr"

    /** Key used to hand text (shared or scanned) to the link checker. */
    const val KEY_INCOMING_TEXT = "incoming_text"

    fun appDetail(packageName: String) = "app/${Uri.encode(packageName)}"
    fun linkCheck(openQr: Boolean = false) = "link_check?openQr=$openQr"
}

/** The five tabs in the bottom navigation bar. */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home),
    SCAN(Routes.SCAN, R.string.nav_scan, Icons.Rounded.Radar, Icons.Outlined.Radar),
    TOOLS(Routes.TOOLS, R.string.nav_tools, Icons.Rounded.Build, Icons.Outlined.Build),
    HISTORY(Routes.HISTORY, R.string.nav_history, Icons.Rounded.History, Icons.Outlined.History),
    TIPS(Routes.TIPS, R.string.nav_tips, Icons.Rounded.Lightbulb, Icons.Outlined.Lightbulb),
}
