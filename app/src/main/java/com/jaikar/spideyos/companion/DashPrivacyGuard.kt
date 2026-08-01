package com.jaikar.spideyos.companion

/**
 * SpideyDashPip privacy chain — on-device only.
 * Blocks location, phone numbers, emails, hidden files, and sensitive filenames.
 * Nothing here is uploaded by WeaveHome; Spidy memory stays in local app storage.
 */
object DashPrivacyGuard {
    private val phoneRegex = Regex(
        """(?<!\d)(?:\+?\d[\d\s().-]{7,}\d)(?!\d)""",
    )
    private val emailRegex = Regex(
        """[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}""",
        RegexOption.IGNORE_CASE,
    )
    private val ssnLike = Regex("""\b\d{3}-\d{2}-\d{4}\b""")
    private val cardLike = Regex("""\b(?:\d[ -]*?){13,19}\b""")

    private val sensitiveNameBits = listOf(
        "password", "passwd", "secret", "private", "wallet", "seed", "mnemonic",
        "2fa", "otp", "keystore", "token", "credential", "bank", "ssn", "passport",
        "tax", "medical", "health", "id_card", "aadhaar", "aadhar", "pan_card",
        ".pem", ".key", ".p12", ".pfx", ".kdbx", ".ovpn",
    )

    private val blockedPathBits = listOf(
        "/.", "/Android/data/", "/Android/obb/", "lost+found",
    )

    /** Never declare or request these — enforced in code + manifest removals. */
    val forbiddenPermissions = setOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.READ_PHONE_NUMBERS",
        "android.permission.READ_PHONE_STATE",
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.ACCESS_MEDIA_LOCATION",
    )

    fun redactPii(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var t = raw
        t = emailRegex.replace(t, "[email hidden]")
        t = phoneRegex.replace(t, "[number hidden]")
        t = ssnLike.replace(t, "[id hidden]")
        t = cardLike.replace(t) { m ->
            if (m.value.replace(Regex("""[\s-]"""), "").length >= 13) "[card hidden]" else m.value
        }
        return t.trim()
    }

    fun containsPhoneOrEmail(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        return phoneRegex.containsMatchIn(raw) || emailRegex.containsMatchIn(raw)
    }

    fun isHiddenOrSensitiveFile(displayName: String?, pathOrUri: String? = null): Boolean {
        val name = displayName?.trim().orEmpty()
        if (name.isEmpty()) return true
        if (name.startsWith(".")) return true
        val lower = name.lowercase()
        if (sensitiveNameBits.any { lower.contains(it) }) return true
        val path = pathOrUri?.lowercase().orEmpty()
        if (path.isNotEmpty() && blockedPathBits.any { path.contains(it.lowercase()) }) return true
        // Android “hidden” style prefixes
        if (lower.startsWith(".")) return true
        return false
    }

    fun safeSpeakLine(raw: String): String = redactPii(raw)

    fun safeContactName(name: String?): String? {
        val n = name?.trim()?.takeIf { it.isNotBlank() } ?: return null
        // Contacts are names only — drop anything that looks like a number/email blob
        if (containsPhoneOrEmail(n)) return redactPii(n).ifBlank { null }
        if (n.all { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }) {
            return null
        }
        return n.take(60)
    }

    fun privacyPledge(userName: String): String =
        "Hey $userName — Spidy stays on your phone. No location. No numbers. " +
            "No hidden or sensitive files. Mail & apps stay between you and this device."
}
