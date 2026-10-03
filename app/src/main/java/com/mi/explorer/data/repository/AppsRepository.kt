package com.mi.explorer.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mi.explorer.data.model.AppInfoItem
import java.io.File

class AppsRepository(private val context: Context) {

    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<AppInfoItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val result = mutableListOf<AppInfoItem>()

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            if (!includeSystemApps && isSystem) {
                continue
            }

            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg.packageName
            }

            val apkSize = try {
                File(appInfo.sourceDir).length()
            } catch (e: Exception) {
                0L
            }

            val icon = try {
                pm.getApplicationIcon(appInfo)
            } catch (e: Exception) {
                null
            }

            result.add(
                AppInfoItem(
                    appName = appName,
                    packageName = pkg.packageName,
                    versionName = pkg.versionName ?: "1.0",
                    isSystemApp = isSystem,
                    apkSize = apkSize,
                    icon = icon
                )
            )
        }

        result.sortedBy { it.appName.lowercase() }
    }
}
