package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.dialogue.*
import com.ansa1r.projectadhd.domain.onboarding.OnboardingStep
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class BobDialogueTest {
    @Test fun revealIsWeightedBoundedAndComplete() {
        val text = "Привет, Боб! Как дела?"
        val reveal = WeightedTextReveal(text)
        assertEquals(0, reveal.visibleLength(0f))
        assertTrue(reveal.visibleLength(0.5f) in 1 until text.length)
        assertEquals(text.length, reveal.visibleLength(1f))
        var previous = 0
        for (step in -10..110) {
            val length = reveal.visibleLength(step / 100f)
            assertTrue(length in previous..text.length)
            previous = length
        }
        assertEquals(2, WeightedTextReveal("а,б").visibleLength(0.75f))
        assertEquals(1, WeightedTextReveal("а,б").visibleLength(0.5f))
        assertEquals(0, WeightedTextReveal("").visibleLength(1f))
        assertEquals(0, reveal.visibleLength(Float.NaN))
    }

    @Test fun revealDoesNotSplitSurrogatePairs() {
        val text = "Боб 😀!"
        val reveal = WeightedTextReveal(text)
        for (step in 0..100) {
            val shown = text.take(reveal.visibleLength(step / 100f))
            assertFalse(shown.lastOrNull()?.isHighSurrogate() == true)
        }
    }

    @Test fun allTenCoachStepsHaveDifferentStableIds() {
        val lines = OnboardingStep.entries.mapNotNull(OnboardingDialogues::lineFor)
        assertEquals(10, lines.size)
        assertEquals(10, lines.map { it.id }.toSet().size)
        assertEquals("WELCOME_INTRO", OnboardingDialogues.lineFor(OnboardingStep.WELCOME)?.id)
        assertEquals("HOME_INTRO", OnboardingDialogues.lineFor(OnboardingStep.HOME)?.id)
        assertEquals("FINAL_CONGRATS", OnboardingDialogues.lineFor(OnboardingStep.FINAL)?.id)
        assertNull(OnboardingDialogues.lineFor(OnboardingStep.SELECT_APPS))
        assertNull(OnboardingDialogues.lineFor(OnboardingStep.COMPLETED))
    }

    @Test fun newIdReleasesOldSessionAndStartsHidden() = runBlocking {
        val engine = FakeSpeech()
        val controller = BobDialogueController(this, engine)
        controller.start(line("WELCOME")); yield()
        engine.emit(50)
        assertTrue(controller.state.value.isSpeaking)
        val stale = engine.callback
        controller.start(line("HOME"))
        repeat(4) { yield() }
        assertEquals(listOf("start:WELCOME", "release:WELCOME", "start:HOME"), engine.events)
        assertEquals("HOME", controller.state.value.currentDialogueId)
        assertEquals("", controller.state.value.visibleText)
        stale?.invoke(BobPlayback(100, 100, 100, false))
        assertEquals("", controller.state.value.visibleText)
        engine.emit(100)
        assertEquals(line("HOME").text, controller.state.value.visibleText)
        assertFalse(controller.state.value.isSpeaking)
        assertTrue(controller.state.value.canAdvance)
        controller.stop(); yield()
    }

    @Test fun firstTapRevealsAndSecondTapAdvancesOnceAfterRelease() = runBlocking {
        val engine = FakeSpeech()
        val controller = BobDialogueController(this, engine)
        var advances = 0
        controller.start(line("WELCOME")); yield()
        engine.emit(10)
        controller.tap { advances++ }
        assertTrue(controller.state.value.isTextComplete)
        assertFalse(controller.state.value.isSpeaking)
        assertEquals(BobDialoguePhase.STOPPING, controller.state.value.phase)
        assertEquals(0, advances)
        controller.tap { advances++ }
        repeat(4) { yield() }
        assertEquals(1, advances)
        assertTrue(engine.events.contains("release:WELCOME"))
        controller.tap { advances++ }
        assertEquals(1, advances)
        controller.stop()
    }

    @Test fun stopClosesMouthAndResumeCreatesNewSession() = runBlocking {
        val engine = FakeSpeech()
        val controller = BobDialogueController(this, engine)
        controller.start(line("WELCOME")); yield(); engine.emit(30)
        controller.stop(); yield()
        assertFalse(controller.state.value.isSpeaking)
        assertFalse(controller.state.value.canAdvance)
        controller.start(line("WELCOME")); repeat(3) { yield() }
        assertEquals(2, engine.events.count { it == "start:WELCOME" })
        assertEquals("", controller.state.value.visibleText)
        controller.stop(); yield()
    }

    @Test fun everyOnboardingMessageStartsItsOwnSpeakingSession() = runBlocking {
        val engine = FakeSpeech()
        val controller = BobDialogueController(this, engine)
        val lines = OnboardingStep.entries.mapNotNull(OnboardingDialogues::lineFor)
        for (line in lines) {
            controller.start(line); repeat(3) { yield() }
            assertEquals("", controller.state.value.visibleText)
            engine.emit(50)
            assertTrue(line.id, controller.state.value.isSpeaking)
            engine.emit(100)
            assertEquals(line.text, controller.state.value.visibleText)
            assertFalse(controller.state.value.isSpeaking)
        }
        controller.stop(); yield()
        assertEquals(lines.map { "start:${it.id}" }, engine.events.filter { it.startsWith("start:") })
        assertEquals(10, engine.events.count { it.startsWith("release:") })
    }

    @Test fun skipBeforePreparationFinishesNeverStartsAudio() = runBlocking {
        val engine = FakeSpeech()
        val controller = BobDialogueController(this, engine)
        var advances = 0
        controller.start(line("WELCOME"))
        controller.tap { advances++ }
        repeat(3) { yield() }
        assertTrue(engine.events.isEmpty())
        assertTrue(controller.state.value.canAdvance)
        assertEquals(0, advances)
        controller.tap { advances++ }
        assertEquals(1, advances)
        controller.stop()
    }

    @Test fun audioFailureRevealsTextAndDoesNotTrapOnboarding() = runBlocking {
        val engine = object : BobSpeechEngine {
            override suspend fun speak(line: BobDialogueLine, onPlayback: (BobPlayback) -> Unit) { error("No output") }
        }
        val controller = BobDialogueController(this, engine)
        controller.start(line("WELCOME")); yield()
        assertTrue(controller.state.value.canAdvance)
        assertFalse(controller.state.value.isSpeaking)
        assertNotNull(controller.state.value.audioError)
        controller.stop()
    }

    private fun line(id: String) = BobDialogueLine(id, "Привет! Я Боб.")
    private class FakeSpeech : BobSpeechEngine {
        val events = mutableListOf<String>()
        var callback: ((BobPlayback) -> Unit)? = null
        override suspend fun speak(line: BobDialogueLine, onPlayback: (BobPlayback) -> Unit) {
            events += "start:${line.id}"
            callback = onPlayback
            try { awaitCancellation() } finally { events += "release:${line.id}" }
        }
        fun emit(frames: Long) { callback?.invoke(BobPlayback(frames, 100, 100, frames in 1..99)) }
    }
}
