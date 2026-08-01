package com.jaikar.spideyos.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Optional Gemini bridge. WeaveHome works without it; with a key, Pip gets smarter replies
 * while local background actions still run on-device.
 */
class GeminiBridge(
    private val apiKeyProvider: () -> String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun hasKey(): Boolean = apiKeyProvider().trim().isNotEmpty()

    private fun systemPrompt(userName: String): String = """
        You are Spidy (SpideyDashPip), the on-phone companion inside WeaveHome by Jaikar Pothula.
        Speak warm, short, and clear — under 60 words. Address the user as $userName.
        You help via voice: open apps, meetings, notifications, device panels, and quick answers.
        PRIVACY: Never ask for or invent location, phone numbers, emails, passwords, OTP, or IDs.
        If the user wants something opened and you can't, suggest a short search phrase.
        Optional tags at the end only: [[ACTION:WATCH_MAIL]] [[ACTION:WATCH_MESSAGES]] [[ACTION:PREP_CAMERA]]
        [[ACTION:HOME_WAG]] [[ACTION:SUMMARIZE_DAY]] [[ACTION:REMEMBER]]
    """.trimIndent()

    suspend fun chat(userName: String, history: List<Pair<String, String>>, userMessage: String): String? =
        withContext(Dispatchers.IO) {
            val key = apiKeyProvider().trim()
            if (key.isEmpty()) return@withContext null
            // Never send raw numbers/emails/OTP-looking content off-device.
            val scrubbed = com.jaikar.spideyos.companion.DashPrivacyGuard.redactPii(userMessage)
            if (scrubbed.contains("[number hidden]") || scrubbed.contains("[email hidden]") ||
                scrubbed.contains("[id hidden]") || scrubbed.contains("[card hidden]")
            ) {
                return@withContext "I keep numbers and private IDs on your phone only — ask me without those details."
            }
            try {
                val contents = JSONArray()
                contents.put(
                    JSONObject().put("role", "user").put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemPrompt(userName))),
                    ),
                )
                contents.put(
                    JSONObject().put("role", "model").put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put("text", "Got it — I'm Pip on WeaveHome. Local pup + optional smarts."),
                        ),
                    ),
                )
                history.takeLast(8).forEach { (role, text) ->
                    val safe = com.jaikar.spideyos.companion.DashPrivacyGuard.redactPii(text)
                    contents.put(
                        JSONObject()
                            .put("role", if (role == "user") "user" else "model")
                            .put("parts", JSONArray().put(JSONObject().put("text", safe))),
                    )
                }
                contents.put(
                    JSONObject().put("role", "user").put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", scrubbed)),
                    ),
                )
                val body = JSONObject().put("contents", contents)
                val url =
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$key"
                val req = Request.Builder()
                    .url(url)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) return@withContext null
                    JSONObject(raw)
                        .optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        ?.takeIf { it.isNotBlank() }
                }
            } catch (_: Exception) {
                null
            }
        }
}
