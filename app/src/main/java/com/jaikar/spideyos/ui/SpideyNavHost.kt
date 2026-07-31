package com.jaikar.spideyos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jaikar.spideyos.SpideyApp
import com.jaikar.spideyos.ui.assistant.AssistantScreen
import com.jaikar.spideyos.ui.camera.PeterCameraScreen
import com.jaikar.spideyos.ui.launcher.LauncherScreen
import com.jaikar.spideyos.ui.mail.MailDigestScreen
import com.jaikar.spideyos.ui.messages.WebMessagesScreen
import com.jaikar.spideyos.ui.onboarding.OnboardingScreen
import com.jaikar.spideyos.ui.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val LAUNCHER = "launcher"
    const val ASSISTANT = "assistant"
    const val MESSAGES = "messages"
    const val MAIL = "mail"
    const val CAMERA = "camera"
    const val SETTINGS = "settings"
}

@Composable
fun SpideyNavHost() {
    val context = LocalContext.current
    val settingsRepo = (context.applicationContext as SpideyApp).settings
    val settings by settingsRepo.settings.collectAsState(
        initial = com.jaikar.spideyos.data.SpideySettings(),
    )
    val nav = rememberNavController()
    val start = if (settings.onboardingDone) Routes.LAUNCHER else Routes.ONBOARDING

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                settings = settings,
                onFinished = { nav.navigate(Routes.LAUNCHER) { popUpTo(Routes.ONBOARDING) { inclusive = true } } },
            )
        }
        composable(Routes.LAUNCHER) {
            LauncherScreen(
                settings = settings,
                onOpenAssistant = { nav.navigate(Routes.ASSISTANT) },
                onOpenMessages = { nav.navigate(Routes.MESSAGES) },
                onOpenMail = { nav.navigate(Routes.MAIL) },
                onOpenCamera = { nav.navigate(Routes.CAMERA) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.ASSISTANT) {
            AssistantScreen(settings = settings, onBack = { nav.popBackStack() })
        }
        composable(Routes.MESSAGES) {
            WebMessagesScreen(settings = settings, onBack = { nav.popBackStack() })
        }
        composable(Routes.MAIL) {
            MailDigestScreen(settings = settings, onBack = { nav.popBackStack() })
        }
        composable(Routes.CAMERA) {
            PeterCameraScreen(settings = settings, onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(settings = settings, onBack = { nav.popBackStack() })
        }
    }
}
