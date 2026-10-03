package com.ansa1r.projectadhd.audio

import android.content.Context
import android.media.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.dialogue.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

/** One mixed static PCM track per line, observed through its real playback head. */
class AndroidBobSpeechEngine(context: Context) : BobSpeechEngine {
    private val app = context.applicationContext
    private val samples by lazy {
        listOf(R.raw.bob_voice_01, R.raw.bob_voice_02, R.raw.bob_voice_03, R.raw.bob_voice_04,
            R.raw.bob_voice_05, R.raw.bob_voice_06, R.raw.bob_voice_07, R.raw.bob_voice_08).map { id ->
            app.resources.openRawResource(id).use { BobPcmWav.decode(it.readBytes()) }
        }
    }

    override suspend fun speak(line: BobDialogueLine, onPlayback: (BobPlayback) -> Unit) = sessions.withLock {
        val loaded = withContext(Dispatchers.IO) { samples }
        val (plan, pcm) = withContext(Dispatchers.Default) {
            val plan = BobSpeechPlanner.plan(line.text, loaded, line.speech)
            plan to BobSpeechPlanner.mix(plan, loaded)
        }
        currentCoroutineContext().ensureActive()
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        val focus = SpeechAudioFocus(app, attributes)
        var track: AudioTrack? = null
        try {
            check(focus.acquire()) { "Audio focus unavailable" }
            // Assign before leaving IO so cancellation during write still releases this track.
            withContext(Dispatchers.IO) {
                track = AudioTrack.Builder().setAudioAttributes(attributes)
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(plan.sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                    .setBufferSizeInBytes(pcm.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
                val output = requireNotNull(track)
                check(output.state == AudioTrack.STATE_INITIALIZED || output.state == AudioTrack.STATE_NO_STATIC_DATA)
                check(output.write(pcm, 0, pcm.size, AudioTrack.WRITE_BLOCKING) == pcm.size)
                check(output.state == AudioTrack.STATE_INITIALIZED)
            }
            val output = requireNotNull(track)
            output.setVolume(VOLUME)
            output.play()
            var lastFrames = 0L
            var lastMovement = SystemClock.elapsedRealtime()
            while (true) {
                currentCoroutineContext().ensureActive()
                check(focus.held.get()) { "Audio focus lost" }
                val frames = (output.playbackHeadPosition.toLong() and 0xffffffffL).coerceAtMost(plan.totalFrames.toLong())
                onPlayback(BobPlayback(frames, plan.totalFrames, plan.sampleRate,
                    output.playState == AudioTrack.PLAYSTATE_PLAYING && frames > 0 && frames < plan.totalFrames))
                if (frames >= plan.totalFrames) break
                if (frames > lastFrames) { lastFrames = frames; lastMovement = SystemClock.elapsedRealtime() }
                check(SystemClock.elapsedRealtime() - lastMovement < 2_000) { "Audio playback stalled" }
                // Polling only: delay never supplies elapsed time to text or mouth.
                delay(16)
            }
        } finally {
            withContext(NonCancellable) {
                try {
                    track?.let { output ->
                        if (output.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            for (step in 1..6) { output.setVolume(VOLUME * (1f - step / 6f)); delay(10) }
                        }
                    }
                } finally {
                    try {
                        withContext(Dispatchers.IO) {
                            track?.let { output ->
                                try { if (output.state == AudioTrack.STATE_INITIALIZED) output.stop() }
                                finally { output.release() }
                            }
                        }
                    } finally { focus.release() }
                }
            }
        }
    }

    private companion object {
        const val VOLUME = 0.8f
        val sessions = Mutex()
    }
}

private class SpeechAudioFocus(context: Context, attributes: AudioAttributes) {
    private val manager = context.getSystemService(AudioManager::class.java)
    val held = AtomicBoolean(false)
    private val listener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change < 0) held.set(false)
    }
    private val request = if (Build.VERSION.SDK_INT >= 26) AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener(listener, Handler(Looper.getMainLooper())).build() else null

    @Suppress("DEPRECATION")
    fun acquire(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= 26) manager.requestAudioFocus(requireNotNull(request))
        else manager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        held.set(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return held.get()
    }

    @Suppress("DEPRECATION")
    fun release() {
        held.set(false)
        if (Build.VERSION.SDK_INT >= 26) manager.abandonAudioFocusRequest(requireNotNull(request))
        else manager.abandonAudioFocus(listener)
    }
}
