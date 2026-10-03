package com.ansa1r.projectadhd.ui.onboarding

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.ansa1r.projectadhd.audio.AndroidBobSpeechEngine
import com.ansa1r.projectadhd.domain.dialogue.BobDialogueController
import com.ansa1r.projectadhd.domain.dialogue.BobDialogueLine
import kotlinx.coroutines.awaitCancellation

@Composable
fun rememberBobDialogue(line: BobDialogueLine): BobDialogueController {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val engine = remember(context) { AndroidBobSpeechEngine(context) }
    val controller = remember(engine, scope) { BobDialogueController(scope, engine) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(controller, lifecycle, line.id, line.text, line.speech) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            controller.start(line)
            try { awaitCancellation() } finally { controller.stop(line.id) }
        }
    }
    DisposableEffect(controller) { onDispose { controller.stop() } }
    return controller
}
