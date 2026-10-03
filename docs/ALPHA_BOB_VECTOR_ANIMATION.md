# Bob Idle Vector Animation v1

## Source and runtime

The current project snapshot is GitHub commit `6f5b4bbf0634023fe53011781e1d2db0b2597952`. This animation update preserves its application version and package. The existing `MascotView` now delegates to one reusable `BobMascot`.

Editable sources: `app/src/main/assets/bob/vector/`. All 36 SVGs use `viewBox="0 0 400 400"`; their visible contents are paths, gradients and clipping paths. No SVG contains an image, embedded data URL, font-dependent face detail or raster frame. `layers.json` records the logical layer names and pivots in the same coordinate system.

Run from the project root after editing the SVG sources:

```powershell
python tools/generate_bob_vectors.py
python tools/generate_bob_vectors.py --check
```

The generator reads these SVGs, validates the supported path-only subset, and writes `ui/mascot/animation/BobVectorAssets.kt`. This generated Kotlin file is checked in with the project; Python is not required to build or run the app. It caches Compose `Path` and `Brush` objects. `BobMascot` draws them with `Canvas.drawPath`, applying transforms around each layer's declared attachment point. There is no runtime SVG parsing, bitmap decoding, GIF, video, sprite sheet or skeletal engine.

Original PNGs and the supplied references are retained in `art/bob/reference/`, outside Android's runtime assets. Existing legacy mascot PNG resources are also retained unchanged, but the main application's mascot renderer no longer refers to them. Startup continues using its own original asset.

## Layer hierarchy

`BOB_ROOT` is the shared Canvas coordinate transform. Its translation moves the complete character vertically. Its children, in drawing order, are:

| Layer | Function / pivot in 400 × 400 coordinates |
|---|---|
| BODY_BASE | Purple silhouette and shading; center (214, 227) |
| BOTTOM_1…4 | Overlapping ghost fringe; pivots (123, 284), (164, 292), (215, 300), (267, 297) |
| ARM_LEFT / ARM_RIGHT | Side flaps; attachment points (114, 276), (310, 244) |
| HAIR | Separate green upper sweep; (221, 115), fixed relative to the body in v1 |
| SCARF_NECK | Separate green neck wrap; (218, 258), follows the root |
| SCARF_TAIL | Free green end and its back fold; attachment (248, 258) |
| EYEBROW_LEFT / RIGHT | Neutral and blocking variants |
| EYE_LEFT / RIGHT | OPEN, HALF, CLOSED; happy expression variants |
| BLUSH | Separate pink face highlights |
| MOUTH | IDLE, SMALL, MEDIUM, WIDE; optional SMILE and blocking variants |

The SVG directories are `source/`, `body/` and `face/`. Body, hair and both scarf parts have separate silhouettes; moving the tail does not rotate the neck wrap. Bottom pieces overlap the body at their attachment edges to avoid gaps under the small transforms. FX and a separate floor shadow are not included in v1.

## BobMascot API

```kotlin
BobMascot(
    modifier = Modifier.size(224.dp),
    state = BobAnimationState.IDLE,
    isTalking = false
)
```

`BobAnimationState` contains IDLE, HAPPY, BLOCKING, CONFIRMATION, LEVEL_UP and ONBOARDING. IDLE is the default. All currently share the gentle idle movement; HAPPY/LEVEL_UP use a joyful face and BLOCKING uses its frown and corresponding mouth shapes. The other states are reserved for later specialized animations.

Optional parameters: `animationEnabled` freezes the pose, `blinkRequest` requests an individual blink, and `showLayerBounds` shows vector bounds/pivots only when `BuildConfig.DEBUG` is true. The existing adapter maps `MascotMood.IDLE/BLOCKING/PRAISE` to IDLE/BLOCKING/HAPPY.

## Independent animation channels

`rememberInfiniteTransition` drives phase clocks with the Compose frame clock. The clocks are read inside Canvas drawing; their continuous changes do not trigger layout or recompose the whole screen. Paths and brushes are allocated once, not on every frame. There is no manual timer, frame list, busy loop, Thread.sleep or 60-frame asset sequence.

