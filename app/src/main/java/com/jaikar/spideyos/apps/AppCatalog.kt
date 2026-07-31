package com.jaikar.spideyos.apps

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledApp(
    val label: String,
    val packageName: String,
    val icon: Bitmap?,
)

object AppCatalog {
    fun loadLaunchableApps(pm: PackageManager): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolved
            .mapNotNull { info ->
                val label = info.loadLabel(pm)?.toString() ?: return@mapNotNull null
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val icon = runCatching {
                    info.loadIcon(pm)?.toBitmap(192, 192)
                }.getOrNull()
                InstalledApp(label = label, packageName = pkg, icon = icon)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun filter(apps: List<InstalledApp>, query: String): List<InstalledApp> {
        val q = query.trim()
        if (q.isEmpty()) return apps.take(24)
        return apps.filter { it.label.contains(q, ignoreCase = true) }.take(24)
    }
}
