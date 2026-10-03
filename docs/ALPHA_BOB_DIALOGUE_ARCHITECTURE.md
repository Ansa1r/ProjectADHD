# Bob Dialogue System v1

## Layout

`AppNavigation` places the coach inside `Scaffold.bottomBar`, in a `Column` before the existing `NavigationBar`. The complete measured bottom slot is included in Scaffold content padding. `main_content_area` applies that padding and consumes its insets before rendering the existing NavHost. A transparent input shield covers only the guided screen content; it does not cover the coach or navigation. Navigation retains its existing guided/disabled behavior.

The panel is only composed for a visible non-editor onboarding step after Startup Animation. After onboarding it contributes no space. Existing bottom navigation and routes are retained. The parent constrains coach height to 40% of its available viewport as a maximum, not a requested height. The existing scrolling row and intrinsic full-message height normally produce a much smaller panel. Bob is on the right, text on the left. No offset or bottom-navigation height estimate is used. Horizontal safeDrawing insets protect the coach from side cutouts; NavigationBar retains its own system bar insets.

`BobCoachPanel` is reused. The older standalone `BobCoachOverlay` remains available for its existing independent component tests, but onboarding does not call it. CoachPosition only selects the existing left/right presentation of the reusable panel; it no longer determines onboarding screen placement.

## Data and responsibilities

- `BobDialogueLine`: stable ID, full text, optional onboarding step, speech profile, skip permission, mascot visibility and expression. It has no Android dependency and can describe future blocking, confirmation or praise lines.
- `OnboardingDialogues`: ten original messages, unchanged wording, with ten distinct IDs. Editor states and COMPLETED have no coach line. Save errors get a distinct suffixed ID and keep the existing error wording.
- `BobSpeechPlan`: clip indices, overlap frames, total frames, source sample rate and derived duration.
- `BobDialogueController`: session identity, progress, visible text boundary, speaking state, skip, readiness and one-shot advance. It depends on a cancellable `BobSpeechEngine` interface and a supplied UI coroutine scope.
- `AndroidBobSpeechEngine`: raw asset loading, background PCM preparation, AudioTrack playback, playhead observation, focus and release.
- `BobCoachPanel`: full measured message, visible range, existing button/secondary action and the existing MascotView.
- `BobMascot`: unchanged vector assets and idle/blink/scarf/body motion; optional external speech time replaces the mouth clock for dialogue.

## Audio preparation

The eight provided files are copied byte-for-byte into `app/src/main/res/raw/bob_voice_01.wav` through `bob_voice_08.wav`. Their actual format is PCM signed 16-bit, 44,100 Hz, mono. The WAV reader checks RIFF/WAVE, PCM format, channel count, sample width and chunk bounds, and skips unknown RIFF chunks. No per-frame resampling or pitch processing occurs.

Samples load on Dispatchers.IO and are cached per engine. Planning/mixing run on Dispatchers.Default. The nominal duration is Unicode code-point count / 16 characters per second, with a 350 ms minimum. The final frame count determines the actual duration. Sample indices vary using Random.Default and never repeat immediately. Every adjacent clip overlaps by 60 ms / 2,646 frames. A linear complementary crossfade mixes the outgoing and incoming samples into a single continuous ShortArray. There are no silence gaps or successive MediaPlayer launches. A 10 ms phrase attack and 25 ms release taper the endpoints. Output gain is 0.8 of the provided samples and respects the user's media volume.

## Actual playback clock

One AudioTrack in MODE_STATIC holds the complete phrase. Its format matches the assets. Creation and the full PCM write occur on Dispatchers.IO. Playback is observed approximately every 16 ms using the unsigned playbackHeadPosition, clamped to the total phrase frames.

`progress = playedFrames / totalFrames`; `playedMillis = playedFrames * 1000 / sampleRate`.

The observation delay only determines refresh frequency. Neither text nor mouth derives elapsed time from that delay, from a separate animation clock, or from expected phrase duration. A two-second stalled-playhead watchdog is only an error detector.

