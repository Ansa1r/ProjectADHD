package com.ansa1r.projectadhd.monitoring

import com.ansa1r.projectadhd.domain.intervention.InterventionDecision
import com.ansa1r.projectadhd.domain.intervention.NoInterventionReason
import com.ansa1r.projectadhd.domain.model.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class MonitorStatus { STOPPED, STARTING, RUNNING }
enum class MonitorIssue { NONE, USAGE_ACCESS, NOTIFICATIONS, START_FAILED, DATA_ERROR }

data class MonitoringSnapshot(
    val status: MonitorStatus = MonitorStatus.STOPPED,
    val issue: MonitorIssue = MonitorIssue.NONE,
    val session: AppSession? = null,
    val foregroundName: String? = null,
    val decision: InterventionDecision = InterventionDecision.None(NoInterventionReason.NO_FOREGROUND),
    val updatedAt: Long? = null
)

class MonitoringState {
    private val mutable = MutableStateFlow(MonitoringSnapshot())
    val state = mutable.asStateFlow()
    fun set(value: MonitoringSnapshot) { mutable.value = value }
    fun stopped(issue: MonitorIssue = MonitorIssue.NONE) { mutable.value = MonitoringSnapshot(issue = issue) }
    fun starting() { mutable.update { it.copy(status = MonitorStatus.STARTING, issue = MonitorIssue.NONE) } }
}
