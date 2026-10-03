package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.dialogue.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class BobSpeechPlanTest {
    private fun samples() = (1..8).map { index ->
        val name = "bob_voice_${index.toString().padStart(2, '0')}.wav"
        val path = listOf(File("src/main/res/raw", name), File("app/src/main/res/raw", name)).first { it.exists() }
        BobPcmWav.decode(path.readBytes())
    }

    @Test fun providedWavsAndGeneratedPhraseUseOriginalFormat() {
        val samples = samples()
        assertTrue(samples.all { it.sampleRate == 44_100 && it.pcm.isNotEmpty() })
        val plan = BobSpeechPlanner.plan("Привет! Я Боб.", samples, BobSpeechProfile(), Random(42))
        val pcm = BobSpeechPlanner.mix(plan, samples)
        assertEquals(2646, plan.overlapFrames)
        assertEquals(plan.totalFrames, pcm.size)
        assertEquals(0, pcm.first().toInt())
        assertEquals(0, pcm.last().toInt())
        assertTrue(plan.clips.zipWithNext().all { (a, b) -> a != b })
        assertTrue(pcm.any { it.toInt() != 0 })
    }

    @Test fun longerMessagesGetLongerSpeechWithoutFixedPattern() {
        val samples = samples()
        val short = BobSpeechPlanner.plan("Привет!", samples, BobSpeechProfile(), Random(1))
        val long = BobSpeechPlanner.plan("Привет! Я Боб. Я помогу следить за привычками.", samples, BobSpeechProfile(), Random(2))
        assertTrue(long.totalFrames > short.totalFrames)
        assertTrue(long.durationMillis > short.durationMillis)
        assertNotEquals(long.clips, BobSpeechPlanner.plan("Привет! Я Боб. Я помогу следить за привычками.", samples, BobSpeechProfile(), Random(3)).clips)
    }

    @Test fun overlapBlendsSamplesInsteadOfInsertingSilence() {
        val samples = listOf(BobPcmSample(1000, ShortArray(200) { 1000 }), BobPcmSample(1000, ShortArray(200) { -1000 }))
        val pcm = BobSpeechPlanner.mix(BobSpeechPlan(listOf(0, 1), 60, 340, 1000), samples)
        assertEquals(1000, pcm[140].toInt())
        assertEquals(-1000, pcm[199].toInt())
        assertTrue(pcm[170].toInt() in -100..100)
        assertTrue((141..199).all { kotlin.math.abs(pcm[it] - pcm[it - 1]) < 40 })
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsUnsupportedWav() {
        BobPcmWav.decode(ByteArray(44))
    }
}
