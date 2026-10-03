package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.ui.mascot.animation.*
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class BobAnimationTimelineTest {
    @Test fun defaultsAreIdleAndSilent() {
        assertEquals(BobAnimationState.IDLE, BobAnimationDefaults.State)
        assertFalse(BobAnimationDefaults.IsTalking)
    }

    @Test fun nonTalkingAlwaysReturnsIdleEvenDuringAnOpenMouthFrame() {
        for (time in 0..3_000) assertEquals(BobMouthState.IDLE, BobAnimationTimeline.mouthAt(time, false))
    }

    @Test fun speechUsesSeveralShapesAndUnevenIntervalsThenLoops() {
        val times = listOf(0, 120, 230, 410, 500, 690, 860, 950, 1_090, 1_260)
        val expected = listOf(BobMouthState.IDLE, BobMouthState.SMALL, BobMouthState.MEDIUM,
            BobMouthState.SMALL, BobMouthState.WIDE, BobMouthState.MEDIUM, BobMouthState.SMALL,
            BobMouthState.IDLE, BobMouthState.MEDIUM, BobMouthState.IDLE)
        assertEquals(expected, times.map { BobAnimationTimeline.mouthAt(it, true) })
        assertTrue(times.zipWithNext { a, b -> b - a }.distinct().size > 3)
        for (time in 0..1_450) assertEquals(BobAnimationTimeline.mouthAt(time, true),
            BobAnimationTimeline.mouthAt(time + BobAnimationTimeline.TALK_CYCLE_MS, true))
    }

    @Test fun blinkUsesOpenHalfClosedHalfOpenWithoutPositionOrScaleState() {
        assertEquals(listOf(BobEyeState.OPEN, BobEyeState.HALF, BobEyeState.CLOSED, BobEyeState.HALF, BobEyeState.OPEN),
            listOf(0, 70, 130, 200, 300).map(BobAnimationTimeline::blinkAt))
        assertEquals(BobEyeState.HALF, BobAnimationTimeline.blinkAt(129))
        assertEquals(BobEyeState.CLOSED, BobAnimationTimeline.blinkAt(199))
        assertEquals(BobEyeState.HALF, BobAnimationTimeline.blinkAt(299))
    }

    @Test fun automaticBlinkRestsForTwoSecondsAndReturnsToOpen() {
        assertEquals(BobEyeState.OPEN, BobAnimationTimeline.eyesAt(1_999))
        assertEquals(BobEyeState.HALF, BobAnimationTimeline.eyesAt(2_070))
        assertEquals(BobEyeState.CLOSED, BobAnimationTimeline.eyesAt(2_130))
        assertEquals(BobEyeState.HALF, BobAnimationTimeline.eyesAt(2_200))
        assertEquals(BobEyeState.OPEN, BobAnimationTimeline.eyesAt(2_300))
        assertEquals(BobEyeState.CLOSED, BobAnimationTimeline.eyesAt(4_430))
    }

    @Test fun floatStartsAtCenterAndClosesItsLoopWithSoftAmplitude() {
        assertEquals(0f, BobIdleMotion.floatY(0f), .0001f)
        assertEquals(-8.8f, BobIdleMotion.floatY(.25f), .0001f)
        assertEquals(8.8f, BobIdleMotion.floatY(.75f), .0001f)
        assertEquals(BobIdleMotion.floatY(0f), BobIdleMotion.floatY(1f), .0001f)
        val before = BobIdleMotion.floatY(1f) - BobIdleMotion.floatY(.999f)
        val after = BobIdleMotion.floatY(.001f) - BobIdleMotion.floatY(0f)
        assertEquals(before, after, .0001f)
    }

    @Test fun bottomAndScarfAreSmallContinuousWavesWithDifferentPhases() {
        assertNotEquals(BobIdleMotion.bottomY(0, 0f), BobIdleMotion.bottomY(1, 0f))
        for (index in 0..3) {
            assertEquals(BobIdleMotion.bottomY(index, 0f), BobIdleMotion.bottomY(index, 1f), .0001f)
            assertEquals(BobIdleMotion.bottomRotation(index, 0f), BobIdleMotion.bottomRotation(index, 1f), .0001f)
            assertEquals(BobIdleMotion.bottomScaleY(index, 0f), BobIdleMotion.bottomScaleY(index, 1f), .0001f)
            for (frame in 0..360) {
                val phase = frame / 360f
                assertTrue(abs(BobIdleMotion.bottomY(index, phase)) <= 1.2501f)
                assertTrue(abs(BobIdleMotion.bottomRotation(index, phase)) <= 1.4001f)
                assertTrue(BobIdleMotion.bottomScaleY(index, phase) in .9849f..1.0151f)
                assertTrue(abs(BobIdleMotion.scarfRotation(phase)) <= 2.4001f)
            }
        }
        assertEquals(BobIdleMotion.scarfRotation(0f), BobIdleMotion.scarfRotation(1f), .0001f)
    }
}
