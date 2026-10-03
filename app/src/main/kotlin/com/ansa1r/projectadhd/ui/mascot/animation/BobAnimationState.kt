package com.ansa1r.projectadhd.ui.mascot.animation

import kotlin.math.PI
import kotlin.math.sin

enum class BobAnimationState { IDLE, HAPPY, BLOCKING, CONFIRMATION, LEVEL_UP, ONBOARDING }
enum class BobEyeState { OPEN, HALF, CLOSED }
enum class BobMouthState { IDLE, SMALL, MEDIUM, WIDE }

object BobAnimationDefaults {
    val State = BobAnimationState.IDLE
    const val IsTalking = false
}

object BobAnimationTimeline {
    const val FLOAT_CYCLE_MS = 2_800
    const val BLINK_REST_MS = 2_000
    const val BLINK_DURATION_MS = 300
    const val BLINK_CYCLE_MS = BLINK_REST_MS + BLINK_DURATION_MS
    const val TALK_CYCLE_MS = 1_450

    fun blinkAt(elapsedMillis: Int): BobEyeState = when {
        elapsedMillis < 70 -> BobEyeState.OPEN
        elapsedMillis < 130 -> BobEyeState.HALF
        elapsedMillis < 200 -> BobEyeState.CLOSED
        elapsedMillis < BLINK_DURATION_MS -> BobEyeState.HALF
        else -> BobEyeState.OPEN
    }

    fun eyesAt(elapsedMillis: Int): BobEyeState {
        val time = elapsedMillis.coerceAtLeast(0) % BLINK_CYCLE_MS
        return if (time < BLINK_REST_MS) BobEyeState.OPEN else blinkAt(time - BLINK_REST_MS)
    }

    fun mouthAt(elapsedMillis: Int, isTalking: Boolean): BobMouthState {
        if (!isTalking) return BobMouthState.IDLE
        return when (elapsedMillis.coerceAtLeast(0) % TALK_CYCLE_MS) {
            in 0 until 120 -> BobMouthState.IDLE
            in 120 until 230 -> BobMouthState.SMALL
            in 230 until 410 -> BobMouthState.MEDIUM
            in 410 until 500 -> BobMouthState.SMALL
            in 500 until 690 -> BobMouthState.WIDE
            in 690 until 860 -> BobMouthState.MEDIUM
            in 860 until 950 -> BobMouthState.SMALL
            in 950 until 1_090 -> BobMouthState.IDLE
            in 1_090 until 1_260 -> BobMouthState.MEDIUM
            else -> BobMouthState.IDLE
        }
    }
}

object BobIdleMotion {
    private fun wave(phase: Float, shift: Float = 0f): Float = sin(phase * 2.0 * PI + shift).toFloat()
    fun floatY(phase: Float): Float = -8.8f * wave(phase)
    fun bottomY(index: Int, phase: Float): Float = 1.25f * wave(phase, index * 0.55f)
    fun bottomRotation(index: Int, phase: Float): Float = 1.4f * wave(phase, index * 0.55f + 0.35f)
    fun bottomScaleY(index: Int, phase: Float): Float = 1f + 0.015f * wave(phase, index * 0.55f + 0.8f)
    fun scarfRotation(phase: Float): Float = 2.4f * wave(phase, -0.65f)
    fun armRotation(left: Boolean, phase: Float): Float = 0.8f * wave(phase, if (left) 0.4f else 0.9f)
}