| Motion | Implementation |
|---|---|
| Float | `-8.8 × sin(2π × phase)` viewBox units, 2,800 ms period. At 224 dp this is approximately ±4.93 dp, capped at ±6 dp for larger renderings. No animated X translation or root rotation. |
| Bottom wave | Four phase offsets separated by 0.55 radians; translation Y ±1.25 units, rotation ±1.4°, scale Y ±1.5%. No stepping or alternating leg lifts. |
| Scarf | Tail rotation ±2.4°, phase lag 0.65 radians. Neck wrap remains attached to the root. |
| Arms | Very small rotation ±0.8° about their attachment pivots. |
| Hair | Separate layer, fixed relative to the root in v1. |

The phase clock advances uniformly; the visible movement follows a sine curve with smooth speed changes at its extrema. At phase 1 the transform and its velocity return to phase 0, so the motion loop closes continuously. A component recreated after leaving a screen starts its own fresh idle cycle.

## Blink and face states

`BobAnimationTimeline` is pure Kotlin state logic. Each automatic blink has a 2,000 ms resting interval followed by a 300 ms sequence:

| Relative blink time | Eye state |
|---|---|
| 0–69 ms | OPEN |
| 70–129 ms | HALF |
| 130–199 ms | CLOSED |
| 200–299 ms | HALF |
| 300 ms | OPEN |

Each eye has its own vector paths in the common coordinate system. HALF uses a separate aperture/eyelid shape with clipping; CLOSED is a prepared curve. The eye is never flattened with scaleY. Both eyes blink together; the original open paths are reused exactly when the blink ends. Happy has corresponding happy squint shapes so blinking does not replace its joyful face with a neutral expression.

## Talking

`isTalking = false` immediately selects the logical IDLE mouth. For the happy expression the resting mouth remains the existing joyful open shape; it is static. Blocking's resting mouth remains a frown.

When talking, the mouth switches through IDLE → SMALL → MEDIUM → SMALL → WIDE → MEDIUM → SMALL → IDLE → MEDIUM → IDLE. The 1,450 ms phrase uses intervals of 90–190 ms, rather than a mechanical two-frame alternation. Blocking uses matching downturned mouth shapes, not a happy talking mouth.

Production talking is enabled only while WELCOME is displayed and while the blocking dialogue is displayed. Moving to HOME disables it; closing the blocking overlay disposes the composition. There is no text completion/audio event in the existing UI, so speech is a visual loop while that dialogue is visible. There is no real lip sync, phoneme processing or sound.

## Integration and lifecycle

The adapter covers Home, both Mascot Screen sizes, all Bob Coach panels, stop-monitoring confirmation, habit confirmation, Blocking and Praise. Only the two specified production dialogues enable talking. The monochrome navigation glyph remains a static navigation icon; it is not the full Bob artwork. Launcher icons and both system/Compose startup animations are unchanged.

When the host lifecycle falls below STARTED, the infinite animations leave composition and the renderer shows a still pose. They also stop on component disposal. The existing overlay's LifecycleOwner and ComposeView disposal are reused; monitoring, window flags and blocking decisions are unchanged. The manual-blink coroutine is scoped to `LaunchedEffect` and is cancelled on disposal or lifecycle change. Standard Compose animation duration scaling applies; no new motion setting is introduced.

## Debug preview and future work

Settings → Developer → Debug contains Bob Animation Preview in debug builds only: Idle/Blocking/Happy, Talking ON/OFF, Blink now, and bounds/pivots. No new production route is added.

Future BobAppearance customization can replace individual SVG layers and their pivot metadata, then regenerate the cached path collection. Hats/glasses can be inserted into the root drawing order without merging the body, hair, eyes or scarf. The current task does not add cosmetic persistence, new business state or a skeletal rig.

Static SVG previews in `docs/bob_vector_preview.svg` and `docs/bob_face_states.svg` are for visual inspection. `bob_face_states.png` is a documentation render, never a runtime animation frame.
