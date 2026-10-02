package com.ansa1r.projectadhd.ui.onboarding

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.domain.onboarding.OnboardingStep
import com.ansa1r.projectadhd.domain.model.MascotMood

internal fun coachPositionFor(step: OnboardingStep): CoachPosition =
    if (step == OnboardingStep.WELCOME) CoachPosition.TOP else CoachPosition.BOTTOM

@Composable
fun OnboardingCoach(vm: OnboardingViewModel, step: OnboardingStep) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val requirements by vm.requirements.collectAsStateWithLifecycle()
    val error by vm.message.collectAsStateWithLifecycle()
    val text = when (step) {
        OnboardingStep.WELCOME -> "Привет! Я Боб. Я помогу тебе следить за привычками и меньше отвлекаться."
        OnboardingStep.HOME -> "Это главная страница. Здесь видно твой прогресс за сегодня."
        OnboardingStep.HABITS -> "Здесь твои привычки. Выполняй их, чтобы получать доступ к ограниченным приложениям."
        OnboardingStep.APPS -> "Здесь выбираются приложения, которые чаще всего тебя отвлекают."
        OnboardingStep.STATS -> "Здесь можно посмотреть свой прогресс и статистику."
        OnboardingStep.MASCOT -> "А здесь живу я. Выполняй привычки и сохраняй серии дней — так будет расти мой уровень."
        OnboardingStep.SETUP_APPS -> "Теперь выберем приложения, которые чаще всего тебя отвлекают."
        OnboardingStep.SETUP_HABIT -> "Отлично. Теперь создадим твою первую привычку."
        OnboardingStep.SETUP_PERMISSIONS -> "Почти готово. Теперь мне нужны разрешения, чтобы всё работало."
        OnboardingStep.FINAL -> "Всё готово. Отличное начало! Нажми «Готово», и я начну следить за лимитами."
        else -> return
    }
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
    BobCoachOverlay(text + if (error != null) "\nНе удалось сохранить. Попробуй ещё раз." else "", action,
        action = {
            vm.dismissMessage()
            if (step in setOf(OnboardingStep.SETUP_APPS, OnboardingStep.SETUP_HABIT, OnboardingStep.SETUP_PERMISSIONS)) vm.begin(step)
            else vm.advance(step)
        },
        position = coachPositionFor(step),
        secondaryLabel = secondary, secondary = { vm.advance(step) }, enabled = !busy,
        mood = if (step == OnboardingStep.FINAL) MascotMood.PRAISE else MascotMood.IDLE)
}
