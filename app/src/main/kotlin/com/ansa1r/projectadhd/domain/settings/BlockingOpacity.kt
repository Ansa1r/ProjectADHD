package com.ansa1r.projectadhd.domain.settings

import kotlin.math.roundToInt

object BlockingOpacity {
    const val MIN_PERCENT = 30
    const val MAX_PERCENT = 90
    const val STEP_PERCENT = 5
    const val DEFAULT_PERCENT = 65
    const val SLIDER_STEPS = (MAX_PERCENT - MIN_PERCENT) / STEP_PERCENT - 1

    fun normalize(percent: Int): Int {
        val bounded = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)
        return ((bounded + STEP_PERCENT / 2) / STEP_PERCENT) * STEP_PERCENT
    }

    fun fromSlider(percent: Float): Int =
        if (percent.isFinite()) normalize(percent.coerceIn(MIN_PERCENT.toFloat(), MAX_PERCENT.toFloat()).roundToInt())
        else DEFAULT_PERCENT

    fun alpha(percent: Int): Float = normalize(percent) / 100f
}
