package com.jaikar.spideyos.companion

/** Friendly package → spoken app name. */
object DashNotifyApps {
    data class Kind(val type: Type, val appName: String)

    enum class Type { MAIL, MESSAGE, SOCIAL, OTHER }

    fun classify(pkg: String): Kind? {
        val p = pkg.lowercase()
        return when {
            p.contains("gmail") || p.contains("outlook") || p.contains("yahoo.mobile.client.android.mail") ||
                p.contains("samsung.android.email") || p.endsWith(".mail") && !p.contains("gmail") ->
                Kind(Type.MAIL, pretty(pkg, "Mail"))
            p.contains("whatsapp") -> Kind(Type.MESSAGE, "WhatsApp")
            p.contains("instagram") -> Kind(Type.MESSAGE, "Instagram")
            p.contains("telegram") -> Kind(Type.MESSAGE, "Telegram")
            p.contains("signal") -> Kind(Type.MESSAGE, "Signal")
            p.contains("messenger") || p.contains("facebook.orca") -> Kind(Type.MESSAGE, "Messenger")
            p.contains("snapchat") -> Kind(Type.MESSAGE, "Snapchat")
            p.contains("discord") -> Kind(Type.MESSAGE, "Discord")
            p.contains("twitter") || p.contains("tweet") -> Kind(Type.SOCIAL, "X")
            p.contains("tiktok") || p.contains("musically") -> Kind(Type.SOCIAL, "TikTok")
            p.contains("messaging") || p.contains(".mms") || p.contains("samsung.android.messaging") ||
                p.contains("google.android.apps.messaging") -> Kind(Type.MESSAGE, "Messages")
            else -> null
        }
    }

    fun speakLine(userName: String, kind: Kind, from: String?): String {
        val who = DashPrivacyGuard.redactPii(from)
            .takeIf { it.isNotBlank() && !it.equals(kind.appName, true) && !it.contains("hidden") }
        val line = when (kind.type) {
            Type.MAIL -> if (who != null) {
                "Hey bud — you got mail from $who, $userName!"
            } else {
                "Hey bud — you got mail, $userName!"
            }
            Type.MESSAGE, Type.SOCIAL -> if (who != null) {
                "Hey bud — you got a message from $who on ${kind.appName}, $userName!"
            } else {
                "Hey bud — you got a message from ${kind.appName}, $userName!"
            }
            Type.OTHER -> "Hey bud — something new just popped up, $userName!"
        }
        return DashPrivacyGuard.safeSpeakLine(line)
    }

    private fun pretty(pkg: String, fallback: String): String {
        return when {
            pkg.contains("gmail", true) -> "Gmail"
            pkg.contains("outlook", true) -> "Outlook"
            else -> fallback
        }
    }
}
