package com.phoneguard.scanner

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.phoneguard.di.IoDispatcher
import com.phoneguard.domain.device.DeviceSettingsEvaluator
import com.phoneguard.domain.model.DeviceCheckResult
import com.phoneguard.domain.model.DeviceSnapshot
import com.phoneguard.domain.scoring.PermissionGroups
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Reads security-related device settings and evaluates them with [DeviceSettingsEvaluator].
 */
class AndroidDeviceSettingsScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val evaluator: DeviceSettingsEvaluator,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DeviceSettingsScanner {

    override suspend fun scan(): List<DeviceCheckResult> = withContext(ioDispatcher) {
        evaluator.evaluate(snapshot())
    }

    private fun snapshot(): DeviceSnapshot {
        val (installers, confirmed) = appsThatCanInstallApps()
        return DeviceSnapshot(
            isDeviceSecure = isDeviceSecure(),
            developerOptionsEnabled = globalInt(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED) == 1,
            usbDebuggingEnabled = globalInt(Settings.Global.ADB_ENABLED) == 1,
            unknownSourceApps = installers,
            unknownSourcesConfirmed = confirmed,
            securityPatch = Build.VERSION.SECURITY_PATCH,
        )
    }

    private fun isDeviceSecure(): Boolean = try {
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure ?: true
    } catch (e: RuntimeException) {
        // If we cannot tell, do not scare the user.
        true
    }

    private fun globalInt(name: String): Int = try {
        Settings.Global.getInt(context.contentResolver, name, 0)
    } catch (e: RuntimeException) {
        0
    }

    /**
     * User apps that asked for "install unknown apps".
     *
     * Android usually does not let us read whether another app was actually allowed, so we
     * try the app-op check and fall back to listing apps that *asked* for the ability.
     *
     * @return app names and whether the list was confirmed by the system.
     */
    private fun appsThatCanInstallApps(): Pair<List<String>, Boolean> {
        val pm = context.packageManager
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val userApps = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }
        } catch (e: RuntimeException) {
            emptyList()
        }.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName }

        // Read each app separately so one unreadable package cannot break the whole check.
        val candidates = userApps.mapNotNull { appInfo ->
            val requested = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(
                        appInfo.packageName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS)
                }.requestedPermissions
            } catch (e: PackageManager.NameNotFoundException) {
                null
            } catch (e: RuntimeException) {
                null
            }
            if (requested?.contains(PermissionGroups.INSTALL_PACKAGES) == true) appInfo else null
        }

        var confirmedAny = false
        var checkFailed = false
        val allowed = candidates.filter { info ->
            val uid = info.uid
            val mode = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps?.unsafeCheckOpNoThrow(OP_REQUEST_INSTALL_PACKAGES, uid, info.packageName)
                } else {
                    @Suppress("DEPRECATION")
                    appOps?.checkOpNoThrow(OP_REQUEST_INSTALL_PACKAGES, uid, info.packageName)
                }
            } catch (e: RuntimeException) {
                checkFailed = true
                null
            }
            when (mode) {
                AppOpsManager.MODE_ALLOWED -> {
                    confirmedAny = true
                    true
                }
                AppOpsManager.MODE_IGNORED, AppOpsManager.MODE_ERRORED -> false
                else -> true
            }
        }
        val names = allowed.map { info ->
            try {
                info.loadLabel(pm).toString()
            } catch (e: RuntimeException) {
                info.packageName
            }
        }.sorted()
        return names to (confirmedAny && !checkFailed)
    }

    private companion object {
        const val OP_REQUEST_INSTALL_PACKAGES = "android:request_install_packages"
    }
}
