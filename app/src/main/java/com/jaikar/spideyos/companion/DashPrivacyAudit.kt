package com.jaikar.spideyos.companion

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Runtime assert: Spidy never holds location / SMS / call-log style access. */
object DashPrivacyAudit {
    fun assertSafe(context: Context): List<String> {
        val pm = context.packageManager
        val pkg = context.packageName
        val bad = mutableListOf<String>()
        DashPrivacyGuard.forbiddenPermissions.forEach { perm ->
            val granted = runCatching {
                pm.checkPermission(perm, pkg) == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
            if (granted) bad += perm
        }
        // Soft check: package info requested permissions (API 23+)
        runCatching {
            val flags = PackageManager.GET_PERMISSIONS
            val info = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, flags)
            }
            info.requestedPermissions?.forEach { p ->
                if (p in DashPrivacyGuard.forbiddenPermissions) bad += "declared:$p"
            }
        }
        return bad.distinct()
    }
}
