package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.model.AppSettings
import com.ansa1r.projectadhd.domain.settings.BlockingOpacity
import org.junit.Assert.*
import org.junit.Test

class BlockingOpacityTest {
    @Test fun defaultIsSixtyFivePercent() {
        assertEquals(65, AppSettings().blockingOverlayOpacityPercent)
        assertEquals(0.65f, BlockingOpacity.alpha(BlockingOpacity.DEFAULT_PERCENT), 0.00001f)
    }
    @Test fun everyAllowedValueSurvivesNormalization() {
        for (value in 30..90 step 5) assertEquals(value, BlockingOpacity.normalize(value))
    }
    @Test fun outOfRangeValuesAreClampedBeforeRounding() {
        for (value in listOf(Int.MIN_VALUE, -1, 0, 29)) assertEquals(30, BlockingOpacity.normalize(value))
        for (value in listOf(91, 100, Int.MAX_VALUE)) assertEquals(90, BlockingOpacity.normalize(value))
    }
    @Test fun offGridValuesUseNearestFivePercentStep() {
        assertEquals(30, BlockingOpacity.normalize(32))
        assertEquals(35, BlockingOpacity.normalize(33))
        assertEquals(65, BlockingOpacity.normalize(67))
        assertEquals(70, BlockingOpacity.normalize(68))
    }
    @Test fun sliderHandlesFractionalAndInvalidInput() {
        assertEquals(35, BlockingOpacity.fromSlider(32.5f))
        assertEquals(90, BlockingOpacity.fromSlider(200f))
        assertEquals(30, BlockingOpacity.fromSlider(-200f))
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(65, BlockingOpacity.fromSlider(value))
        }
    }
    @Test fun alphaHasRequiredBounds() {
        assertEquals(0.3f, BlockingOpacity.alpha(-1), 0.00001f)
        assertEquals(0.9f, BlockingOpacity.alpha(100), 0.00001f)
    }
    @Test fun normalizationIsIdempotentAcrossAllNearbyInputs() {
        for (value in -100..200) {
            val normalized = BlockingOpacity.normalize(value)
            assertEquals(normalized, BlockingOpacity.normalize(normalized))
            assertTrue(normalized in 30..90 && normalized % 5 == 0)
        }
    }
}
