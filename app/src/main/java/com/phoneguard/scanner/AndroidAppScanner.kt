package com.phoneguard.scanner

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import com.phoneguard.di.IoDispatcher
import com.phoneguard.domain.model.AppRiskReport
import com.phoneguard.domain.model.AppScanInput
import com.phoneguard.domain.scoring.AppRiskScorer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Reads installed apps from [PackageManager] and scores them with [AppRiskScorer].
 *
 * Every package is read in its own try/catch so one broken app can never stop the scan.
 */
class AndroidAppScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scorer: AppRiskScorer,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AppScanner {

    private val packageManager: PackageManager get() = context.packageManager

    override suspend fun scanInstalledApps(onProgress: (done: Int, total: Int) -> Unit): List<AppRiskReport> =
        withContext(ioDispatcher) {
            val packages = userInstalledPackageNames()
            val access = loadSpecialAccess()
            val reports = ArrayList<AppRiskReport>(packages.size)
            onProgress(0, packages.size)
            packages.forEachIndexed { index, packageName ->
                ensureActive()
                buildReport(packageName, access)?.let(reports::add)
                onProgress(index + 1, packages.size)
            }
            reports.sortedWith(
                compareByDescending<AppRiskReport> { it.level }
                    .thenByDescending { it.assessment.points }
                    .thenBy { it.appName.lowercase() },
            )
        }

    override suspend fun scanApp(packageName: String): AppRiskReport? = withContext(ioDispatcher) {
        buildReport(packageName, loadSpecialAccess())
    }

    /** Names of apps the user installed (system apps and PhoneGuard itself are skipped). */
    private fun userInstalledPackageNames(): List<String> {
        val apps: List<ApplicationInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }
        } catch (e: RuntimeException) {
            emptyList()
        }
        return apps
            .filter { it.packageName != context.packageName && !it.isSystemApp() }
            .map { it.packageName }
    }

    private fun buildReport(packageName: String, access: SpecialAccess): AppRiskReport? {
        val info = packageInfo(packageName) ?: return null
        val appInfo = info.applicationInfo ?: return null
        if (appInfo.isSystemApp() || packageName == context.packageName) return null
        return try {
            val requested = info.requestedPermissions?.toList().orEmpty()
            val flags = info.requestedPermissionsFlags
            val granted = requested.filterIndexed { index, _ ->
                flags != null && index < flags.size && (flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            }
            val installer = installerOf(packageName)
            val input = AppScanInput(
                packageName = packageName,
                grantedPermissions = granted.toSet(),
                requestedPermissions = requested.toSet(),
                installerPackage = installer,
                hasEnabledAccessibilityService = packageName in access.accessibility,
                isActiveDeviceAdmin = packageName in access.deviceAdmins,
                hasNotificationAccess = packageName in access.notificationListeners,
            )
            AppRiskReport(
                packageName = packageName,
                appName = labelOf(appInfo, packageName),
                versionName = info.versionName,
                installerPackage = installer,
                firstInstallTime = info.firstInstallTime,
                assessment = scorer.assess(input),
            )
        } catch (e: RuntimeException) {
            null
        }
    }

    private fun packageInfo(packageName: String): PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }
    } catch (e: PackageManager.NameNotFoundException) {
        null
    } catch (e: RuntimeException) {
        null
    }

    private fun installerOf(packageName: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        }
    } catch (e: PackageManager.NameNotFoundException) {
        null
    } catch (e: RuntimeException) {
        null
    }

    private fun labelOf(appInfo: ApplicationInfo, fallback: String): String = try {
        appInfo.loadLabel(packageManager).toString().ifBlank { fallback }
    } catch (e: RuntimeException) {
        fallback
    }

    private fun ApplicationInfo.isSystemApp(): Boolean = (flags and ApplicationInfo.FLAG_SYSTEM) != 0

    /** Apps holding powerful special access that is not a normal permission. */
    private data class SpecialAccess(
        val accessibility: Set<String>,
        val deviceAdmins: Set<String>,
        val notificationListeners: Set<String>,
    )

    private fun loadSpecialAccess(): SpecialAccess = SpecialAccess(
        accessibility = enabledAccessibilityPackages(),
        deviceAdmins = activeDeviceAdmins(),
        notificationListeners = notificationListenerPackages(),
    )

    private fun enabledAccessibilityPackages(): Set<String> {
        val fromManager = try {
            context.getSystemService(AccessibilityManager::class.java)
                ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                ?.mapNotNull { it.resolveInfo?.serviceInfo?.packageName }
                ?.toSet()
                .orEmpty()
        } catch (e: RuntimeException) {
            emptySet()
        }
        val fromSettings = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                .orEmpty()
                .split(':')
                .mapNotNull { ComponentName.unflattenFromString(it)?.packageName }
                .toSet()
        } catch (e: RuntimeException) {
            emptySet()
        }
        return fromManager + fromSettings
    }

    private fun activeDeviceAdmins(): Set<String> = try {
        context.getSystemService(DevicePolicyManager::class.java)
            ?.activeAdmins
            ?.map { it.packageName }
            ?.toSet()
            .orEmpty()
    } catch (e: RuntimeException) {
        emptySet()
    }

    private fun notificationListenerPackages(): Set<String> = try {
        NotificationManagerCompat.getEnabledListenerPackages(context)
    } catch (e: RuntimeException) {
        emptySet()
    }
}
