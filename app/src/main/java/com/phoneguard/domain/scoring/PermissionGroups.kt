package com.phoneguard.domain.scoring

import com.phoneguard.domain.model.InstallSource

/**
 * Android permission names grouped by the risk they represent.
 *
 * Plain strings are used so this file stays free of Android imports and can be unit tested.
 */
object PermissionGroups {
    val SMS = setOf(
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_MMS",
        "android.permission.RECEIVE_WAP_PUSH",
    )
    val CALL_LOG = setOf(
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS",
    )
    val MICROPHONE = setOf("android.permission.RECORD_AUDIO")
    val CAMERA = setOf("android.permission.CAMERA")
    val LOCATION = setOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
    )
    const val OVERLAY = "android.permission.SYSTEM_ALERT_WINDOW"
    const val INSTALL_PACKAGES = "android.permission.REQUEST_INSTALL_PACKAGES"
}

/** Known app-store installer package names. */
object InstallSources {
    const val PLAY_STORE = "com.android.vending"
    const val PLAY_STORE_FEEDBACK = "com.google.android.feedback"
    const val GALAXY_STORE = "com.sec.android.app.samsungapps"
    const val AMAZON_APPSTORE = "com.amazon.venezia"

    /** Classifies an installer package name. */
    fun classify(installerPackage: String?): InstallSource = when (installerPackage) {
        null, "" -> InstallSource.UNKNOWN
        PLAY_STORE, PLAY_STORE_FEEDBACK -> InstallSource.PLAY_STORE
        GALAXY_STORE -> InstallSource.GALAXY_STORE
        AMAZON_APPSTORE -> InstallSource.AMAZON_APPSTORE
        else -> InstallSource.OTHER_APP
    }
}

/** Screen-sharing and remote-control apps that scammers ask victims to install. */
object RemoteControlApps {
    private val exactPackages = setOf(
        "com.anydesk.anydeskandroid",
        "com.anydesk.adcontrol.ad1",
        "com.teamviewer.quicksupport.market",
        "com.teamviewer.teamviewer.market.mobile",
        "com.teamviewer.host.market",
        "com.sand.airdroid",
        "com.sand.airdroidbiz",
        "com.sand.airmirror",
        "com.rustdesk.rustdesk",
    )
    private val prefixes = listOf("com.anydesk.", "com.teamviewer.")

    /** True when [packageName] is a known remote-control app. */
    fun isRemoteControl(packageName: String): Boolean =
        packageName in exactPackages || prefixes.any { packageName.startsWith(it) }
}
