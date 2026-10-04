package com.phoneguard.ui.util

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.phoneguard.domain.model.DeviceCheckType
import com.phoneguard.domain.model.SuggestedAction

/**
 * Opens the exact Settings screen for each fix, falling back to more general screens when
 * a phone maker has removed or renamed the specific one.
 */
object SettingsLauncher {

    /** Opens the screen that fixes [type]. Returns false if nothing could be opened. */
    fun openFor(context: Context, type: DeviceCheckType): Boolean = launchFirst(context, intentsFor(context, type))

    /** Opens the right screen for an app's [action]. */
    fun openForApp(context: Context, packageName: String, action: SuggestedAction): Boolean = when (action) {
        SuggestedAction.UNINSTALL_NOW, SuggestedAction.UNINSTALL_IF_UNKNOWN -> uninstall(context, packageName)
        SuggestedAction.TURN_OFF_ACCESSIBILITY -> launchFirst(
            context,
            listOf(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), appDetailsIntent(packageName)),
        )
        SuggestedAction.REMOVE_DEVICE_ADMIN -> launchFirst(
            context,
            listOf(
                Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings")),
                Intent(Settings.ACTION_SECURITY_SETTINGS),
                appDetailsIntent(packageName),
            ),
        )
        SuggestedAction.NO_ACTION, SuggestedAction.REVIEW_PERMISSIONS -> openAppSettings(context, packageName)
    }

    /** Opens Android's "App info" page for [packageName]. */
    fun openAppSettings(context: Context, packageName: String): Boolean =
        launchFirst(context, listOf(appDetailsIntent(packageName), Intent(Settings.ACTION_APPLICATION_SETTINGS)))

    /** Asks Android to uninstall [packageName]; the user confirms in a system dialog. */
    fun uninstall(context: Context, packageName: String): Boolean = launchFirst(
        context,
        listOf(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")), appDetailsIntent(packageName)),
    )

    /** Opens Wi-Fi settings. */
    fun openWifiSettings(context: Context): Boolean =
        launchFirst(context, listOf(Intent(Settings.ACTION_WIFI_SETTINGS), Intent(Settings.ACTION_SETTINGS)))

    /** Opens this app's own settings (used when a permission was permanently denied). */
    fun openOwnAppSettings(context: Context): Boolean = openAppSettings(context, context.packageName)

    /** Starts the phone dialer with [number] filled in. No call permission is needed. */
    fun dial(context: Context, number: String): Boolean =
        launchFirst(context, listOf(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))))

    /** Opens [url] in the browser. */
    fun openUrl(context: Context, url: String): Boolean =
        launchFirst(context, listOf(Intent(Intent.ACTION_VIEW, Uri.parse(url))))

    private fun intentsFor(context: Context, type: DeviceCheckType): List<Intent> = when (type) {
        DeviceCheckType.SCREEN_LOCK -> listOf(
            Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD),
            Intent(Settings.ACTION_SECURITY_SETTINGS),
        )
        DeviceCheckType.DEVELOPER_OPTIONS, DeviceCheckType.USB_DEBUGGING -> listOf(
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
            Intent(Settings.ACTION_DEVICE_INFO_SETTINGS),
        )
        DeviceCheckType.UNKNOWN_SOURCES -> listOf(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES),
            Intent(Settings.ACTION_SECURITY_SETTINGS),
        )
        DeviceCheckType.SECURITY_PATCH -> listOf(
            Intent(ACTION_SYSTEM_UPDATE_SETTINGS),
            Intent(Settings.ACTION_DEVICE_INFO_SETTINGS),
        )
        DeviceCheckType.PLAY_PROTECT -> listOfNotNull(
            Intent().setClassName(GMS_PACKAGE, PLAY_PROTECT_ACTIVITY),
            context.packageManager.getLaunchIntentForPackage(PLAY_STORE_PACKAGE),
            Intent(Settings.ACTION_SECURITY_SETTINGS),
        )
    } + Intent(Settings.ACTION_SETTINGS)

    private fun appDetailsIntent(packageName: String) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))

    private fun launchFirst(context: Context, intents: List<Intent>): Boolean {
        val needsNewTask = context.findActivity() == null
        for (intent in intents) {
            try {
                if (needsNewTask) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: ActivityNotFoundException) {
                continue
            } catch (e: SecurityException) {
                continue
            }
        }
        return false
    }

    private const val ACTION_SYSTEM_UPDATE_SETTINGS = "android.settings.SYSTEM_UPDATE_SETTINGS"
    private const val GMS_PACKAGE = "com.google.android.gms"
    private const val PLAY_PROTECT_ACTIVITY = "com.google.android.gms.security.settings.VerifyAppsSettingsActivity"
    private const val PLAY_STORE_PACKAGE = "com.android.vending"
}

/** Finds the hosting Activity, unwrapping context wrappers. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
