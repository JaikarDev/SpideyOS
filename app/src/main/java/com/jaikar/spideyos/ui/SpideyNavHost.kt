package com.jaikar.spideyos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jaikar.spideyos.SpideyApp
import com.jaikar.spideyos.ui.assistant.AssistantScreen
import com.jaikar.spideyos.ui.camera.SnapBoothScreen
import com.jaikar.spideyos.ui.launcher.LauncherScreen
import com.jaikar.spideyos.ui.mail.MailDigestScreen
import com.jaikar.spideyos.ui.messages.WebMessagesScreen
import com.jaikar.spideyos.ui.motion.weaveEnter
import com.jaikar.spideyos.ui.motion.weaveExit
import com.jaikar.spideyos.ui.motion.weavePopEnter
import com.jaikar.spideyos.ui.motion.weavePopExit
import com.jaikar.spideyos.ui.onboarding.OnboardingScreen
import com.jaikar.spideyos.ui.companion.PermissionDashboardScreen
import com.jaikar.spideyos.ui.companion.TodayScreen
import com.jaikar.spideyos.ui.settings.SettingsScreen
import com.jaikar.spideyos.ui.vibe.VibeSplashScreen

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LAUNCHER = "launcher"
    const val ASSISTANT = "assistant"
    const val MESSAGES = "messages"
    const val MAIL = "mail"
    const val CAMERA = "camera"
    const val SETTINGS = "settings"
    const val TODAY = "today"
    const val PERMISSIONS = "permissions"
}

@Composable
fun SpideyNavHost(
    deepLinkRoute: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val settingsRepo = (context.applicationContext as SpideyApp).settings
    val settings by settingsRepo.settings.collectAsState(
        initial = com.jaikar.spideyos.data.SpideySettings(),
    )
    val nav = rememberNavController()
    val start = if (settings.onboardingDone) Routes.SPLASH else Routes.ONBOARDING

    LaunchedEffect(deepLinkRoute, settings.onboardingDone) {
        val route = deepLinkRoute ?: return@LaunchedEffect
        if (!settings.onboardingDone) return@LaunchedEffect
        val allowed = setOf(
            Routes.ASSISTANT,
            Routes.MESSAGES,
            Routes.MAIL,
            Routes.CAMERA,
            Routes.SETTINGS,
            Routes.TODAY,
            Routes.PERMISSIONS,
            Routes.LAUNCHER,
        )
        if (route in allowed) {
            nav.navigate(Routes.LAUNCHER) {
                popUpTo(nav.graph.startDestinationId) { inclusive = false }
                launchSingleTop = true
            }
            if (route != Routes.LAUNCHER) {
                nav.navigate(route) { launchSingleTop = true }
            }
            onDeepLinkConsumed()
        }
    }

    NavHost(
        navController = nav,
        startDestination = start,
        enterTransition = { weaveEnter() },
        exitTransition = { weaveExit() },
        popEnterTransition = { weavePopEnter() },
        popExitTransition = { weavePopExit() },
    ) {
        composable(Routes.SPLASH) {
            VibeSplashScreen(
                onFinished = {
                    nav.navigate(Routes.LAUNCHER) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                settings = settings,
                onFinished = {
                    nav.navigate(Routes.SPLASH) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
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
                onOpenToday = { nav.navigate(Routes.TODAY) },
            )
        }
        composable(Routes.ASSISTANT) {
            AssistantScreen(
                settings = settings,
                onBack = { nav.popBackStack() },
                onOpenMail = { nav.navigate(Routes.MAIL) },
                onOpenMessages = { nav.navigate(Routes.MESSAGES) },
                onOpenCamera = { nav.navigate(Routes.CAMERA) },
            )
        }
        composable(Routes.MESSAGES) {
            WebMessagesScreen(settings = settings, onBack = { nav.popBackStack() })
        }
        composable(Routes.MAIL) {
            MailDigestScreen(settings = settings, onBack = { nav.popBackStack() })
        }
        composable(Routes.CAMERA) {
            SnapBoothScreen(settings = settings, onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                onBack = { nav.popBackStack() },
                onOpenToday = { nav.navigate(Routes.TODAY) },
                onOpenPermissions = { nav.navigate(Routes.PERMISSIONS) },
            )
        }
        composable(Routes.TODAY) {
            TodayScreen(
                settings = settings,
                onBack = { nav.popBackStack() },
                onOpenPermissions = { nav.navigate(Routes.PERMISSIONS) },
            )
        }
        composable(Routes.PERMISSIONS) {
            PermissionDashboardScreen(
                settings = settings,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
