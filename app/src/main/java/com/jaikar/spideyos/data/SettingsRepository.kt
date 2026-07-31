package com.jaikar.spideyos.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jaikar.spideyos.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("spidey_settings")

data class SpideySettings(
    val userName: String = "Jaikar",
    val geminiApiKey: String = "",
    val themeIntensity: Float = 0.85f,
    val onboardingDone: Boolean = false,
    val messagesEnabled: Boolean = true,
    val mailEnabled: Boolean = true,
    val cameraEnabled: Boolean = true,
    val spideyVoiceEnabled: Boolean = true,
    val imapHost: String = "imap.gmail.com",
    val imapEmail: String = "",
    val imapPassword: String = "",
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val GEMINI_KEY = stringPreferencesKey("gemini_key")
        val THEME = floatPreferencesKey("theme_intensity")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val MESSAGES = booleanPreferencesKey("messages_enabled")
        val MAIL = booleanPreferencesKey("mail_enabled")
        val CAMERA = booleanPreferencesKey("camera_enabled")
        val VOICE = booleanPreferencesKey("spidey_voice_enabled")
        val IMAP_HOST = stringPreferencesKey("imap_host")
        val IMAP_EMAIL = stringPreferencesKey("imap_email")
        val IMAP_PASS = stringPreferencesKey("imap_pass")
    }

    val settings: Flow<SpideySettings> = context.dataStore.data.map { p ->
        SpideySettings(
            userName = p[Keys.USER_NAME] ?: "Jaikar",
            geminiApiKey = p[Keys.GEMINI_KEY] ?: BuildConfig.GEMINI_API_KEY,
            themeIntensity = p[Keys.THEME] ?: 0.85f,
            onboardingDone = p[Keys.ONBOARDING] ?: false,
            messagesEnabled = p[Keys.MESSAGES] ?: true,
            mailEnabled = p[Keys.MAIL] ?: true,
            cameraEnabled = p[Keys.CAMERA] ?: true,
            spideyVoiceEnabled = p[Keys.VOICE] ?: true,
            imapHost = p[Keys.IMAP_HOST] ?: "imap.gmail.com",
            imapEmail = p[Keys.IMAP_EMAIL] ?: "",
            imapPassword = p[Keys.IMAP_PASS] ?: "",
        )
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name.ifBlank { "Jaikar" } }
    }

    suspend fun setGeminiKey(key: String) {
        context.dataStore.edit { it[Keys.GEMINI_KEY] = key.trim() }
    }

    suspend fun setThemeIntensity(value: Float) {
        context.dataStore.edit { it[Keys.THEME] = value.coerceIn(0.3f, 1f) }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING] = done }
    }

    suspend fun setMessagesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MESSAGES] = enabled }
    }

    suspend fun setMailEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MAIL] = enabled }
    }

    suspend fun setCameraEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CAMERA] = enabled }
    }

    suspend fun setSpideyVoiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE] = enabled }
    }

    suspend fun setImap(host: String, email: String, password: String) {
        context.dataStore.edit {
            it[Keys.IMAP_HOST] = host.trim().ifBlank { "imap.gmail.com" }
            it[Keys.IMAP_EMAIL] = email.trim()
            it[Keys.IMAP_PASS] = password
        }
    }
}
