package com.ansa1r.projectadhd.domain.dialogue

import java.text.BreakIterator
import java.util.Locale

/** Full-text character boundaries keep surrogate pairs and combining marks together. */
class WeightedTextReveal(val text: String) {
    private val ends = mutableListOf<Int>()
    private val weights = mutableListOf<Float>()
    private val total: Float

    init {
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
        iterator.setText(text)
        var start = iterator.first()
        var end = iterator.next()
        var accumulated = 0f
        while (end != BreakIterator.DONE) {
            accumulated += when (text[start]) {
                ' ', '\n', '\t' -> 0.3f
                ',', ':', ';' -> 2f
                '.', '!', '?', '…' -> 3f
                else -> 1f
            }
            ends += end
            weights += accumulated
            start = end
            end = iterator.next()
        }
        total = accumulated
    }

    fun visibleLength(progress: Float): Int {
        if (!progress.isFinite() || progress <= 0f) return 0
        if (progress >= 1f) return text.length
        val target = total * progress
        val index = weights.binarySearch(target)
        val visibleIndex = if (index >= 0) index else -index - 2
        return if (visibleIndex < 0) 0 else ends[visibleIndex]
    }
}
