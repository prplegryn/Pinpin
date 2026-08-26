package com.prplegryn.pinpin.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * One motion vocabulary for the whole product. Durations describe intent instead of
 * individual screens, which prevents the UI from feeling like unrelated samples.
 */
internal object PinpinMotion {
    const val Instant = 90
    const val Quick = 150
    const val Standard = 240
    const val Emphasized = 360

    val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val EaseInOut = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

    fun <T> quickTween() = tween<T>(durationMillis = Quick, easing = EaseOut)
    fun <T> standardTween() = tween<T>(durationMillis = Standard, easing = EaseOut)
    fun <T> emphasizedTween() = tween<T>(durationMillis = Emphasized, easing = EaseOut)

    fun <T> settledSpring() = spring<T>(
        dampingRatio = 0.86f,
        stiffness = 430f
    )

    fun <T> expressiveSpring() = spring<T>(
        dampingRatio = 0.72f,
        stiffness = 360f
    )
}

/** Draw-phase press feedback: responsive, interruptible, and isolated from layout. */
@Composable
internal fun Modifier.pinpinPressFeedback(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.965f,
    pressedAlpha: Float = 0.92f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (pressed) PinpinMotion.quickTween() else PinpinMotion.settledSpring(),
        label = "press feedback"
    )
    return graphicsLayer {
        val scale = 1f + (pressedScale - 1f) * progress
        scaleX = scale
        scaleY = scale
        alpha = 1f + (pressedAlpha - 1f) * progress
    }
}
