package com.ansa1r.projectadhd.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ansa1r.projectadhd.domain.dialogue.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.domain.onboarding.OnboardingStep

@Composable
fun OnboardingCoach(vm: OnboardingViewModel, step: OnboardingStep, modifier: Modifier = Modifier) {
    BackHandler { }
    val busy by vm.busy.collectAsStateWithLifecycle()
    val requirements by vm.requirements.collectAsStateWithLifecycle()
    val error by vm.message.collectAsStateWithLifecycle()
    val base = OnboardingDialogues.lineFor(step) ?: return
    val line = if (error == null) base else base.copy(id = base.id + "_SAVE_ERROR",
        text = base.text + "\nНе удалось сохранить. Попробуй ещё раз.")
    val dialogue = rememberBobDialogue(line)
    val playback by dialogue.state.collectAsStateWithLifecycle()
    val current = if (playback.currentDialogueId == line.id) playback else BobDialogueState(line, BobDialoguePhase.PREPARING)
    val action = when (step) {
        OnboardingStep.SETUP_APPS -> "Выбрать приложения"
        OnboardingStep.SETUP_HABIT -> "Создать привычку"
        OnboardingStep.SETUP_PERMISSIONS -> "Настроить разрешения"
        OnboardingStep.FINAL -> "Готово"
        else -> "Продолжить"
    }
    val secondary = when {
        step == OnboardingStep.SETUP_APPS && requirements.appsSaved -> "Оставить сохранённый выбор"
        step == OnboardingStep.SETUP_HABIT && requirements.habitSaved -> "Продолжить с сохранённой привычкой"
        else -> null
    }
    val advance: () -> Unit = {
        vm.dismissMessage()
        if (step in setOf(OnboardingStep.SETUP_APPS, OnboardingStep.SETUP_HABIT, OnboardingStep.SETUP_PERMISSIONS)) vm.begin(step)
        else vm.advance(step)
    }
    val primary: () -> Unit = { if (dialogue.state.value.currentDialogueId == line.id) dialogue.tap(advance) }
    BobCoachPanel(line.text, action, primary, modifier = modifier,
        position = CoachPosition.BOTTOM,
        secondaryLabel = secondary, secondary = { if (dialogue.state.value.currentDialogueId == line.id) dialogue.tap { vm.advance(step) } }, enabled = !busy,
        mood = line.expression, isTalking = current.isSpeaking,
        visibleTextLength = current.visibleLength, speechElapsedMillis = current.playedMillis,
        onPanelTap = primary, showBob = line.showBob, readyToAdvance = current.canAdvance)
}
