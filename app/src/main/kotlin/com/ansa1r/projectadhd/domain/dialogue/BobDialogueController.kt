package com.ansa1r.projectadhd.domain.dialogue

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BobPlayback(val playedFrames: Long, val totalFrames: Int, val sampleRate: Int, val playing: Boolean)

interface BobSpeechEngine {
    /** Cancellation must finish fading/releasing the previous output before returning. */
    suspend fun speak(line: BobDialogueLine, onPlayback: (BobPlayback) -> Unit)
}

enum class BobDialoguePhase { PREPARING, PLAYING, STOPPING, FINISHED, SUSPENDED }

data class BobDialogueState(
    val line: BobDialogueLine? = null,
    val phase: BobDialoguePhase = BobDialoguePhase.SUSPENDED,
    val visibleLength: Int = 0,
    val speechProgress: Float = 0f,
    val playedMillis: Int = 0,
    val isSpeaking: Boolean = false,
    val advanceRequested: Boolean = false,
    val audioError: String? = null
) {
    val currentDialogueId: String? get() = line?.id
    val fullText: String get() = line?.text.orEmpty()
    val visibleText: String get() = fullText.take(visibleLength)
    val isTextComplete: Boolean get() = line != null && visibleLength == fullText.length
    val canAdvance: Boolean get() = phase == BobDialoguePhase.FINISHED && isTextComplete && !isSpeaking && !advanceRequested
}

/** Commands and engine callbacks run on the supplied UI scope; no text or mouth clock. */
class BobDialogueController(private val scope: CoroutineScope, private val speech: BobSpeechEngine) {
    private val mutable = MutableStateFlow(BobDialogueState())
    val state = mutable.asStateFlow()
    private var session = 0L
    private var job: Job? = null
    private var pendingAdvance: (() -> Unit)? = null

    fun start(line: BobDialogueLine) {
        val token = ++session
        val previous = job
        previous?.cancel()
        pendingAdvance = null
        mutable.value = BobDialogueState(line, BobDialoguePhase.PREPARING)
        val reveal = WeightedTextReveal(line.text)
        job = scope.launch {
            previous?.join()
            try {
                speech.speak(line) { playback ->
                    if (session == token) {
                        val frames = playback.playedFrames.coerceIn(0, playback.totalFrames.toLong())
                        val progress = if (playback.totalFrames > 0) frames.toFloat() / playback.totalFrames else 0f
                        mutable.value = mutable.value.copy(
                            phase = if (progress >= 1f) BobDialoguePhase.FINISHED else if (playback.playing && frames > 0) BobDialoguePhase.PLAYING else BobDialoguePhase.PREPARING,
                            visibleLength = reveal.visibleLength(progress), speechProgress = progress,
                            playedMillis = (frames * 1000 / playback.sampleRate.coerceAtLeast(1)).toInt(),
                            isSpeaking = playback.playing && frames > 0 && progress < 1f
                        )
                    }
                }
                if (session == token) finish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (session == token) finish(error.javaClass.simpleName)
            }
        }
    }

    fun tap(advance: () -> Unit) {
        val current = mutable.value
        val line = current.line ?: return
        when {
            current.canAdvance -> advanceOnce(advance)
            current.phase == BobDialoguePhase.STOPPING -> if (pendingAdvance == null) pendingAdvance = advance
            current.phase == BobDialoguePhase.SUSPENDED || current.advanceRequested -> Unit
            line.allowSkip -> {
                val token = ++session
                val previous = job
                previous?.cancel()
                mutable.value = current.copy(phase = BobDialoguePhase.STOPPING, visibleLength = line.text.length,
                    speechProgress = 1f, isSpeaking = false)
                job = scope.launch {
                    previous?.join()
                    if (session == token) {
                        finish()
                        pendingAdvance?.let { next -> pendingAdvance = null; advanceOnce(next) }
                    }
                }
            }
        }
    }

    fun stop(expectedDialogueId: String? = null) {
        if (expectedDialogueId != null && mutable.value.currentDialogueId != expectedDialogueId) return
        ++session
        pendingAdvance = null
        job?.cancel()
        mutable.value = mutable.value.copy(phase = BobDialoguePhase.SUSPENDED, isSpeaking = false)
    }

    private fun finish(error: String? = null) {
        mutable.value = mutable.value.copy(phase = BobDialoguePhase.FINISHED,
            visibleLength = mutable.value.fullText.length, speechProgress = 1f, isSpeaking = false, audioError = error)
    }

    private fun advanceOnce(advance: () -> Unit) {
        if (!mutable.value.canAdvance) return
        mutable.value = mutable.value.copy(advanceRequested = true)
        advance()
    }
}
