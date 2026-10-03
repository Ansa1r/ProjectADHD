package com.ansa1r.projectadhd.domain.dialogue

import kotlin.math.roundToInt
import kotlin.random.Random

data class BobPcmSample(val sampleRate: Int, val pcm: ShortArray)
data class BobSpeechPlan(val clips: List<Int>, val overlapFrames: Int, val totalFrames: Int, val sampleRate: Int) {
    val durationMillis: Long get() = totalFrames * 1000L / sampleRate
}

object BobSpeechPlanner {
    fun plan(text: String, samples: List<BobPcmSample>, profile: BobSpeechProfile, random: Random = Random.Default): BobSpeechPlan {
        require(samples.size >= 2 && samples.all { it.sampleRate == samples.first().sampleRate })
        require(profile.charactersPerSecond > 0 && profile.overlapMillis in 50..80)
        val rate = samples.first().sampleRate
        require(rate > 0)
        val overlap = rate * profile.overlapMillis / 1000
        require(samples.all { it.pcm.size > overlap })
        val characters = text.codePointCount(0, text.length)
        val target = (characters.toLong() * rate / profile.charactersPerSecond).coerceAtLeast(rate * 350L / 1000)
        require(target <= rate * 120L) { "Dialogue exceeds two minutes" }
        val clips = mutableListOf<Int>()
        var frames = 0
        while (frames < target) {
            val previous = clips.lastOrNull()
            val clip = if (previous == null) random.nextInt(samples.size) else (previous + 1 + random.nextInt(samples.size - 1)) % samples.size
            frames += samples[clip].pcm.size - if (clips.isEmpty()) 0 else overlap
            clips += clip
        }
        return BobSpeechPlan(clips, overlap, target.toInt(), rate)
    }

    fun mix(plan: BobSpeechPlan, samples: List<BobPcmSample>): ShortArray {
        val result = ShortArray(plan.totalFrames)
        var cursor = 0
        for ((index, clip) in plan.clips.withIndex()) {
            val source = samples[clip].pcm
            val overlap = if (index == 0) 0 else plan.overlapFrames
            val start = cursor - overlap
            for (i in 0 until minOf(source.size, result.size - start)) {
                val position = start + i
                val value = if (i < overlap) {
                    val incoming = i.toFloat() / (overlap - 1).coerceAtLeast(1)
                    result[position] * (1f - incoming) + source[i] * incoming
                } else source[i].toFloat()
                result[position] = value.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            cursor = start + source.size
        }
        val attack = (plan.sampleRate * 10 / 1000).coerceAtMost(result.size)
        val release = (plan.sampleRate * 25 / 1000).coerceAtMost(result.size)
        for (i in 0 until attack) result[i] = (result[i] * i.toFloat() / attack).roundToInt().toShort()
        for (i in 0 until release) {
            val position = result.lastIndex - i
            result[position] = (result[position] * i.toFloat() / release).roundToInt().toShort()
        }
        return result
    }
}
