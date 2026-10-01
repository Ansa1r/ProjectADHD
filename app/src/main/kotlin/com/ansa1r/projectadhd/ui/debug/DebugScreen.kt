package com.ansa1r.projectadhd.ui.debug

import com.ansa1r.projectadhd.domain.mascot.MascotProgression
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.BuildConfig
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.components.*
import com.ansa1r.projectadhd.util.durationText
import com.ansa1r.projectadhd.util.timestampText

@Composable
fun DebugScreen(viewModel: DebugViewModel) {
    if (!BuildConfig.DEBUG) return
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    RefreshOnResume(viewModel::refresh)
    ScreenList {
        item { MessageBanner(viewModel) }
        item { SectionCard {
            Text("Stage 4 · Маскот", style = MaterialTheme.typography.titleLarge)
            val level = MascotProgression.level(state.mascot.totalXp)
            Detail("XP", state.mascot.totalXp.toString())
            Detail("Level / next threshold / multiplier", "$level / ${MascotProgression.threshold(level)} / ${MascotProgression.multiplier(level)}")
            Detail("Streak / last reward", "${state.mascot.streak} / ${state.mascot.lastStreakRewardDate ?: "—"}")
            Detail("Last habit XP award", state.awards.firstOrNull { it.kind == "HABIT" }?.let { "${it.localDate}: +${it.awardedXp} XP (${it.eventKey})" } ?: "—")
            Detail("Habit runtime error", state.habitRuntimeError ?: "—")
            Detail("Legacy app conflicts (blocking suppressed)", state.conflicts.joinToString().ifEmpty { "—" })
            Text("Для коротких проверок создайте привычку на 00:01. Тестовые кнопки overlay не начисляют XP.")
        } }
        items(state.habitDetails, key = { "habit:${it.id}" }) { habit -> SectionCard {
            Text("${habit.title} · ${habit.type}")
            Detail("Base / extra / effective minutes", "${habit.targetDurationMinutes} / ${habit.progress.extraTargetMinutes} / ${habit.targetDurationMinutes + habit.progress.extraTargetMinutes}")
            Detail("Daily accumulated", durationText(habit.progress.accumulatedMillis))
            Detail("State / awaiting confirmation", "${habit.progress.state} / ${habit.progress.state == com.ansa1r.projectadhd.domain.habits.HabitState.AWAITING_CONFIRMATION}")
            Detail("Manual checkpoint / boot / session", "${habit.progress.checkpointElapsed} / ${habit.progress.bootCount} / ${durationText(habit.progress.sessionMillis)}")
            Detail("Linked package / app time", "${habit.linkedAppPackage ?: "—"} / ${durationText(habit.progress.accumulatedMillis)}")
        } }
        item {
            SectionCard {
                Text(stringResource(R.string.debug_intro))
                Detail(stringResource(R.string.package_name), BuildConfig.APPLICATION_ID)
                Detail(stringResource(R.string.build_type), BuildConfig.BUILD_TYPE)
                Detail(stringResource(R.string.overlay_permission), permissionText(state.permissions.overlay))
                Detail(stringResource(R.string.usage_access), permissionText(state.permissions.usageAccess))
                Detail(stringResource(R.string.notification_permission), permissionText(state.permissions.notifications))
                Detail(stringResource(R.string.channels_status),
                    permissionText(state.permissions.monitoringChannel && state.permissions.interventionChannel))
                Detail(stringResource(R.string.monitoring), monitorText(state.monitoring.status))
                MonitorIssueText(state.monitoring.issue)
                Detail(stringResource(R.string.foreground_package), state.monitoring.session?.packageName ?: "—")
                Detail(stringResource(R.string.foreground_app), state.monitoring.foregroundName ?: "—")
                Detail(stringResource(R.string.session_start), timestampText(state.monitoring.session?.startedAt))
                Detail(stringResource(R.string.current_session), durationText(state.monitoring.session?.durationMillis ?: 0))
                Detail(stringResource(R.string.session_limit), state.monitoring.limitMillis?.let(::durationText) ?: "—")
                Detail(stringResource(R.string.daily_task_summary), "${state.tasks.total} / ${state.tasks.completed} / ${state.tasks.incomplete}")
                Detail(stringResource(R.string.engine_decision), decisionText(state.monitoring.decision))
                Detail(stringResource(R.string.praise_cooldown), stringResource(R.string.minutes_value, state.settings.praiseCooldownMinutes))
                Detail(stringResource(R.string.last_praise), timestampText(state.settings.lastPraiseAt))
                Detail(stringResource(R.string.last_unlock), timestampText(state.lastUnlock))
                Detail(stringResource(R.string.blocking_opacity), stringResource(R.string.percent_value, 85))
                Detail(stringResource(R.string.overlay_visible), state.overlay.visible.toString())
                Detail(stringResource(R.string.overlay_error), state.overlay.lastError ?: "—")
                Detail(stringResource(R.string.debug_test_until), timestampText(state.overlay.testArmedUntil))
                Detail(stringResource(R.string.cooldown), stringResource(R.string.minutes_value, state.settings.cooldownMinutes))
                Detail(stringResource(R.string.cooldown_reserved_at), timestampText(state.settings.lastInterventionAt))
                Detail(stringResource(R.string.last_intervention), timestampText(state.lastEventAt))
                Detail(stringResource(R.string.monitoring_started_at), timestampText(state.settings.lastMonitoringStartedAt))
                Detail(stringResource(R.string.state_updated), timestampText(state.monitoring.updatedAt))
            }
        }
        item { Detail(stringResource(R.string.active_block_count), state.blocks.size.toString()) }
        items(state.blocks, key = { it.packageName }) { block ->
            SectionCard {
                Text(block.appName, style = MaterialTheme.typography.titleMedium)
                Detail(stringResource(R.string.blocked_package), block.packageName)
                Detail(stringResource(R.string.block_started), timestampText(block.startedAt))
                Detail(stringResource(R.string.baseline_completed), block.baselineCompletedCount.toString())
                Detail(stringResource(R.string.baseline_ids), block.eligibleHabitIds.joinToString())
                Detail(stringResource(R.string.current_session), durationText(block.triggerSessionDurationMillis))
            }
        }
        item {
            SectionCard {
                Text(stringResource(R.string.database_state), style = MaterialTheme.typography.titleMedium)
                state.counts?.let {
                    Detail(stringResource(R.string.habits), it.habits.toString())
                    Detail(stringResource(R.string.completions_count), it.completions.toString())
                    Detail(stringResource(R.string.apps), it.apps.toString())
                    Detail(stringResource(R.string.events_count), it.events.toString())
                } ?: Text(stringResource(R.string.loading))
            }
        }
        item {
            SectionCard {
                BrandOutlinedButton(onClick = viewModel::refresh) { Text(stringResource(R.string.refresh)) }
                BrandOutlinedButton(onClick = viewModel::testNotification) { Text(stringResource(R.string.test_notification)) }
                BrandOutlinedButton(onClick = viewModel::simulate) { Text(stringResource(R.string.simulate_intervention)) }
                BrandOutlinedButton(onClick = viewModel::testBlock) { Text(stringResource(R.string.debug_block_test)) }
                BrandOutlinedButton(onClick = viewModel::testPraise) { Text(stringResource(R.string.debug_praise_test)) }
                Text(stringResource(R.string.debug_test_hint))
                BrandOutlinedButton(onClick = viewModel::clearBlocks) { Text(stringResource(R.string.clear_blocks)) }
                BrandOutlinedButton(onClick = viewModel::stop) { Text(stringResource(R.string.stop_monitoring)) }
                Text(stringResource(R.string.simulation_hint))
                state.simulation?.let { Detail(stringResource(R.string.simulation_result), decisionText(it)) }
                TextButton(onClick = { confirmClear = true }) { Text(stringResource(R.string.clear_history)) }
            }
        }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false },
        title = { Text(stringResource(R.string.clear_history)) },
        text = { Text(stringResource(R.string.clear_history_hint)) },
        confirmButton = { TextButton(onClick = { viewModel.clearHistory(); confirmClear = false }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } })
}