Speech begins visually only after the playhead advances beyond zero and the track is PLAYING. On reaching the last frame, all text is shown and isSpeaking becomes false. Bob remains visible with idle motion and blinking.

## Weighted reveal and fixed panel size

`WeightedTextReveal` stores character boundaries from BreakIterator. Ordinary characters weigh 1, whitespace 0.3, comma/colon/semicolon 2, and sentence punctuation 3. The accumulated weights are normalized against actual audio progress. Zero progress reveals no characters; progress one always reveals the whole string. Boundaries prevent splitting UTF-16 surrogate pairs and combining character sequences.

The Text composable always receives the full AnnotatedString. Only the not-yet-revealed suffix has transparent foreground color. Its font, wrapping and measurements remain unchanged, so the coach does not resize during reveal. Bob, border, actions and visible text retain their normal opacity. Only the purple Surface fill uses Coach alpha 0.60; Ordinary 0.70 and Blocking 0.85 are untouched.

Accessibility exposes the full line once, rather than announcing each new substring. The button remains in the same place and retains its existing label. Panel stateDescription indicates whether the next tap reveals or advances. Existing action enablement while saving is retained.

## Mouth, expressions and existing animation

Onboarding passes playedMillis and isSpeaking through MascotView to BobMascot. When external speech time is supplied, BobMascot does not create its independent talking transition. Existing vector mouth states are selected by the existing irregular sequence at that playhead time. Blink, floating, arms, lower-body wave and scarf continue independently.

The FINAL expression keeps its happy eyes, but the audio-driven mouth returns to MOUTH_IDLE at the end/skip, including while that happy expression remains. Legacy silent happy/praise and blocking callers retain their previous behavior through the default null speech time. Dialogue does not replace vector assets or apply color filters.

## Tap and cancellation

First tap during PREPARING or PLAYING exposes the complete text immediately, closes the mouth and cancels the current engine coroutine. Controller phase is STOPPING while the engine fades out over six 10 ms volume steps and releases the track. This tap does not call the onboarding action.

A second tap after cleanup advances once. A very fast second tap during STOPPING is queued and advances after cleanup, so it is not lost. Extra repeated taps are ignored until a new dialogue session. The existing ViewModel expected-step comparison and persistence rules are retained; FINAL still completes onboarding and enables monitoring only through its original action.

Each start increments a session token, discarding late callbacks from old lines. Previous jobs are cancelled/joined. An app-wide engine Mutex also prevents overlap when Compose destroys one engine instance and creates another before its fade finishes. Cleanup uses NonCancellable with nested finally blocks so track release and audio-focus abandonment happen even on cancellation or playback failure.

## Lifecycle and focus

rememberBobDialogue retains the engine/controller across normal recompositions. LaunchedEffect keys include line ID/text/profile and the lifecycle owner. repeatOnLifecycle(RESUMED) starts a session when foregrounded and cancels it on leaving RESUMED. Returning can restart the current line from the beginning. Disposal also stops the controller. A previous line's lifecycle cleanup cannot stop a different current ID.

Audio focus is transient with ducking of other media. Denial, focus loss, decoding/output failure or a stalled playhead cancels/releases audio and reveals the full line, leaving the existing Continue action usable. There is no background audio service and no queued autoplay after focus returns.

## References

- https://developer.android.com/reference/android/media/AudioTrack
- https://developer.android.com/media/optimize/audio-focus

## Source limitation

No fresh project ZIP was attached. The implementation uses the verified GitHub snapshot aa57ae06f0eca9ca202eede2887336dd1faeccd2. Its customization route is still ProfilePlaceholderScreen; it contains no BobAppearance or persisted per-part colors. Existing rendering/colors are preserved, but compatibility with an unprovided customization implementation cannot be claimed. The Gradle version name in that snapshot remains 0.4.1-pre-alpha; release metadata was not changed as part of this update.
