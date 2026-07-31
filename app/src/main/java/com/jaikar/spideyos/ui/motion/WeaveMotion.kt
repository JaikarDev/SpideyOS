package com.jaikar.spideyos.ui.motion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation.NavBackStackEntry

object WeaveMotion {
    const val ENTER_MS = 420
    const val EXIT_MS = 280
    private val ease = FastOutSlowInEasing

    fun enter() = fadeIn(tween(ENTER_MS, easing = ease)) +
        slideInHorizontally(tween(ENTER_MS, easing = ease)) { it / 6 } +
        scaleIn(tween(ENTER_MS, easing = ease), initialScale = 0.94f)

    fun exit() = fadeOut(tween(EXIT_MS, easing = ease)) +
        slideOutHorizontally(tween(EXIT_MS, easing = ease)) { -it / 10 } +
        scaleOut(tween(EXIT_MS, easing = ease), targetScale = 0.96f)

    fun popEnter() = fadeIn(tween(ENTER_MS, easing = ease)) +
        slideInHorizontally(tween(ENTER_MS, easing = ease)) { -it / 8 }

    fun popExit() = fadeOut(tween(EXIT_MS, easing = ease)) +
        slideOutHorizontally(tween(EXIT_MS, easing = ease)) { it / 5 } +
        scaleOut(tween(EXIT_MS, easing = ease), targetScale = 1.04f)
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.weaveEnter() = WeaveMotion.enter()
fun AnimatedContentTransitionScope<NavBackStackEntry>.weaveExit() = WeaveMotion.exit()
fun AnimatedContentTransitionScope<NavBackStackEntry>.weavePopEnter() = WeaveMotion.popEnter()
fun AnimatedContentTransitionScope<NavBackStackEntry>.weavePopExit() = WeaveMotion.popExit()

fun vibeTransform(): ContentTransform =
    (fadeIn(tween(500)) + scaleIn(initialScale = 0.92f)) togetherWith
        (fadeOut(tween(320)) + scaleOut(targetScale = 1.05f))
