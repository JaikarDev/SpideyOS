package com.jaikar.spideyos.mail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Folder
import javax.mail.Session

data class ImapConfig(
    val host: String,
    val port: Int = 993,
    val email: String,
    val password: String,
)

/**
 * Fetches recent inbox messages over IMAP (Gmail: imap.gmail.com + app password).
 */
object ImapMailFetcher {
    suspend fun fetchRecent(config: ImapConfig, limit: Int = 8): Result<List<MailItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(config.host.isNotBlank() && config.email.isNotBlank() && config.password.isNotBlank()) {
                    "IMAP host, email, and password required"
                }
                val props = Properties().apply {
                    put("mail.store.protocol", "imaps")
                    put("mail.imaps.host", config.host)
                    put("mail.imaps.port", config.port.toString())
                    put("mail.imaps.ssl.enable", "true")
                    put("mail.imaps.ssl.trust", "*")
                }
                val session = Session.getInstance(props)
                val store = session.getStore("imaps")
                try {
                    store.connect(config.host, config.port, config.email, config.password)
                    val inbox = store.getFolder("INBOX")
                    try {
                        inbox.open(Folder.READ_ONLY)
                        val end = inbox.messageCount
                        if (end == 0) return@runCatching emptyList()
                        val start = (end - limit + 1).coerceAtLeast(1)
                        inbox.getMessages(start, end).reversed().map { msg ->
                            val from = msg.from?.firstOrNull()?.toString()?.substringBefore("<")?.trim()
                                ?: msg.from?.firstOrNull()?.toString()
                                ?: "Unknown"
                            val subject = msg.subject ?: "(no subject)"
                            val preview = try {
                                when (val content = msg.content) {
                                    is String -> content.take(120).replace("\n", " ")
                                    else -> "Open in your mail app for the full message."
                                }
                            } catch (_: Exception) {
                                "Open in your mail app for the full message."
                            }
                            MailItem(from = from, subject = subject, preview = preview, unread = true)
                        }
                    } finally {
                        if (inbox.isOpen) inbox.close(false)
                    }
                } finally {
                    store.close()
                }
            }
        }

    fun gmailDefaults(email: String, appPassword: String) = ImapConfig(
        host = "imap.gmail.com",
        port = 993,
        email = email,
        password = appPassword.replace(" ", ""),
    )
}
