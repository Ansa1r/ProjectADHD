package com.ansa1r.projectadhd.domain.habits

enum class HabitType { MANUAL, APP_BASED }
enum class HabitState { READY, IN_PROGRESS, PAUSED, AWAITING_CONFIRMATION, COMPLETED }

data class HabitProgress(
    val accumulatedMillis: Long = 0,
    val extraTargetMinutes: Long = 0,
    val state: HabitState = HabitState.READY,
    val checkpointElapsed: Long? = null,
    val bootCount: Int = -1,
    val sessionMillis: Long = 0
) {
    fun targetMillis(baseMinutes: Int): Long = Math.multiplyExact(Math.addExact(baseMinutes.toLong(), extraTargetMinutes), 60_000L)
    fun start(elapsed: Long, boot: Int): HabitProgress = when (state) {
        HabitState.READY, HabitState.PAUSED -> copy(state = HabitState.IN_PROGRESS, checkpointElapsed = elapsed,
            bootCount = boot)
        else -> this
    }
    fun advance(baseMinutes: Int, elapsed: Long, boot: Int): HabitProgress {
        val checkpoint = checkpointElapsed ?: return this
        if (state != HabitState.IN_PROGRESS) return this
        if (boot < 0 || boot != bootCount || elapsed < checkpoint) return copy(state = HabitState.PAUSED, checkpointElapsed = null)
        val remaining = (targetMillis(baseMinutes) - accumulatedMillis).coerceAtLeast(0)
        val addition = (elapsed - checkpoint).coerceIn(0, remaining)
        val reached = addition == remaining
        return copy(accumulatedMillis = accumulatedMillis + addition, sessionMillis = sessionMillis + addition,
            checkpointElapsed = if (reached) null else elapsed,
            state = if (reached) HabitState.AWAITING_CONFIRMATION else HabitState.IN_PROGRESS)
    }
    fun pause() = if (state == HabitState.IN_PROGRESS) copy(state = HabitState.PAUSED, checkpointElapsed = null) else this
    fun confirm(yes: Boolean, elapsed: Long, boot: Int): HabitProgress {
        if (state != HabitState.AWAITING_CONFIRMATION) return this
        return if (yes) copy(state = HabitState.COMPLETED, checkpointElapsed = null)
        else copy(extraTargetMinutes = Math.addExact(extraTargetMinutes, 10L), state = HabitState.IN_PROGRESS,
            checkpointElapsed = elapsed, bootCount = boot)
    }
    fun appTotal(baseMinutes: Int, observedMillis: Long): HabitProgress {
        if (state == HabitState.COMPLETED) return this
        val total = maxOf(accumulatedMillis, observedMillis).coerceAtMost(targetMillis(baseMinutes))
        return copy(accumulatedMillis = total, state = if (total >= targetMillis(baseMinutes)) HabitState.COMPLETED
            else if (total > 0) HabitState.PAUSED else HabitState.READY)
    }
}

object HabitAppConflict {
    fun canLink(packageName: String, limitedPackages: Set<String>) = packageName !in limitedPackages
    fun canLimit(packageName: String, activeHabitPackages: Set<String>) = packageName !in activeHabitPackages
}
