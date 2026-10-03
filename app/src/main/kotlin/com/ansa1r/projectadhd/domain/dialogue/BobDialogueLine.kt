package com.ansa1r.projectadhd.domain.dialogue

import com.ansa1r.projectadhd.domain.model.MascotMood
import com.ansa1r.projectadhd.domain.onboarding.OnboardingStep

data class BobSpeechProfile(val charactersPerSecond: Int = 16, val overlapMillis: Int = 60)

data class BobDialogueLine(
    val id: String,
    val text: String,
    val onboardingStep: OnboardingStep? = null,
    val speech: BobSpeechProfile = BobSpeechProfile(),
    val allowSkip: Boolean = true,
    val showBob: Boolean = true,
    val expression: MascotMood = MascotMood.IDLE
)

object OnboardingDialogues {
    fun lineFor(step: OnboardingStep): BobDialogueLine? {
        val (id, text) = when (step) {
            OnboardingStep.WELCOME -> "WELCOME_INTRO" to "Привет! Я Боб. Я помогу тебе следить за привычками и меньше отвлекаться."
            OnboardingStep.HOME -> "HOME_INTRO" to "Это главная страница. Здесь видно твой прогресс за сегодня."
            OnboardingStep.HABITS -> "HABITS_INTRO" to "Здесь твои привычки. Выполняй их, чтобы получать доступ к ограниченным приложениям."
            OnboardingStep.APPS -> "APPS_INTRO" to "Здесь выбираются приложения, которые чаще всего тебя отвлекают."
            OnboardingStep.STATS -> "STATS_INTRO" to "Здесь можно посмотреть свой прогресс и статистику."
            OnboardingStep.MASCOT -> "MASCOT_INTRO" to "А здесь живу я. Выполняй привычки и сохраняй серии дней — так будет расти мой уровень."
            OnboardingStep.SETUP_APPS -> "SETUP_APPS_INTRO" to "Теперь выберем приложения, которые чаще всего тебя отвлекают."
            OnboardingStep.SETUP_HABIT -> "SETUP_HABIT_INTRO" to "Отлично. Теперь создадим твою первую привычку."
            OnboardingStep.SETUP_PERMISSIONS -> "PERMISSIONS_INTRO" to "Почти готово. Теперь мне нужны разрешения, чтобы всё работало."
            OnboardingStep.FINAL -> "FINAL_CONGRATS" to "Всё готово. Отличное начало! Нажми «Готово», и я начну следить за лимитами."
            else -> return null
        }
        return BobDialogueLine(id, text, step, expression = if (step == OnboardingStep.FINAL) MascotMood.PRAISE else MascotMood.IDLE)
    }
}
