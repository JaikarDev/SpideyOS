package com.jaikar.spideyos.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient(
    private val apiKeyProvider: () -> String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun systemPrompt(userName: String): String = """
        You are Spidey, a friendly, witty personal assistant living inside SpideyOS on $userName's Android phone.
        Speak like a heroic, encouraging web-slinger: warm, playful, concise.
        Always address the user as $userName when greeting or announcing things.
        Help with messages, mail summaries, camera tips, and daily tasks.
        Never claim to be officially Marvel or Sony. You are a fan-inspired assistant persona.
        Keep replies under 120 words unless $userName asks for detail.
        Example vibe: "Hey $userName — you've got mail. Want the short version?"
    """.trimIndent()

    suspend fun chat(userName: String, history: List<Pair<String, String>>, userMessage: String): String =
        withContext(Dispatchers.IO) {
            val key = apiKeyProvider().trim()
            if (key.isEmpty()) {
                return@withContext offlineReply(userName, userMessage)
            }
            try {
                val contents = JSONArray()
                contents.put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", systemPrompt(userName))),
                        ),
                )
                contents.put(
                    JSONObject()
                        .put("role", "model")
                        .put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put(
                                    "text",
                                    "Got it — I'm Spidey on SpideyOS. Ready to help $userName.",
                                ),
                            ),
                        ),
                )
                history.takeLast(8).forEach { (role, text) ->
                    contents.put(
                        JSONObject()
                            .put("role", if (role == "user") "user" else "model")
                            .put("parts", JSONArray().put(JSONObject().put("text", text))),
                    )
                }
                contents.put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", userMessage))),
                )

                val bodyJson = JSONObject().put("contents", contents)
                val url =
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$key"
                val req = Request.Builder()
                    .url(url)
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext "Whoa $userName — Gemini returned ${resp.code}. Check your API key. (Offline tip: ${offlineReply(userName, userMessage)})"
                    }
                    val json = JSONObject(raw)
                    val text = json
                        .optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                    text?.takeIf { it.isNotBlank() } ?: offlineReply(userName, userMessage)
                }
            } catch (e: Exception) {
                "Web fluid jam, $userName — ${e.message ?: "network error"}. ${offlineReply(userName, userMessage)}"
            }
        }

    fun offlineReply(userName: String, message: String): String {
        val m = message.lowercase()
        return when {
            m.contains("mail") || m.contains("email") ->
                "Hey $userName — you've got mail energy! Open Mail Digest and I'll summarize the stack."
            m.contains("message") || m.contains("sms") ->
                "Messages incoming like webs, $userName. Swing into Web Messages and I'll help you reply."
            m.contains("photo") || m.contains("camera") || m.contains("picture") ->
                "Nice timing, $userName — Peter Camera is ready. Snap it; I'll tell you if it's Daily Bugle material."
            m.contains("hi") || m.contains("hello") || m.contains("hey") ->
                "Hey $userName! Spidey here. Need mail, messages, or a quick pep talk?"
            else ->
                "On it, $userName! Add a Gemini API key in Settings for smarter answers — until then I'm still swinging with you."
        }
    }

    fun announceMail(userName: String): String =
        "Spidey here — $userName, you've got mail!"

    fun announceMessage(userName: String, from: String?): String =
        if (from.isNullOrBlank()) "Hey $userName — new message swinging in!"
        else "Hey $userName — message from $from just hit the web!"

    fun reactToPhoto(userName: String): String =
        "Nice shot, $userName — Daily Bugle material?"
}
