package com.dhikr.app.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.dhikr.app.design.theme.DhikrMotion
import com.dhikr.app.design.theme.DhikrTheme

/**
 * The bead pad: one large slab that is the counter's only primary action.
 * - Each tap calls [onTap] exactly once. No debounce, and deliberately no double-tap
 *   detector (that would delay every single tap).
 * - Pressed: surface → surfaceRaised and content scales to 0.97 (no ripple, no elevation).
 */
@Composable
fun BeadPad(
    onTap: () -> Unit,
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = DhikrTheme.colors
    val currentOnTap by rememberUpdatedState(onTap)
    var pressed by remember { mutableStateOf(false) }
    val bg by animateColorAsState(
        if (pressed) c.surfaceRaised else c.surface, tween(DhikrMotion.FAST_MS), label = "padBg",
    )
    val scale by animateFloatAsState(
        if (pressed) 0.97f else 1f, tween(if (pressed) DhikrMotion.FAST_MS else 120), label = "padScale",
    )
    Box(
        modifier
            .clip(RoundedCornerShape(DhikrTheme.spacing.padRadius))
            .background(bg)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = accessibilityLabel
                onClick { currentOnTap(); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { currentOnTap() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }, contentAlignment = Alignment.Center) {
            content()
        }
    }
}
