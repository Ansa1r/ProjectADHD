package com.ansa1r.projectadhd.ui.mascot.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ansa1r.projectadhd.BuildConfig
import com.ansa1r.projectadhd.R
import kotlin.math.min

@Composable
fun BobMascot(
    modifier: Modifier = Modifier,
    state: BobAnimationState = BobAnimationDefaults.State,
    isTalking: Boolean = BobAnimationDefaults.IsTalking,
    animationEnabled: Boolean = true,
    blinkRequest: Int = 0,
    showLayerBounds: Boolean = false,
    speechElapsedMillis: Int? = null
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var visible by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> visible = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val running = visible && animationEnabled
    val frozen = remember { mutableFloatStateOf(0f) }
    val motion: State<Float>
    val blink: State<Float>
    val speech: State<Float>
    if (running) {
        val transition = rememberInfiniteTransition(label = "Bob idle")
        motion = transition.animateFloat(0f, 1f,
            infiniteRepeatable(tween(BobAnimationTimeline.FLOAT_CYCLE_MS, easing = LinearEasing)), label = "Idle phase")
        blink = transition.animateFloat(0f, BobAnimationTimeline.BLINK_CYCLE_MS.toFloat(),
            infiniteRepeatable(tween(BobAnimationTimeline.BLINK_CYCLE_MS, easing = LinearEasing)), label = "Blink clock")
        speech = if (isTalking && speechElapsedMillis == null) transition.animateFloat(0f, BobAnimationTimeline.TALK_CYCLE_MS.toFloat(),
            infiniteRepeatable(tween(BobAnimationTimeline.TALK_CYCLE_MS, easing = LinearEasing)), label = "Talking clock") else frozen
    } else {
        motion = frozen; blink = frozen; speech = frozen
    }
    val forcedBlink = remember { Animatable(BobAnimationTimeline.BLINK_DURATION_MS.toFloat()) }
    LaunchedEffect(blinkRequest, running) {
        if (running && blinkRequest > 0) {
            forcedBlink.snapTo(0f)
            forcedBlink.animateTo(BobAnimationTimeline.BLINK_DURATION_MS.toFloat(), tween(BobAnimationTimeline.BLINK_DURATION_MS, easing = LinearEasing))
        }
    }
    val art = remember { BobVectorAssets.layers }
    val happy = state == BobAnimationState.HAPPY || state == BobAnimationState.LEVEL_UP
    val blocking = state == BobAnimationState.BLOCKING
    val description = stringResource(when {
        blocking -> R.string.mascot_blocking_description
        happy -> R.string.mascot_praise_description
        else -> R.string.mascot_idle_description
    })
    Canvas(modifier.size(160.dp).semantics { contentDescription = description; role = Role.Image }) {
        val phase = motion.value
        val eyes = when {
            !running -> BobEyeState.OPEN
            forcedBlink.value < BobAnimationTimeline.BLINK_DURATION_MS -> BobAnimationTimeline.blinkAt(forcedBlink.value.toInt())
            else -> BobAnimationTimeline.eyesAt(blink.value.toInt())
        }
        val mouth = BobAnimationTimeline.mouthAt(speechElapsedMillis ?: speech.value.toInt(), running && isTalking)
        val side = min(size.width, size.height)
        val unitScale = side / 400f
        val floatY = if (running && unitScale > 0f) BobIdleMotion.floatY(phase) * min(1f, 6.dp.toPx() / (8.8f * unitScale)) else 0f
        withTransform({
            translate((size.width - side) / 2f, (size.height - side) / 2f)
            scale(unitScale, unitScale, Offset.Zero)
            translate(top = floatY)
        }) {
            fun part(id: String, angle: Float = 0f, y: Float = 0f, stretch: Float = 1f) {
                drawBobLayer(art.getValue(id), angle, y, stretch, BuildConfig.DEBUG && showLayerBounds)
            }
            part("BODY_BASE")
            for (index in 0..3) part("BOTTOM_${index + 1}",
                if (running) BobIdleMotion.bottomRotation(index, phase) else 0f,
                if (running) BobIdleMotion.bottomY(index, phase) else 0f,
                if (running) BobIdleMotion.bottomScaleY(index, phase) else 1f)
            part("ARM_LEFT", if (running) BobIdleMotion.armRotation(true, phase) else 0f)
            part("ARM_RIGHT", if (running) BobIdleMotion.armRotation(false, phase) else 0f)
            part("HAIR")
            part("SCARF_NECK")
            part("SCARF_TAIL", if (running) BobIdleMotion.scarfRotation(phase) else 0f)
            part(if (blocking) "EYEBROW_LEFT_BLOCKING" else "EYEBROW_LEFT")
            part(if (blocking) "EYEBROW_RIGHT_BLOCKING" else "EYEBROW_RIGHT")
            val eyeSuffix = if (happy) when (eyes) {
                BobEyeState.OPEN -> "HAPPY"
                BobEyeState.HALF -> "HAPPY_HALF"
                BobEyeState.CLOSED -> "HAPPY_CLOSED"
            } else eyes.name
            part("EYE_LEFT_$eyeSuffix")
            part("EYE_RIGHT_$eyeSuffix")
            part("BLUSH")
            part(when {
                blocking -> "MOUTH_BLOCKING_${mouth.name}"
                happy && mouth == BobMouthState.IDLE && speechElapsedMillis == null -> "MOUTH_WIDE"
                else -> "MOUTH_${mouth.name}"
            })
        }
    }
}

private fun DrawScope.drawBobLayer(layer: BobVectorLayer, angle: Float, y: Float, stretch: Float, bounds: Boolean) {
    withTransform({
        translate(top = y)
        rotate(angle, layer.pivot)
        scale(1f, stretch, layer.pivot)
    }) {
        for (shape in layer.shapes) {
            val clip = shape.clip
            if (clip == null) drawPath(shape.path, shape.brush, shape.alpha, shape.style)
            else clipPath(clip) { drawPath(shape.path, shape.brush, shape.alpha, shape.style) }
            if (bounds) {
                val box = shape.path.getBounds()
                drawRect(Color.Magenta.copy(alpha = 0.35f), box.topLeft, box.size, style = Stroke(0.6f))
            }
        }
        if (bounds) drawCircle(Color.Cyan, 2f, layer.pivot)
    }
}
