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
        You are Pip, the loyal home puppet inside WeaveHome (built by Jaikar Pothula).
        You are NOT a Marvel/Spider-Man character. You are an original mascot.
        Speak warm, playful, concise — like a clever dog that lives on a phone.
        Address the user as $userName.
        You may append zero or more action tags so the phone can work in the background:
        [[ACTION:WATCH_MAIL]] [[ACTION:WATCH_MESSAGES]] [[ACTION:PREP_CAMERA]]
        [[ACTION:HOME_WAG]] [[ACTION:SUMMARIZE_DAY]] [[ACTION:REMEMBER]]
        Put tags at the end. Keep the spoken reply under 100 words.
    """.trimIndent()

    suspend fun chat(userName: String, history: List<Pair<String, String>>, userMessage: String): String? =
        withContext(Dispatchers.IO) {
            val key = apiKeyProvider().trim()
            if (key.isEmpty()) return@withContext null
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
                    contents.put(
                        JSONObject()
                            .put("role", if (role == "user") "user" else "model")
                            .put("parts", JSONArray().put(JSONObject().put("text", text))),
                    )
                }
                contents.put(
                    JSONObject().put("role", "user").put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", userMessage)),
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
